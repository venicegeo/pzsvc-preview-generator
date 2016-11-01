/**
 * Copyright 2016, RadiantBlue Technologies, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package generator.components;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.Future;

import javax.media.jai.PlanarImage;

import org.apache.commons.io.FileUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.coverage.grid.GridCoverageWriter;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import generator.model.RasterCropRequest;
import generator.model.ServiceResource;
import model.data.DataResource;
import model.data.location.FileAccessFactory;
import model.data.location.FileLocation;
import model.data.location.S3FileStore;
import model.data.type.RasterDataType;
import model.job.JobProgress;
import model.job.metadata.ResourceMetadata;
import model.status.StatusUpdate;
import util.UUIDFactory;

import org.geotools.geometry.GeneralEnvelope;
import org.geotools.resources.image.ImageUtilities;

/**
 * 
 * Service to crop the raster resource. It will read raster from S3 to local disk, 
 * crop it, and upload it back up to the same s3 bucket.
 * 
 * @author Sonny.Saniev
 * 
 */
@Component
public class RasterGenerator {

	@Autowired
	private UUIDFactory uuidFactory;
	@Autowired
	private MongoAccessor mongoAccessor;
	
	@Value("${s3.key.access:}")
	private String AMAZONS3_ACCESS_KEY;
	@Value("${s3.key.private:}")
	private String AMAZONS3_PRIVATE_KEY;
	@Value("${raster.temp.directory}")
	private String RASTER_LOCAL_DIRECTORY;

	private AmazonS3 s3Client;

	private static final String S3_OUTPUT_BUCKET = "pz-svcs-prevgen-output";

	/**
	 * Asynchronous handler for cropping the image demonstrating service monitor capabilities of piazza.
	 * 
	 * @return Future
	 * @throws Exception 
	 */
	@Async
	public Future<String> run(RasterCropRequest payload, String id) throws Exception {
		// Crop raster
		DataResource dataResource = cropRasterCoverage(payload, id);

		// Create storage model
		ServiceResource serviceResource = new ServiceResource();
		serviceResource.setServiceResourceId(id);
		// Generate a random number
		Random rand = new SecureRandom();
		// Need to include 100%
		int percent = rand.nextInt(101);
		JobProgress jobProgress = new JobProgress(percent);
		StatusUpdate statusUpdate = new StatusUpdate(StatusUpdate.STATUS_SUCCESS);
		statusUpdate.setProgress(jobProgress);
		serviceResource.setStatus(statusUpdate);
		serviceResource.setResult(dataResource);
		
		// persist ServiceResource
		mongoAccessor.addServiceResource(serviceResource);
		return new AsyncResult<String>("crop raster thread");
	}
	
	/**
	 * Create a cropped coverage.
	 * 
	 * @param RasterCropRequest Payload to Describe the Resource Location and Bounding Box.
	 * 
	 * @throws Exception 
	 */
	public DataResource cropRasterCoverage(RasterCropRequest payload, String serviceId) throws Exception {

		// sleeping for 15 seconds for demo and test purposes
		Thread.sleep(15000);

		// Read Original File to From S3
		Long fileSize = Long.valueOf(0);
		FileLocation fileLocation = new S3FileStore(payload.getSource().getBucketName(),
				payload.getSource().getFileName(), fileSize, payload.getSource().getDomain());
		File tiff = getFileFromS3(fileLocation, serviceId);

		// Create Temporary Local Write Directory
		String tempTopFolder = String.format("%s_%s", RASTER_LOCAL_DIRECTORY, serviceId);
		File localWriteDir = new File(String.format("%s%s%s", tempTopFolder, File.separator, "writeDir"));
		localWriteDir.mkdir();

		// Create Format and Reader
		GeoTiffFormat format = new GeoTiffFormat();
		GridCoverageReader reader = format.getReader(tiff);

		// Read Original Coverage.
		GridCoverage2D gridCoverage = (GridCoverage2D) reader.read(null);

		// Set the Crop Envelope
		double xmin = payload.getBounds().getMinx();
		double ymin = payload.getBounds().getMiny();
		double xmax = payload.getBounds().getMaxx();
		double ymax = payload.getBounds().getMaxy();
		
		double[] min = { xmin, ymin };
		double[] max = { xmax, ymax };
		final GeneralEnvelope cropEnvelope = new GeneralEnvelope(min, max);

		// Crop the Raster
		final CoverageProcessor processor = CoverageProcessor.getInstance();
		final ParameterValueGroup param = processor.getOperation("CoverageCrop").getParameters();
		param.parameter("Source").setValue(gridCoverage);
		param.parameter("Envelope").setValue(cropEnvelope);
		GridCoverage2D cropped = (GridCoverage2D) processor.doOperation(param);

		// Writing Cropped Image to File
		String newFilePath = new StringBuilder(localWriteDir.getAbsolutePath()).append(File.separator)
				.append(cropped.getName().toString()).append(".tif").toString();
		final File s3File = new File(newFilePath);
		GridCoverageWriter writer = format.getWriter(s3File);
		try {
			writer.write(cropped, null);
		} catch (IOException e) {
		} finally {
			try {
				writer.dispose();
			} catch (Throwable e) {
			}
		}

		// Persist Cropped Raster to S3 Bucket
		String fileName = writeFileToS3(s3File, fileLocation);

		// Close Things
		if (gridCoverage != null) {
			try {
				// This is essential for releasing locks on image files!
				PlanarImage planarImage = (PlanarImage) gridCoverage.getRenderedImage();
				ImageUtilities.disposePlanarImageChain(planarImage);
				gridCoverage.dispose(false);
			} catch (Throwable t) {
				// ignored
			}
		}

		cropped.dispose(true);
		try {
			if (reader != null)
				reader.dispose();
		} catch (Throwable e) {

		}

		// Delete local temp folder recursively
		deleteDirectoryRecursive(new File(tempTopFolder));

		return getDataSource(fileName, payload);
	}
	
	/**
	 * Getting DataResource for a return type
	 * 
	 * @param id, request
	 * @return DataResource
	 */
	private DataResource getDataSource(String id, RasterCropRequest request) {

		// create data type to return
		S3FileStore s3Store = new S3FileStore();
		s3Store.domainName = request.getSource().getDomain();
		s3Store.bucketName = S3_OUTPUT_BUCKET;
		s3Store.fileName = id;

		RasterDataType dataType = new RasterDataType();
		dataType.setLocation(s3Store);
		dataType.setMimeType("image/tiff");

		ResourceMetadata resourceMetadata = new ResourceMetadata();
		resourceMetadata.name = "External Crop Raster Service";
		resourceMetadata.description = "Service that takes payload containing S3 location and bounding box for some raster file, downloads, crops and uploads the crop back up to s3.";

		DataResource dataSource = new DataResource();
		dataSource.dataType = dataType;
		dataSource.metadata = resourceMetadata;

		return dataSource;
	}
	
	/**
	 * Upload file to s3 bucket
	 * 
	 * @param file the object
	 */
	private String writeFileToS3(File file, FileLocation fileLocation) throws FileNotFoundException {

		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(file.length());
		String fileKey = String.format("%s-%s", uuidFactory.getUUID(), file.getName());

		// BasicAWSCredentials credentials = new BasicAWSCredentials(AMAZONS3_ACCESS_KEY, AMAZONS3_PRIVATE_KEY);
		s3Client = new AmazonS3Client();

		// Making the object public
		PutObjectRequest putObj = new PutObjectRequest(S3_OUTPUT_BUCKET, fileKey, file);
		putObj.setCannedAcl(CannedAccessControlList.PublicRead);
		s3Client.putObject(putObj);

		return fileKey;
	}
	
	/**
	 * Will Copy File from S3 to Local Dir and Return the File
	 * 
	 * @param fileLocation
	 *            Interface to get file info from.
	 * @return File file object
	 * @throws Exception
	 * @throws IOException
	 */
	private File getFileFromS3(FileLocation fileLocation, String serviceId) throws IOException, Exception {

		// Obtain file stream from s3
		FileAccessFactory fileFactory = new FileAccessFactory(AMAZONS3_ACCESS_KEY, AMAZONS3_PRIVATE_KEY);
		File file = new File(String.format("%s_%s%s%s", RASTER_LOCAL_DIRECTORY, serviceId, File.separator,
				fileLocation.getFileName()));

		InputStream inputStream = fileFactory.getFile(fileLocation);
		FileUtils.copyInputStreamToFile(inputStream, file);
		inputStream.close();

		return file;
	}
	
	/**
	 * Recursive deletion of directory
	 * 
	 * @param File
	 *            Directory to be deleted
	 * 
	 * @return boolean if successful
	 * @throws Exception
	 */
	private boolean deleteDirectoryRecursive(File directory) throws Exception {
		boolean result = false;

		if (directory.isDirectory()) {
			File[] files = directory.listFiles();

			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectoryRecursive(files[i]);
				}
				if (!files[i].delete() && files[i].exists())
					throw new Exception(
							"Unable to delete file " + files[i].getName() + " from " + directory.getAbsolutePath());
			}
			result = directory.delete();
		}

		return result;
	}
}
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
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.Future;

import javax.media.jai.PlanarImage;

import org.apache.commons.io.FileUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.resources.image.ImageUtilities;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.coverage.grid.GridCoverageWriter;
import org.opengis.parameter.ParameterValueGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import com.amazonaws.AmazonClientException;

import exception.InvalidInputException;
import generator.model.RasterCropRequest;
import generator.model.ServiceResource;
import model.data.DataResource;
import model.data.location.FileLocation;
import model.data.location.S3FileStore;
import model.data.type.RasterDataType;
import model.job.JobProgress;
import model.job.metadata.ResourceMetadata;
import model.status.StatusUpdate;

/**
 * 
 * Service to crop the raster resource. It will read raster from S3 to local disk, crop it, and upload it back up to the
 * same s3 bucket.
 * 
 * @author Sonny.Saniev
 * 
 */
@Component
public class RasterGenerator {
	@Autowired
	private MongoAccessor mongoAccessor;
	@Autowired
	private S3FileUtility fileUtility;

	@Value("${raster.temp.directory}")
	private String RASTER_LOCAL_DIRECTORY;

	private final static Logger LOGGER = LoggerFactory.getLogger(RasterGenerator.class);
	private long SLEEP_DELAY = 15000;

	/**
	 * Asynchronous handler for cropping the image demonstrating service monitor capabilities of piazza.
	 * 
	 * @return Future
	 */
	@Async
	public Future<String> run(RasterCropRequest payload, String id)
			throws AmazonClientException, InvalidInputException, IOException, InterruptedException {
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
	 * @param RasterCropRequest
	 *            Payload to Describe the Resource Location and Bounding Box.
	 */
	public DataResource cropRasterCoverage(RasterCropRequest payload, String serviceId)
			throws AmazonClientException, InvalidInputException, IOException, InterruptedException {

		// sleeping for 15 seconds for demo and test purposes
		Thread.sleep(SLEEP_DELAY);

		// Read Original File to From S3
		Long fileSize = Long.valueOf(0);
		FileLocation fileLocation = new S3FileStore(payload.getSource().getBucketName(), payload.getSource().getFileName(), fileSize,
				payload.getSource().getDomain());
		File tiff = fileUtility.getFileFromS3(fileLocation, serviceId);
		
System.out.println(tiff.getName() + "-------------------tiff: " + tiff.getAbsolutePath());
		// Create Temporary Local Write Directory
		String tempTopFolder = String.format("%s_%s", RASTER_LOCAL_DIRECTORY, serviceId);
		File localWriteDir = new File(String.format("%s%s%s", tempTopFolder, File.separator, "writeDir"));
		localWriteDir.mkdir();
		
		// Create Format and Reader
		GeoTiffFormat format = new GeoTiffFormat();
		GridCoverageReader reader = format.getReader(tiff);
		
System.out.println(" aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa ");

		// Read Original Coverage.
		GridCoverage2D gridCoverage = (GridCoverage2D) reader.read(null);
		
System.out.println(" bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb ");

		// Set the Crop Envelope
		double xmin = payload.getBounds().getMinx();
		double ymin = payload.getBounds().getMiny();
		double xmax = payload.getBounds().getMaxx();
		double ymax = payload.getBounds().getMaxy();

		double[] min = { xmin, ymin };
		double[] max = { xmax, ymax };
		final GeneralEnvelope cropEnvelope = new GeneralEnvelope(min, max);
		
System.out.println(" ccccccccccccccccccccccccccccccccccccccccccccccccccccc ");

		// Crop the Raster
		final CoverageProcessor processor = CoverageProcessor.getInstance();
		final ParameterValueGroup param = processor.getOperation("CoverageCrop").getParameters();
		param.parameter("Source").setValue(gridCoverage);
		param.parameter("Envelope").setValue(cropEnvelope);
		GridCoverage2D cropped = (GridCoverage2D) processor.doOperation(param);
		
System.out.println(" dddddddddddddddddddddddddddddddddddddddddddddddddddddddddd ");

		// Writing Cropped Image to File
		String newFilePath = new StringBuilder(localWriteDir.getAbsolutePath()).append(File.separator).append(cropped.getName().toString())
				.append(".tif").toString();
		final File s3File = new File(newFilePath);
		GridCoverageWriter writer = format.getWriter(s3File);
		
System.out.println(" eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee " + cropped.getName().toString());

		try {
			
System.out.println(" fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff ");
			writer.write(cropped, null);
System.out.println(" gggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggg ");

		} catch (IllegalArgumentException | IOException e) {
			LOGGER.warn("Error writing Grid Coverage file.", e);
		} finally {
			try {
				writer.dispose();
			} catch (Exception e) {
				LOGGER.warn("Error disposing of Grid Writer.", e);
			}
		}

		// Persist Cropped Raster to S3 Bucket
		String fileName = fileUtility.writeFileToS3(s3File, fileLocation);

		// Close Things
		if (gridCoverage != null) {
			try {
				// This is essential for releasing locks on image files!
				PlanarImage planarImage = (PlanarImage) gridCoverage.getRenderedImage();
				ImageUtilities.disposePlanarImageChain(planarImage);
				gridCoverage.dispose(false);
			} catch (Exception t) {
				LOGGER.error("Error releasing locks on Grid Coverage files.", t);
			}
		}

		cropped.dispose(true);
		try {
			if (reader != null)
				reader.dispose();
		} catch (Exception e) {
			LOGGER.error("Error disposing the Grid Coverage Reader.", e);
		}

		// Delete local temp folder recursively
		deleteDirectoryRecursive(new File(tempTopFolder));

		return getDataSource(fileName, payload);
	}

	/**
	 * Getting DataResource for a return type
	 * 
	 * @param id,
	 *            request
	 * @return DataResource
	 */
	private DataResource getDataSource(String id, RasterCropRequest request) {

		// create data type to return
		S3FileStore s3Store = new S3FileStore();
		s3Store.domainName = request.getSource().getDomain();
		s3Store.bucketName = S3FileUtility.S3_OUTPUT_BUCKET;
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
	 * Recursive deletion of directory
	 * 
	 * @param File
	 *            Directory to be deleted
	 * 
	 * @return boolean if successful
	 * @throws IOException
	 * @throws Exception
	 */
	private boolean deleteDirectoryRecursive(File directory) throws IOException {
		boolean result = false;

		if (directory.isDirectory()) {
			File[] files = directory.listFiles();

			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectoryRecursive(files[i]);
				}
				if (!files[i].delete() && files[i].exists())
					throw new IOException("Unable to delete file " + files[i].getName() + " from " + directory.getAbsolutePath());
			}
			result = directory.delete();
		}

		return result;
	}
}
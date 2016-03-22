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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.coverage.grid.GridCoverageWriter;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;

import generator.model.RasterCropRequest;
import model.data.DataResource;
import model.data.location.FileAccessFactory;
import model.data.location.FileLocation;
import model.data.location.S3FileStore;
import model.data.type.RasterDataType;
import model.job.metadata.ResourceMetadata;
import util.UUIDFactory;

import org.geotools.geometry.GeneralEnvelope;

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

	@Value("${s3.key.access:}")
	private String AMAZONS3_ACCESS_KEY;
	@Value("${s3.key.private:}")
	private String AMAZONS3_PRIVATE_KEY;
	@Value("${raster.temp.directory}")
	private String RASTER_LOCAL_DIRECTORY;

	private AmazonS3 s3Client;

	private static final String S3_OUTPUT_BUCKET = "pz-svcs-prevgen-output";
	
	/**
	 * Create a cropped coverage.
	 * 
	 * @param RasterCropRequest Payload to Describe the Resource Location and Bounding Box.
	 * 
	 * @throws Exception 
	 */
	public DataResource cropRasterCoverage(RasterCropRequest request) throws Exception {

		// Read Original File to From S3
		FileLocation fileLocation = new S3FileStore(request.getSource().getBucketName(), request.getSource().getFileName(), request.getSource().getDomain());
		File tiff = getFileFromS3(fileLocation);

		// Create Temporary Local Write Directory
		File localWriteDir = new File(String.format("%s%s%s", RASTER_LOCAL_DIRECTORY, File.separator, "writeDir"));
		localWriteDir.mkdir();

		// Create Format and Reader
		final GeoTiffFormat format = new GeoTiffFormat();
		GridCoverageReader reader = format.getReader(tiff);

		// Read Original Coverage.
		GridCoverage2D gridCoverage = (GridCoverage2D) reader.read(null);
		
		// Set the Crop Envelope
		double xmin = request.getBounds().getMinx();
		double ymin = request.getBounds().getMiny();
		double xmax = request.getBounds().getMaxx();
		double ymax = request.getBounds().getMaxy();
		final GeneralEnvelope cropEnvelope = new GeneralEnvelope(new double[] { xmin, ymin}, new double[] { xmax, ymax});

		// Crop the Raster
		final CoverageProcessor processor = CoverageProcessor.getInstance();
		final ParameterValueGroup param = processor.getOperation("CoverageCrop").getParameters();
		param.parameter("Source").setValue(gridCoverage);
		param.parameter("Envelope").setValue(cropEnvelope);
		final GridCoverage2D cropped = (GridCoverage2D) processor.doOperation(param);

		// Writing Cropped Image to File
		String newFilePath = new StringBuilder(localWriteDir.getAbsolutePath()).append(File.separator).append(cropped.getName().toString()).append(".tif").toString();
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
		cropped.dispose(true);
		gridCoverage.dispose(true);
		try {
			if (reader != null)
				reader.dispose();
		} catch (Throwable e) {

		}
//		writer = null;
//		cropped = null;
//		gridCoverage = null;

		// Delete local raster
		//System.gc(); //Java Garbage Collector
		tiff.delete();

		return getDataSource(fileName, request);
	}
	
	/**
	 * Getting DataResource as a return type
	 * 
	 * @param id, request
	 * @return DataResource
	 */
	private DataResource getDataSource(String id, RasterCropRequest request){
		
		//create data type to return
		S3FileStore s3Store = new S3FileStore();
		s3Store.domainName = request.getSource().getDomain();
		s3Store.bucketName = request.getSource().getBucketName();
		s3Store.fileName = id;
		
		RasterDataType dataType = new RasterDataType();
		dataType.setLocation(s3Store);
		dataType.setMimeType("image/tiff");
		
		ResourceMetadata resourceMetadata = new ResourceMetadata();
		resourceMetadata.name = "External Crop Raster Service";
		resourceMetadata.description = "Service that takes payload containing S3 location and bounding box for some raster file, downloads, crops and uploads the crop back up to s3.";
		resourceMetadata.url = "http://host:8086/crop";
		resourceMetadata.method = "POST";
		resourceMetadata.id = id;
		
		DataResource dataSource = new DataResource();
		dataSource.dataType=dataType;
		dataSource.metadata = resourceMetadata;
		
		return dataSource;
	}
	
	/**
	 * Upload file to an s3 bucket
	 * 
	 * @param file the object
	 */
	private String writeFileToS3(File file, FileLocation fileLocation) throws FileNotFoundException{
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(file.length());

		String fileKey = String.format("%s-%s", uuidFactory.getUUID(), file.getName());
		
		BasicAWSCredentials credentials = new BasicAWSCredentials(AMAZONS3_ACCESS_KEY, AMAZONS3_PRIVATE_KEY);
		s3Client = new AmazonS3Client(credentials);
		InputStream inputStream = new FileInputStream(file);
		s3Client.putObject(S3_OUTPUT_BUCKET, fileKey, inputStream, metadata);
		
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
	private File getFileFromS3(FileLocation fileLocation) throws IOException, Exception {
		
		// Get file stream from AWS S3
		FileAccessFactory fileFactory = new FileAccessFactory(AMAZONS3_ACCESS_KEY, AMAZONS3_PRIVATE_KEY);
		File file = new File(String.format("%s%s%s", RASTER_LOCAL_DIRECTORY, File.separator, fileLocation.getFileName()));
		//file.createNewFile();

		InputStream inputStream = fileFactory.getFile(fileLocation);
		FileUtils.copyInputStreamToFile(inputStream, file);
		inputStream.close();

		return file;
	}
}
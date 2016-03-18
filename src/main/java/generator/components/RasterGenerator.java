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
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.coverage.grid.GridCoverageWriter;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;

import model.data.location.FileAccessFactory;
import model.data.location.FileLocation;
import model.data.location.S3FileStore;
import org.geotools.geometry.GeneralEnvelope;

/**
 * 
 * @author Sonny.Saniev
 * 
 */
@Component
public class RasterGenerator {

//	@Autowired
//	private UUIDFactory uuidFactory;

	@Value("${s3.key.access:}")
	private String AMAZONS3_ACCESS_KEY;
	@Value("${s3.key.private:}")
	private String AMAZONS3_PRIVATE_KEY;
	@Value("${raster.temp.directory}")
	private String RASTER_LOCAL_DIRECTORY;

	private AmazonS3 s3Client;
	
	private static final String AWS_BUCKET_NAME = "external-public-access-test";
	private static final String AWS_DOMAIN = "s3.amazonaws.com";

	public String cropGeoTiff() {
		
		return "true";
	}
	
	/**
	 * Create a cropped coverage.
	 * @throws Exception 
	 */
	public void cropRasterCoverage() throws Exception {

		// Grab the original file to want to crop from s3
		//FileLocation fileLocation = new S3FileStore("external-public-access-test", "NASA-GDEM-10km-colorized.tif", "s3.amazonaws.com");
		FileLocation fileLocation = new S3FileStore("external-public-access-test", "nasa_land_ocean_ice.tif", "s3.amazonaws.com");
		File tiff = getFileFromS3(fileLocation);
		//assert tiff.exists() && tiff.canRead() && tiff.isFile();

		// create temporary local write directory
		File localWriteDir = new File(String.format("%s%s%s", RASTER_LOCAL_DIRECTORY, File.separator, "writeDir"));
		localWriteDir.mkdir();

		// Create format and reader
		final GeoTiffFormat format = new GeoTiffFormat();
		GridCoverageReader reader = format.getReader(tiff);

		// Read the original coverage.
		GridCoverage2D gc = (GridCoverage2D) reader.read(null);
	
		//final CoordinateReferenceSystem sourceCRS = gc.getCoordinateReferenceSystem2D();
		final GeneralEnvelope sourceEnvelope = (GeneralEnvelope) gc.getEnvelope();
		//final GridGeometry2D sourcedGG = (GridGeometry2D) gc.getGridGeometry();
		//final MathTransform sourceG2W = sourcedGG.getGridToCRS(PixelInCell.CELL_CENTER);

		// Crop the raster
		double xc = sourceEnvelope.getMedian(0);
		double yc = sourceEnvelope.getMedian(1);
		double xl = sourceEnvelope.getSpan(0);
		double yl = sourceEnvelope.getSpan(1);
		final GeneralEnvelope cropEnvelope = new GeneralEnvelope(new double[] { xc - xl / 4.0, yc - yl / 4.0 }, new double[] { xc + xl / 4.0, yc + yl / 4.0 });
		final CoverageProcessor processor = CoverageProcessor.getInstance();
		final ParameterValueGroup param = processor.getOperation("CoverageCrop").getParameters();
		param.parameter("Source").setValue(gc);
		param.parameter("Envelope").setValue(cropEnvelope);
		final GridCoverage2D cropped = (GridCoverage2D) processor.doOperation(param);

		// checking the ranges of the output image.
		//final GridGeometry2D croppedGG = (GridGeometry2D) cropped.getGridGeometry();
		//final GridEnvelope croppedGR = croppedGG.getGridRange();
		//final MathTransform croppedG2W = croppedGG.getGridToCRS(PixelInCell.CELL_CENTER);
		//final GeneralEnvelope croppedEnvelope = (GeneralEnvelope) cropped.getEnvelope();
		
		// Check that the envelope is correct
		//final GeneralEnvelope expectedEnvelope = new GeneralEnvelope(croppedGR,PixelInCell.CELL_CENTER, croppedG2W, cropped.getCoordinateReferenceSystem2D());
		//Assert.assertTrue("Expected envelope is different from the computed one",expectedEnvelope.equals(croppedEnvelope, XAffineTransform.getScale((AffineTransform) croppedG2W) / 2.0, false));

		// WRITING AND TESTING
		String newFilePath = new StringBuilder(localWriteDir.getAbsolutePath()).append(File.separator).append(cropped.getName().toString()).append(".tif").toString();
		final File s3File = new File(newFilePath);
		final GridCoverageWriter writer = format.getWriter(s3File);

		// Write cropped raster into the file
		try {
			writer.write(cropped, null);
		} catch (IOException e) {
		} finally {
			try {
				writer.dispose();
			} catch (Throwable e) {
			}
		}

		// persist new raster
		writeFileToS3(s3File);

		// Close things
		// Dispose things
		cropped.dispose(true);
		gc.dispose(true);
		
		try {
			if (reader != null)
				reader.dispose();
		} catch (Throwable e) {
		}

		// Delete local raster
		//System.gc(); //java garbage collector
		tiff.delete();
	}
	
	/**
	 * Upload file to an s3 bucket
	 * 
	 * @param file the object
	 */
	private void writeFileToS3(File file) throws FileNotFoundException{
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(file.length());
		
		// fix the id, either uuid gen or use dataresource to pull from.
		Long randdom = Long.valueOf((int)(Math.random()*100000));
		String fileKey = String.format("%s-%s-%s", "croppedRaster", randdom.toString(), file.getName());
		
		BasicAWSCredentials credentials = new BasicAWSCredentials(AMAZONS3_ACCESS_KEY, AMAZONS3_PRIVATE_KEY);
		s3Client = new AmazonS3Client(credentials);
		InputStream inputStream = new FileInputStream(file);
		s3Client.putObject(AWS_BUCKET_NAME, fileKey, inputStream, metadata);
	}
	
	/**
	 * Will copy file from s3 to local dir and return the file
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
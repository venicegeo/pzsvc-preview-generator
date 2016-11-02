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
package generator;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.AmazonClientException;

import exception.InvalidInputException;
import generator.components.MongoAccessor;
import generator.components.RasterGenerator;
import generator.components.S3FileUtility;
import generator.model.BoundingBoxInfo;
import generator.model.RasterCropRequest;
import generator.model.S3StoreInfo;
import model.data.DataResource;
import util.UUIDFactory;

/**
 * Tests the Raster Generator component
 * 
 * @author Patrick.Doody
 *
 */
public class RasterGeneratorTests {
	@Mock
	private UUIDFactory uuidFactory;
	@Mock
	private MongoAccessor mongoAccessor;
	@Mock
	private S3FileUtility fileUtility;
	@InjectMocks
	private RasterGenerator rasterGenerator;

	private RasterCropRequest mockRequest = new RasterCropRequest();
	private String testDataPath;
	private File testFile;

	/**
	 * Initialized
	 */
	@Before
	public void setup() throws AmazonClientException, InvalidInputException, IOException {
		MockitoAnnotations.initMocks(this);
		ReflectionTestUtils.setField(rasterGenerator, "RASTER_LOCAL_DIRECTORY", "tmp");
		ReflectionTestUtils.setField(rasterGenerator, "SLEEP_DELAY", 1000);

		// Initialize common test ata
		mockRequest.setFunction("crop");
		BoundingBoxInfo boundingBox = new BoundingBoxInfo();
		boundingBox.setMinx(-140.00);
		boundingBox.setMaxx(-60.00);
		boundingBox.setMiny(10.00);
		boundingBox.setMaxy(70.00);
		mockRequest.setBounds(boundingBox);
		S3StoreInfo storeInfo = new S3StoreInfo();
		storeInfo.setBucketName("test");
		storeInfo.setDomain("test");
		storeInfo.setFileName("test");
		mockRequest.setSource(storeInfo);

		// Set the test data path
		testDataPath = "src" + File.separator + "test" + File.separator + "resources" + File.separator + "world.tif";
		testFile = new File(testDataPath);
		
System.out.println(testFile.getName() + "-------------------testFile: " + testFile.getAbsolutePath());

		// When the Raster Generator attempts to get the file, return our test file
		Mockito.doReturn(testFile).when(fileUtility).getFileFromS3(Mockito.any(), Mockito.anyString());

		// Report a file name pushed to S3 when run is complete
		Mockito.doReturn("Output.tif").when(fileUtility).writeFileToS3(Mockito.any(), Mockito.any());
	}

	/**
	 * Tests cropping the test raster file
	 */
	@Test
	public void testCropRaster() throws AmazonClientException, InvalidInputException, IOException, InterruptedException {
		DataResource dataResource = rasterGenerator.cropRasterCoverage(mockRequest, "123456");
		assertTrue(dataResource != null);
	}
}

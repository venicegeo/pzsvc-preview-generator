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

import org.junit.Before;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import generator.components.MongoAccessor;
import generator.components.RasterGenerator;
import util.UUIDFactory;

/**
 * Tests the Raster Generator component
 * 
 * @author Patrick.Doody
 *
 */
public class RasterGeneratorTests {
	@Autowired
	private UUIDFactory uuidFactory;
	@Autowired
	private MongoAccessor mongoAccessor;
	@InjectMocks
	private RasterGenerator rasterGenerator;

	/**
	 * Initialized
	 */
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		ReflectionTestUtils.setField(rasterGenerator, "RASTER_LOCAL_DIRECTORY", "tmp");
	}
	
	

}

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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 
 * @author Sonny.Saniev
 * 
 */
@Component
public class RasterGenerator {
//	@Value("${s3.key.access:}")
//	private String AMAZONS3_ACCESS_KEY;
//	@Value("${s3.key.private:}")
//	private String AMAZONS3_PRIVATE_KEY;

	private static final String STATIC_LOCAL_FIELD = "URL";
	
	public String cropGeoTiff() {

		return "true";
	}
}
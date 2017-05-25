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
package generator.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Sonny.Saniev
 *
 * The model for payload
 */
public class RasterCropRequest {
	@NotNull
	public S3StoreInfo source;
	
	@NotNull
	public String function;
	
	@NotNull
	public BoundingBoxInfo bounds;
	
	public S3StoreInfo getSource() {
		return source;
	}
	public void setSource(S3StoreInfo source) {
		this.source = source;
	}
	public String getFunction() {
		return function;
	}
	public void setFunction(String function) {
		this.function = function;
	}
	public BoundingBoxInfo getBounds() {
		return bounds;
	}
	public void setBounds(BoundingBoxInfo bounds) {
		this.bounds = bounds;
	}
}

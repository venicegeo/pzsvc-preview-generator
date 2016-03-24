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
package generator.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import generator.components.RasterGenerator;
import generator.model.RasterCropRequest;
import model.data.DataResource;

/**
 * Handles raster resource payload requests, and processes them from s3.
 * 
 * The controller acts as an entry point for a service that accepts payload containing 
 * the location of s3 raster file, crops the raster into provided 
 * bounding box and stores it back in s3 buckets.
 * 
 * @author Sonny.Saniev
 * 
 */

@RestController
public class ServiceEntrypoint {

	@Autowired 
	RasterGenerator rasterGenerator;
	
	/*
	 * Entry point for raw post accepting s3 location of the raster resource and bounding box 
	 * to parse and return the location of the newly created s3 resource with the given bounding box.
	 *  
	 * @param RasterCropRequest
	 *            The Json Payload
	 * @return DataResource object.
	 */
	@RequestMapping(value = "/crop", method = RequestMethod.POST)
	public DataResource processRasterResouceRawPost(@RequestBody RasterCropRequest request) {
		DataResource dataResource = new DataResource();
			try {
				dataResource = rasterGenerator.cropRasterCoverage(request);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return dataResource;
	}
	
	/*
	 * Entry point for form-data post accepting s3 location of the raster resource and bounding box 
	 * to parse and return the location of the newly created s3 resource with the given bounding box.
	 *  
	 * @param body
	 *            The Json Payload
	 * @return DataResource object.
	 */
	@RequestMapping(value = "/cropFormData", method = RequestMethod.POST)
	public DataResource processRasterResouce(@RequestParam(required = true) String body) {
		DataResource dataResource = new DataResource();
		try {
			// Parse the Request String
			RasterCropRequest request = new ObjectMapper().readValue(body, RasterCropRequest.class);
			dataResource = rasterGenerator.cropRasterCoverage(request);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return dataResource;
	}
	
	/**
	 * 
	 * Info endpoint for the service to see if it is running.
	 * @return String true
	 */
	@RequestMapping(value = "/info", method = RequestMethod.GET)
	public String getInfo() {
		return "true";
	}

}

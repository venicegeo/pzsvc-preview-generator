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

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import generator.components.RasterGenerator;
import generator.model.RasterCropRequest;
import model.data.DataResource;
import model.data.type.RasterDataType;
import model.data.type.TextDataType;

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
	@RequestMapping(value = "/crop", method = RequestMethod.POST, produces={"application/json; charset=UTF-8"})
	public String processRasterResouceRawPost(@RequestBody RasterCropRequest request) {
		DataResource dataResource=null;
		try {
				dataResource = rasterGenerator.cropRasterCoverage(request);
	
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		ObjectMapper mapper = new ObjectMapper();
		String responseString = "";
		try {
			 if (dataResource != null) {
			    responseString = mapper.writeValueAsString(dataResource);
			 }
			 else
				 responseString = "";
		}
		catch (JsonProcessingException ex) {
			
		}
		return responseString;
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
			//dataResource = rasterGenerator.cropRasterCoverage(request);
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
	@RequestMapping(value = "/test", method = RequestMethod.GET)
	public String getInfo() {
		
		ClassLoader classLoader = getClass().getClassLoader();
		String payload="";
		try {
			payload = IOUtils.toString(classLoader.getResourceAsStream("templates/payload.json"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		HttpHeaders headers = new HttpHeaders();
		//headers.add("Authorization", "Basic " + credentials);
		headers.setContentType(MediaType.APPLICATION_JSON);

		// Create the Request template and execute
		HttpEntity<String> request = new HttpEntity<String>(payload, headers);

		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<DataResource> response = restTemplate.exchange("http://localhost:8086/crop", HttpMethod.POST, request, DataResource.class);
		
		//System.out.println(" WORKS ----------------------------------- " + response.getBody().getDataType().getType() );
		
		return "true";
	}

}

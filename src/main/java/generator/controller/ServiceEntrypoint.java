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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import generator.components.RasterGenerator;
import generator.model.RasterCropRequest;
import model.request.FileRequest;

/**
 * Handles raster location payloads, and processes them from s3.
 * 
 * The controller accepts payload containing the location of s3 raster file
 * crops the raster into provided bounding box and stores it back up in s3 buckets.
 * 
 * @author Sonny.Saniev
 * 
 */

@RestController
public class ServiceEntrypoint {

	@Autowired 
	RasterGenerator rasterGenerator;
	
	/*
	 * Entry point for accepting s3 location of the raster resource and bounding box 
	 * to parse and return the location of the newly created s3 resource with the given bounding box.
	 *  
	 * @param body
	 *            The Json Payload
	 * @return Response object.
	 */
	@RequestMapping(value = "/crop", method = RequestMethod.POST)
	public String processRasterResouce(@RequestParam(required = true) String body) {

		try {
			// Parse the Request String
			RasterCropRequest request = new ObjectMapper().readValue(body, RasterCropRequest.class);
			System.out.println("\n---------------------------------\n /crop payload body: \n" + request.function +  " -- " + request.bounds.maxx + " -- " + request.source.bucketName);
			
			rasterGenerator.cropRasterCoverage(request);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return "new raster is on s3.";
	}
	
	@RequestMapping(value = "/cropRaw", method = RequestMethod.POST)
	public String processRasterResouceRawType(@RequestBody String body) {

		try {
			// Parse the Request String
			RasterCropRequest request = new ObjectMapper().readValue(body, RasterCropRequest.class);
			System.out.println("\n---------------------------------\n /crop payload body: \n" + request.function +  " -- " + request.bounds.maxx + " -- " + request.source.bucketName);
			
			rasterGenerator.cropRasterCoverage(request);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return "new raster is on s3.";
	}
	
//	@RequestMapping(value = "/string/convert", method = RequestMethod.POST, headers="Accept=application/json", produces=MediaType.APPLICATION_JSON_VALUE)
//	public @ResponseBody String convert(@RequestBody Message msg) {
//		
//		String result = "Could not Convert, please check message";
//		String conversionType = msg.getConversionType();
//		String theString = msg.gettheString();
//		if ((conversionType != null) && (theString != null)) {
//			if (conversionType.equals(Message.UPPER))  {
//				LOGGER.info("Make the String uppercase" + theString);
//				LOGGER.info("The message" + msg);
//		        result=convertStringtoUpper(theString);
//			} 
//			else if (conversionType.equals(Message.LOWER))  {
//				LOGGER.info("Make the String lower case" + theString);
//				result=convertStringtoLower(theString);
//		       
//			}
//		}
//		
//		return result;
//
//		
//	}
	
	
	
	
}

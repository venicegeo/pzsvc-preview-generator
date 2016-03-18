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

import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.ParseException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import generator.components.RasterGenerator;


@RestController
public class ServiceEntrypoint {

	@Autowired 
	RasterGenerator rasterGenerator;
	
	/*
	 * Entry point for accepting s3 location of the raster resource and bounding box 
	 * to parse and return the location of the newly created s3 resource with the given bounding box. 
	 */
//	@RequestMapping(value = "/demo", method = RequestMethod.GET)
//	public String demo() {
//		System.out.println("pz-services-prevgen controller trigger...");
//		
//		return "pz-services-prevgen controller trigger..." + rasterGenerator.testDemo();
//	}

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
			rasterGenerator.testWriteCroppedCoverage();
		} catch (Exception e) {
			
			System.out.println("Error error... shutting down....");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//System.out.println("----------------------------------------------------------------\n" + body);
		return "new raster is on s3.";
	}

//	@RequestMapping(value = "/crop", method = RequestMethod.POST)
//	public ResponseEntity<PiazzaResponse> cropRaster(@RequestParam(required = true) String body, @RequestParam(required = false) final MultipartFile file) {
//
//		// Deserialize the incoming JSON to Request Model objects
//		PiazzaJobRequest request;
//		try {
//			request = JobMessageFactory.parseRequestJson(body);
//		} catch (Exception exception) {
//			logger.log(String.format("An Invalid Job Request sent to the Gateway: %s", exception.getMessage()),
//					PiazzaLogger.WARNING);
//			return new ResponseEntity<PiazzaResponse>(new ErrorResponse(null, "Error Parsing Job Request: "
//					+ exception.getMessage(), "Gateway"), HttpStatus.BAD_REQUEST);
//		}
//
//		// Authenticate and Authorize the request
//		try {
//			AuthConnector.verifyAuth(request);
//		} catch (SecurityException securityEx) {
//			logger.log("Non-authorized connection to Gateway Blocked.", PiazzaLogger.WARNING);
//			return new ResponseEntity<PiazzaResponse>(new ErrorResponse(null, "Authentication Error", "Gateway"),
//					HttpStatus.UNAUTHORIZED);
//		}
//
//		// Determine if this Job is processed via synchronous REST, or via Kafka
//		// message queues.
//		if (isSynchronousJob(request.jobType)) {
//			return sendRequestToDispatcherViaRest(request);
//		} else {
//			return sendRequestToDispatcherViaKafka(request, file);
//		}
//	}
}

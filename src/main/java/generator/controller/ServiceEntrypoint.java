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

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import generator.components.RasterGenerator;
import generator.components.ServiceThreadManager;
import generator.model.ErrorResponse;
import generator.model.RasterCropRequest;
import model.data.DataResource;
import model.logger.AuditElement;
import model.logger.Severity;
import model.response.JobResponse;
import model.response.PiazzaResponse;
import model.status.StatusUpdate;
import util.PiazzaLogger;
import util.UUIDFactory;

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
	private RasterGenerator rasterGenerator;
	@Autowired 
	private ServiceThreadManager serviceThreadManager;
	@Autowired
	private UUIDFactory uuidFactory;
	@Autowired
	private PiazzaLogger pzLogger;
	
	private final static Logger LOGGER = LoggerFactory.getLogger(ServiceEntrypoint.class);
	
	/**
	 * Entry point for raw post accepting s3 location of the raster resource and bounding box 
	 * to parse and return the location of the newly created s3 resource with the given bounding box.
	 *  
	 * @param RasterCropRequest
	 *            Required payload
	 * @return DataResource object or an error.
	 */
	@RequestMapping(value = "/crop", method = RequestMethod.POST, produces={"application/json; charset=UTF-8"})
	public ResponseEntity<?> processRasterResouceRawPost2(@RequestBody RasterCropRequest request) {
		
		String serviceId = uuidFactory.getUUID();
		DataResource dataResource=null;
		
		pzLogger.log("Cropping raster non-async endpoint", Severity.INFORMATIONAL);

		try {
				dataResource = rasterGenerator.cropRasterCoverage(request, serviceId);
			} catch (Exception e) {
				LOGGER.error("Error Cropping Raster.", e);
				return new ResponseEntity<ErrorResponse>(new ErrorResponse(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
			}
		
		if (dataResource == null) {
			pzLogger.log("Error occurred, data resource is empty.", Severity.ERROR, new AuditElement("pz-svcs-prevgen", "errorNonAsyncRasterCrop", serviceId));
			return new ResponseEntity<ErrorResponse>(new ErrorResponse("Unknown error, data resource is empty."), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return new ResponseEntity<DataResource>(dataResource, HttpStatus.OK);
	}

	/**
	 * Endpoint for curl integration tests. Do not erase!
	 * @throws Exception 
	 */
	@RequestMapping(value = "/crop2", method = RequestMethod.POST, produces={"application/json; charset=UTF-8"})
	public ResponseEntity<?> processRasterResouceRawPost4(@RequestBody RasterCropRequest request) throws Exception {
		throw new Exception("Oh no, bad stuff happened!!!");
	}
	
	/**
	 * Entry point for accepting s3 location of the raster resource and bounding 
	 * box info to parse and return the location of the newly cropped s3 resource.
	 * This endpoint runs the crop service asynchronously on a new thread.
	 *  
	 * @param RasterCropRequest
	 *            Required payload
	 * @return ResponseEntity<?> of JobResponse or ErrorResponse
	 */
	@RequestMapping(value = "/cropasync", method = RequestMethod.POST, produces={"application/json; charset=UTF-8"})
	public ResponseEntity<?> processRasterAsync(@RequestBody RasterCropRequest request) {
		pzLogger.log("Cropping raster async endpoint", Severity.INFORMATIONAL);
		try {
			JobResponse job = new JobResponse(serviceThreadManager.processRasterAsync(request));
			return new ResponseEntity<JobResponse>(job, HttpStatus.OK);
		} catch (Exception e) {
			LOGGER.error("Error while cropping raster.", e);
			pzLogger.log(String.format("Error while cropping raster. %s", e.getMessage()), Severity.ERROR, new AuditElement("pz-svcs-prevgen", "errorAsyncRasterCrop", "error"));
			return new ResponseEntity<ErrorResponse>(new ErrorResponse(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Returns the status of the running process.
	 * 
	 * @param serviceId
	 * @return
	 */
	@RequestMapping(value = "/cropasync/status/{serviceId}", method = RequestMethod.GET, produces={"application/json; charset=UTF-8"})
	public ResponseEntity<?> processRasterAsyncGetStatus(@PathVariable(value = "serviceId") String serviceId) {
		pzLogger.log(String.format("Getting the status of running service %s", serviceId), Severity.INFORMATIONAL);
		try {
			return new ResponseEntity<StatusUpdate>(serviceThreadManager.getJobStatus(serviceId), HttpStatus.OK);
		} catch (Exception e) {
			LOGGER.error("Error Getting Status.", e);
			pzLogger.log(String.format("Error Getting Status. %s", ExceptionUtils.getStackTrace(e)), Severity.ERROR, new AuditElement("pz-svcs-prevgen", "errorGettingStatus", serviceId));
			return new ResponseEntity<ErrorResponse>(new ErrorResponse(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}

	/**
	 * Returns the result of the completed process.
	 * 
	 * @param serviceId
	 * @return
	 */
	@RequestMapping(value = "/cropasync/result/{serviceId}", method = RequestMethod.GET, produces={"application/json; charset=UTF-8"})
	public ResponseEntity<?> processRasterAsyncGetResult(@PathVariable(value = "serviceId") String serviceId) {
		pzLogger.log(String.format("Getting the result of service %s", serviceId), Severity.INFORMATIONAL);
		try {
			return new ResponseEntity<DataResource>(serviceThreadManager.getServiceResult(serviceId), HttpStatus.OK);
		} catch (Exception e) {
			LOGGER.error("Error Getting Result.", e);
			pzLogger.log(String.format("Error Getting Result %s", e.getMessage()), Severity.ERROR, new AuditElement("pz-svcs-prevgen", "errorGettingResult", serviceId));
			return new ResponseEntity<ErrorResponse>(new ErrorResponse(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Delete running job and stop the thread if possible
	 * 
	 * @param serviceId
	 * @return
	 */
	@RequestMapping(value = "/cropasync/job/{serviceId}", method = RequestMethod.DELETE, produces={"application/json; charset=UTF-8"})
	public ResponseEntity<?> processRasterAsyncDeleteJob(@PathVariable(value = "serviceId") String serviceId) {
		pzLogger.log(String.format("Deleting running job %s", serviceId), Severity.INFORMATIONAL);
		try {
			serviceThreadManager.deleteService(serviceId);
			return new ResponseEntity(HttpStatus.OK);
		} catch (Exception e) {
			LOGGER.error("Error Cancelling Job.", e);
			pzLogger.log(String.format("Deleting running job %s", e.getMessage()), Severity.ERROR, new AuditElement("pz-svcs-prevgen", "errorDeletingJob", serviceId));
			return new ResponseEntity<ErrorResponse>(new ErrorResponse(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	/**
	 * Info endpoint for the service to see if it is running.
	 * 
	 * @return String true
	 */
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String getInfo() {
		return "Greetings Earthlings. pz-svcs-prevgen is alive!";
	}
}

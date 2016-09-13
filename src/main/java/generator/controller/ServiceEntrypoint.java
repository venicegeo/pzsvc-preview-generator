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
import model.response.JobResponse;
import model.status.StatusUpdate;

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
		DataResource dataResource=null;
		try {
				dataResource = rasterGenerator.cropRasterCoverage(request, "123456");
			} catch (Exception e) {
				e.printStackTrace();
				return new ResponseEntity<ErrorResponse>(new ErrorResponse(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
			}
		
		if (dataResource == null) {
			return new ResponseEntity<ErrorResponse>(new ErrorResponse("Unknown error, data resource is empty."), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return new ResponseEntity<DataResource>(dataResource, HttpStatus.OK);
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
		try {
			JobResponse job = new JobResponse(serviceThreadManager.processRasterAsync(request));
			return new ResponseEntity<JobResponse>(job, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
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
		try {
			return new ResponseEntity<StatusUpdate>(serviceThreadManager.getJobStatus(serviceId), HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<ErrorResponse>(new ErrorResponse(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
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
		try {
			return new ResponseEntity<DataResource>(serviceThreadManager.getServiceResult(serviceId), HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
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
		try {
			serviceThreadManager.deleteService(serviceId);
			return new ResponseEntity(HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
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

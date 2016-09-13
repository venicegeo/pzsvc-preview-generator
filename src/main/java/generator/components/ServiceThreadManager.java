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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import generator.model.RasterCropRequest;
import generator.model.ServiceResource;
import model.data.DataResource;
import model.job.JobProgress;
import model.status.StatusUpdate;
import util.UUIDFactory;

@Component
public class ServiceThreadManager {

	@Autowired
	private UUIDFactory uuidFactory;
	
	@Autowired
	private MongoAccessor mongoAccessor;
	
	@Autowired
	private RasterGenerator rasterGenerator;

	private Map<String, Future<?>> threadMap;

	/**
	 * Initializing
	 */
	@PostConstruct
	public void initialize() {
		// Initialize the HashMap
		threadMap = new HashMap<String, Future<?>>();
	}

	/**
	 * Asynchronously processing raster image
	 * @throws Exception 
	 */
	public String processRasterAsync(RasterCropRequest payload) throws Exception {
		String id = uuidFactory.getUUID();
		
		// No need to keep track of threads for now
		rasterGenerator.run(payload, id);
		
		return id;
	}

	/**
	 * Returns job status
	 * 
	 * @throws Exception
	 */
	public StatusUpdate getJobStatus(String serviceId) throws Exception {
		return mongoAccessor.getServiceResourceById(serviceId).getStatus();
	}

	/**
	 * Returns job result
	 * 
	 * @throws Exception
	 */
	public DataResource getServiceResult(String serviceId) throws Exception {
		return mongoAccessor.getServiceResourceById(serviceId).getResult();
	}
	
	/**
	 * Delete running job and stop the thread if possible
	 * 
	 * @throws Exception
	 */
	public void deleteService(String serviceId) throws Exception {
		mongoAccessor.removeJob(serviceId);
	}
}

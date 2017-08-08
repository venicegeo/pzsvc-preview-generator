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

import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import generator.model.ServiceResource;

/**
 * In-memory data store for testing Piazza Service features.
 * 
 * @see pz-jobmanager implementation as main reference.
 * 
 */
@Component
public class DataAccessor {
	/**
	 * In-memory data store.
	 */
	private Map<String, ServiceResource> data = new HashMap<String, ServiceResource>();

	public DataAccessor() {
		// Expected for Component instantiation
	}

	/**
	 * Returns a Job that matches the specified Id.
	 * 
	 * @param jobId
	 *            Job Id
	 * @return The Job with the specified Id
	 * @throws InterruptedException
	 */
	public ServiceResource getServiceResourceById(String serviceResourceId) throws ResourceAccessException, InterruptedException {
		if (data.containsKey(serviceResourceId)) {
			return data.get(serviceResourceId);
		} else {
			return null;
		}
	}

	/**
	 * 
	 * @param sMetadata
	 * @return
	 */
	public String update(ServiceResource serviceResource) {
		data.put(serviceResource.getServiceResourceId(), serviceResource);
		return serviceResource.getServiceResourceId();
	}

	/**
	 * Deletes a Job entry.
	 * 
	 * @param serviceId
	 *            The Id of the job to delete
	 */
	public void removeJob(String serviceResourceId) {
		data.remove(serviceResourceId);
	}

	/**
	 * Adds a Job
	 * 
	 * @param job
	 *            The Job
	 */
	public void addServiceResource(ServiceResource resource) {
		data.put(resource.getServiceResourceId(), resource);
	}
}
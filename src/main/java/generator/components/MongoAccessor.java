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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.mongojack.DBQuery;
import org.mongojack.DBUpdate;
import org.mongojack.JacksonDBCollection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoTimeoutException;

import generator.model.ServiceResource;
import model.job.JobProgress;

/**
 * Accessor for MongoDB instance to persist models. 
 * @see pz-jobmanager implementation as main reference.
 * 
 */
@Component
public class MongoAccessor {
	@Value("${vcap.services.pz-mongodb.credentials.uri}")
	private String DATABASE_URI;
	@Value("${vcap.services.pz-mongodb.credentials.database}")
	private String DATABASE_NAME;
	@Value("${mongo.db.collection.name}")
	private String JOB_COLLECTION_NAME;
	private MongoClient mongoClient;

	public MongoAccessor() {
	}

	@PostConstruct
	private void initialize() {
		try {
			mongoClient = new MongoClient(new MongoClientURI(DATABASE_URI));
		} catch (Exception exception) {
			System.out.println("Error connecting to MongoDB Instance.");
			exception.printStackTrace();
		}
	}

	@PreDestroy
	private void close() {
		mongoClient.close();
	}

	/**
	 * Gets a reference to the MongoDB Client Object.
	 * 
	 * @return
	 */
	public MongoClient getClient() {
		return mongoClient;
	}

	/**
	 * Gets a reference to the MongoDB's Job Collection.
	 * 
	 * @return
	 */
	public JacksonDBCollection<ServiceResource, String> getJobCollection() {
		DBCollection collection = mongoClient.getDB(DATABASE_NAME).getCollection(JOB_COLLECTION_NAME);
		return JacksonDBCollection.wrap(collection, ServiceResource.class, String.class);
	}

	/**
	 * Gets the total number of Jobs in the database
	 * 
	 * @return Number of jobs in the DB
	 */
	public long getCollectionCount() {
		return getJobCollection().getCount();
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
		BasicDBObject query = new BasicDBObject("serviceResourceId", serviceResourceId);
		ServiceResource serviceResource;

		try {
			if ((serviceResource = getJobCollection().findOne(query)) == null) {
				// In case the serviceResource was being updated, or it doesn't
				// exist at this point, try once more. I admit this is
				// not optimal, but it certainly covers a host of race conditions.
				Thread.sleep(100);
				serviceResource = getJobCollection().findOne(query);
			}
		} catch (MongoTimeoutException mte) {
			throw new ResourceAccessException("MongoDB instance not available.");
		}

		return serviceResource;
	}

	/**
	 * Updates the status of a Job.
	 * 
	 * @param jobId
	 *            The Job Id
	 * @param status
	 *            The Status String of the Job
	 */
	public void updateJobStatus(String jobId, String status) {
		getJobCollection().update(DBQuery.is("jobId", jobId), DBUpdate.set("status", status));
	}

	/**
	 * Updates the Progress of a Job
	 * 
	 * @param jobId
	 *            The Job Id to update
	 * @param progress
	 *            The progress to set
	 */
	public void updateJobProgress(String jobId, JobProgress progress) {
		getJobCollection().update(DBQuery.is("jobId", jobId), DBUpdate.set("progress", progress));
	}

	/**
	 * Deletes a Job entry.
	 * 
	 * @param serviceId
	 *            The Id of the job to delete
	 */
	public void removeJob(String serviceId) {
		getJobCollection().remove(DBQuery.is("serviceId", serviceId));
	}

	/**
	 * Adds a Job
	 * 
	 * @param job
	 *            The Job
	 */
	public void addServiceResource(ServiceResource resource) {
		getJobCollection().insert(resource);
	}
}
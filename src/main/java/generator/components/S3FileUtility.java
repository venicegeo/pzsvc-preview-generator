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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import exception.InvalidInputException;
import model.data.location.FileAccessFactory;
import model.data.location.FileLocation;
import util.UUIDFactory;

/**
 * File utilities for getting files from S3
 * 
 * @author Patrick.Doody
 *
 */
@Component
public class S3FileUtility {
	@Autowired
	private UUIDFactory uuidFactory;

	private AmazonS3 s3Client;
	public static final String S3_OUTPUT_BUCKET = "pz-svcs-prevgen-output";

	@Value("${s3.key.access:}")
	private String AMAZONS3_ACCESS_KEY;
	@Value("${s3.key.private:}")
	private String AMAZONS3_PRIVATE_KEY;
	@Value("${raster.temp.directory}")
	private String RASTER_LOCAL_DIRECTORY;

	/**
	 * Will Copy File from S3 to Local Dir and Return the File
	 * 
	 * @param fileLocation
	 *            Interface to get file info from.
	 * @return File file object
	 * @throws Exception
	 * @throws IOException
	 * @throws InvalidInputException
	 * @throws AmazonClientException
	 */
	public File getFileFromS3(FileLocation fileLocation, String serviceId)
			throws AmazonClientException, InvalidInputException, IOException {

		// Obtain file stream from s3
		FileAccessFactory fileFactory = new FileAccessFactory(AMAZONS3_ACCESS_KEY, AMAZONS3_PRIVATE_KEY);
		File file = new File(String.format("%s_%s%s%s", RASTER_LOCAL_DIRECTORY, serviceId, File.separator, fileLocation.getFileName()));

		InputStream inputStream = fileFactory.getFile(fileLocation);
		FileUtils.copyInputStreamToFile(inputStream, file);
		inputStream.close();

		return file;
	}

	/**
	 * Upload file to s3 bucket
	 * 
	 * @param file
	 *            the object
	 */
	public String writeFileToS3(File file, FileLocation fileLocation) throws FileNotFoundException {

		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(file.length());
		String fileKey = String.format("%s-%s", uuidFactory.getUUID(), file.getName());

		// BasicAWSCredentials credentials = new BasicAWSCredentials(AMAZONS3_ACCESS_KEY, AMAZONS3_PRIVATE_KEY);
		s3Client = new AmazonS3Client();

		// Making the object public
		PutObjectRequest putObj = new PutObjectRequest(S3_OUTPUT_BUCKET, fileKey, file);
		putObj.setCannedAcl(CannedAccessControlList.PublicRead);
		s3Client.putObject(putObj);

		return fileKey;
	}
}

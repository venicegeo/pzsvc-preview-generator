package generator.model;

/**
 * S3 store model for representing service payload.
 * 
 * @author Sonny.Saniev
 * 
 */
public class S3StoreInfo {
	public String domain;
	public String bucketName;
	public String fileName;

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getBucketName() {
		return bucketName;
	}

	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

}
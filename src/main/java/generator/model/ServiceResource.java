package generator.model;

import model.data.DataResource;
import model.status.StatusUpdate;

/**
 * mongoDB persistence model.
 * 
 * @author Sonny.Saniev
 * 
 */
public class ServiceResource {

	public String serviceResourceId;
	public StatusUpdate status;
	public DataResource result;

	public String getServiceResourceId() {
		return serviceResourceId;
	}

	public void setServiceResourceId(String serviceResourceId) {
		this.serviceResourceId = serviceResourceId;
	}

	public StatusUpdate getStatus() {
		return status;
	}

	public void setStatus(StatusUpdate status) {
		this.status = status;
	}

	public DataResource getResult() {
		return result;
	}

	public void setResult(DataResource result) {
		this.result = result;
	}
}

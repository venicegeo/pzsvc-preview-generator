package generator.components;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import generator.model.RasterCropRequest;
import util.UUIDFactory;

@Component
public class ServiceThreadManager {

	@Autowired
	private UUIDFactory uuidFactory;
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
		Future<?> thread = rasterGenerator.run(payload, id);
		

		// Keep track of running jobs
		threadMap.put(id, thread);
		
		return id;
	}

}

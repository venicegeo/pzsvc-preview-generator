package generator.model;
/*
 * 
 */
public class RasterCropRequest {
	public S3StoreInfo source;
	public String function;
	public BoundingBoxInfo bounds;
	
	public S3StoreInfo getSource() {
		return source;
	}
	public void setSource(S3StoreInfo source) {
		this.source = source;
	}
	public String getFunction() {
		return function;
	}
	public void setFunction(String function) {
		this.function = function;
	}
	public BoundingBoxInfo getBounds() {
		return bounds;
	}
	public void setBounds(BoundingBoxInfo bounds) {
		this.bounds = bounds;
	}
}

package video;

public class Frame {
	
	public static final int FRAME_WIDTH = 480, FRAME_HEIGHT = 270;
	
	private long frameIndex;
	
	private double[][] red;
	private double[][] green;
	private double[][] blue;
	
	public Frame(double[][] red, double[][] green, double[][] blue, long index) {
		this.red = red;
		this.green = green;
		this.blue = blue;
		this.frameIndex = index;
	}
	
	public long getFrameIndex() {
		return frameIndex;
	}
	
	public double[][] getRedChannel(){
		return red;
	}
	
	public double[][] getGreenChannel(){
		return green;
	}
	
	public double[][] getBlueChannel(){
		return blue;
	}
}

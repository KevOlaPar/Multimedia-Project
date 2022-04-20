package preprocess;

public class MotionVector {
	
	public int referenceX;
	public int referenceY;
	public int offsetX;
	public int offsetY;
	
	public MotionVector() {
		
	}
	
	public MotionVector(int refX, int refY, int offsetX, int offsetY) {
		this.referenceX = refX;
		this.referenceY = refY;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
	}
}

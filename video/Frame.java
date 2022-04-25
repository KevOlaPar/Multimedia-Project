package video;

import java.awt.image.BufferedImage;

/*
 * the Frame class contains RGB values for a single frame, and operations to convert RGB to YUV
 */
public class Frame {
	
	public static final int FRAME_WIDTH = 480, FRAME_HEIGHT = 270;
	
	private int frameIndex;
	
	private double[][] red;
	private double[][] green;
	private double[][] blue;
	
	public Frame(double[][] red, double[][] green, double[][] blue, int index) {
		this.red = red;
		this.green = green;
		this.blue = blue;
		this.frameIndex = index;
	}
	
	public int getFrameIndex() {
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
	
	/*
	 * Calculate Y channel (YUV) values from RGB
	 */
	public double[][] getYChannel(){
		
		double[][] Y = new double[FRAME_WIDTH][FRAME_HEIGHT];
		
		for(int row=0; row<FRAME_WIDTH; row++) {
			for(int col=0; col<FRAME_HEIGHT; col++) {
				Y[row][col] = overflowCheck(0.299 * red[row][col] + 0.587 * green[row][col] + 0.114 * blue[row][col], true);
			}
		}
		
		return Y;
	}
	
	/*
	 * Calculate U channel (YUV) values from RGB
	 */
	public double[][] getUChannel(){
		
		double[][] U = new double[FRAME_WIDTH][FRAME_HEIGHT];
		
		for(int row=0; row<FRAME_WIDTH; row++) {
			for(int col=0; col<FRAME_HEIGHT; col++) {
				U[row][col] = overflowCheck(-0.147*red[row][col] - 0.289*green[row][col] + 0.436*blue[row][col], false);
			}
		}
		
		return U;
	}
	
	/*
	 * Calculate V channel (YUV) values from RGB
	 */
	public double[][] getVChannel(){
		
		double[][] V = new double[FRAME_WIDTH][FRAME_HEIGHT];
		
		for(int row=0; row<FRAME_WIDTH; row++) {
			for(int col=0; col<FRAME_HEIGHT; col++) {
				V[row][col] = overflowCheck(0.615*red[row][col] - 0.515*green[row][col] - 0.100*blue[row][col], false);
			}
		}
		
		return V;
	}

	public BufferedImage toBufferedImage() {
		BufferedImage image = new BufferedImage(FRAME_WIDTH, FRAME_HEIGHT, BufferedImage.TYPE_INT_RGB);
		for (int i = 0; i < FRAME_WIDTH; i++) {
			for (int j = 0; j < FRAME_HEIGHT; j++) {
				int pix = 0xff000000 | (((int)red[i][j] & 0xff) << 16) | (((int)green[i][j] & 0xff) << 8) | ((int)blue[i][j] & 0xff);
				image.setRGB(i, j, pix);
			}
		}
		return image;
	}
	
	/*
	 * checks if there is an overflow in YUV channel values
	 * 
	 * @param	num			the number that needs to be checked against an overflow
	 * @param	isYChannel	boolean indicating if the number is from Y channel or UV channel
	 * @return				the checked value that is guaranteed to have no overflow
	 */
	private double overflowCheck(double num, boolean isYChannel) {
		if(isYChannel) {
			num = num < 0 ? 0 : num;
			num = num > 255? 255 : num;
		}
		else {
			num = num < -127.5 ? -127.5 : num;
			num = num > 127.5 ? 127.5 : num;
		}
		return num;
	}
}

package preprocess;

import java.util.ArrayList;
import java.util.List;

import video.Frame;
import preprocess.MotionVector;

/*
 * class for calculating motion compensation between 2 consecutive frames
 * Reference: Textbook Chapter "Media Compression: Video" 1.2 - 1.5
 */
public class MotionCompensation {
	
	public static final int MACROBLOCK_WIDTH = 15; // width == height
	public static final int SEARCH_PARAM = 15; // search parameter k
	public static final double ENTROPY_THRESHOLD = 45.0; // hard-coded threshold for deciding shot boundaries
	
	public static double[][] getMotionCompensatedErrorFrame(double[][] referenceFrame, double[][] targetFrame){
		
		List<MotionVector> motionVectorList = new ArrayList<>();
		
		//for each macroblock starting index (x, y) in target frame, search it in the reference frame
		for(int targetX = 0; targetX + MACROBLOCK_WIDTH <= Frame.FRAME_WIDTH; targetX += MACROBLOCK_WIDTH) {
			for(int targetY = 0; targetY + MACROBLOCK_WIDTH <= Frame.FRAME_HEIGHT; targetY += MACROBLOCK_WIDTH) {
				
				double minMeanAbsoluteDifference = Integer.MAX_VALUE;
				MotionVector motionVector = new MotionVector();
				
				//from -k to +k search parameter in reference frame
				for(int refX = targetX - SEARCH_PARAM; refX + MACROBLOCK_WIDTH <= targetX + SEARCH_PARAM; refX++) {
					for(int refY = targetY - SEARCH_PARAM; refY + MACROBLOCK_WIDTH <= targetY + SEARCH_PARAM; refY++) {
						
						//out of bound check
						if(refX < 0 || refX + MACROBLOCK_WIDTH > Frame.FRAME_WIDTH || refY < 0 || refY + MACROBLOCK_WIDTH > Frame.FRAME_HEIGHT) {
							continue;
						}
						
						double mad = getMeanAbsoluteDifference(referenceFrame, targetFrame, targetX, targetY, refX, refY);
						if(mad < minMeanAbsoluteDifference) {
							minMeanAbsoluteDifference = mad;
							motionVector.referenceX = refX;
							motionVector.referenceY = refY;
							motionVector.offsetX = targetX - refX;
							motionVector.offsetY = targetY - refY;
						}
					}
				}
				
				// after search of one target macroblock, save the motion vector for later use
				motionVectorList.add(motionVector);
			}
		}
		
		double[][] predictedFrame = generateCompensatedFrame(referenceFrame, motionVectorList);
		
		//do a pixel-wise subtraction between the predicted frame and the actual frame to calculate the error frame
		double[][] errorFrame = new double[Frame.FRAME_WIDTH][Frame.FRAME_HEIGHT];
		
		for(int row = 0; row < Frame.FRAME_WIDTH; row++) {
			for(int col = 0; col < Frame.FRAME_HEIGHT; col++) {
				errorFrame[row][col] = (targetFrame[row][col] - predictedFrame[row][col]);
			}
		}
		
		return errorFrame;
	}
	
	/*
	 * predict/compensate the target frame using reference frame and motion vectors
	 */
	private static double[][] generateCompensatedFrame(double[][] referenceFrame, List<MotionVector> motionVectorList){
		
		double[][] compensatedFrame = new double[Frame.FRAME_WIDTH][Frame.FRAME_HEIGHT];
		
		//for each motion vector
		for(MotionVector mv : motionVectorList) {
			//for each macroblock
			for(int x = 0; x < MACROBLOCK_WIDTH; x ++) {
				for(int y = 0; y < MACROBLOCK_WIDTH; y++) {
					compensatedFrame[mv.referenceX + mv.offsetX + x][mv.referenceY + mv.offsetY + y] = referenceFrame[mv.referenceX + x][mv.referenceY + y];
				}
			}
		}
		
		return compensatedFrame;
	}
	
	/*
	 * Calculate the MAD of a macroblock given the starting index of macroblocks in both frames
	 */
	public static double getMeanAbsoluteDifference(double[][] referenceFrame, double[][] targetFrame, int targetX, int targetY, int refX, int refY) {
		
		double mad = 0;
		
		for(int offsetX = 0; offsetX < MACROBLOCK_WIDTH; offsetX++) {
			for(int offsetY = 0; offsetY < MACROBLOCK_WIDTH; offsetY++) {
				
				mad += Math.abs(referenceFrame[refX + offsetX][refY + offsetY] - targetFrame[targetX + offsetX][targetY + offsetY]);
			}
		}
		
		mad /= MACROBLOCK_WIDTH * MACROBLOCK_WIDTH;
		
		return mad;
	}

	/*
	 * Calculate entropy of the error/difference frame,
	 * using average error per pixel
	 */
	public static double getEntropy(double[][] errorFrame){
		double sum = 0;
		for(int row = 0; row < Frame.FRAME_WIDTH; row++){
			for(int col = 0; col < Frame.FRAME_HEIGHT; col++){
				sum += Math.abs(errorFrame[row][col])/(Frame.FRAME_WIDTH * Frame.FRAME_HEIGHT);
			}
		}
		return sum;
	}
}

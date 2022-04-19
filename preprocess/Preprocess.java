package preprocess;

import java.util.ArrayList;
import java.util.List;

import video.Frame;
import video.VideoFrameReader;

/*
 * Preprocess the video.
 * Calculate motion compensation (calculate difference frames, and its entropy).
 * Divide frames into shots and scenes. 
 */
public class Preprocess {
	
	private String inputRgbFile, inputWavFile, outputRgbFile, outputWavFile;
	private int ad;
	
	public Preprocess(String inputRgb, String inputWav, String outputRgb, String outputWav) {
		inputRgbFile = inputRgb;
		inputWavFile = inputWav;
		outputRgbFile = outputRgb;
		outputWavFile = outputWav;
	}
	
	/*
	 * Find shot boundaries by calculating the entropy of difference frames
	 * 
	 * @return	List<Integer>	a list contains the starting frame index for each shot
	 */
	public List<Integer> findShotBoundaries(){
		
		List<Integer> boundaries = new ArrayList<>();
		boundaries.add(0);
		
		VideoFrameReader frameReader = new VideoFrameReader(inputRgbFile);
		Frame prev = frameReader.nextFrame();
		Frame cur = null;
		while(true) {
			cur = frameReader.nextFrame();
			if(cur == null) {
				break;
			}
			double[][] prevY = prev.getYChannel();
			double[][] curY = cur.getYChannel();
			
			//need to do frame differencing with motion compensation
			double[][] differenceFrame = getDifferenceFrame(prevY, curY);
			double entropy = getEntropy(differenceFrame);
		}
		
		
		return boundaries;
	}
	
	
	/*
	 * 
	 */
	private double[][] getDifferenceFrame(double[][] prev, double[][] cur){
		
		double[][] diff = new double[Frame.FRAME_WIDTH][Frame.FRAME_HEIGHT];
		
		for(int row=0; row<diff.length; row++) {
			for(int col=0; col<diff[0].length; col++) {
				diff[row][col] = Math.abs(prev[row][col] - cur[row][col]);
			}
		}
		
		return diff;
	}
	
	
	private double getEntropy(double[][] diff) {
		return 0;
	}

	/*
	 *  args[0] = input rgb file
	 *  args[1] = input wav file
	 *  args[2] = output rgb file
	 *  args[3] = output wav file
	 */
	public static void main(String[] args) {
		
		Preprocess processor = new Preprocess(args[0], args[1], args[2], args[3]);
		
	}

}

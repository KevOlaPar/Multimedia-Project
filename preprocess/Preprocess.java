package preprocess;

import java.util.ArrayList;
import java.util.List;

import utils.Utils;
import video.Frame;
import video.VideoFrameReader;

/*
 * Preprocess the video.
 * Calculate motion compensation (calculate difference frames, and its entropy).
 * Divide frames into shots and scenes. 
 */
public class Preprocess {
	
	private String inputRgbFile, inputWavFile, outputRgbFile, outputWavFile;
	private int adNum; // 0 for no ad, 1 for starbucks, etc...
	public List<double[][]> errorFrameList;
	
	public Preprocess(String inputRgb, String inputWav, String outputRgb, String outputWav) {
		inputRgbFile = inputRgb;
		inputWavFile = inputWav;
		outputRgbFile = outputRgb;
		outputWavFile = outputWav;
		
		errorFrameList = new ArrayList<>();
	}
	
	/*
	 * Find shot boundaries by calculating the entropy of difference frames
	 * 
	 * @return	List<Integer>	a list containing the starting frame index for each shot
	 */
	public List<Integer> findShotBoundaries(){
		
		List<Integer> boundaries = new ArrayList<>();
		boundaries.add(0);
		
		VideoFrameReader frameReader = new VideoFrameReader(inputRgbFile);
		Frame prev = frameReader.nextFrame();
		Frame cur = null;
		int count = 0;
		while(true) {
			cur = frameReader.nextFrame();
			if(cur == null || count == 900) {
				break;
			}
			double[][] prevY = prev.getYChannel();
			double[][] curY = cur.getYChannel();
			
			//do frame differencing with motion compensation
			double[][] compensatedErrorFrame = MotionCompensation.getMotionCompensatedErrorFrame(prevY, curY);
			if(count % 30 == 0)
				errorFrameList.add(compensatedErrorFrame);
			count ++;
		}
		
		
		return boundaries;
	}
	
	
	

	/*
	 *  args[0] = input rgb file
	 *  args[1] = input wav file
	 *  args[2] = output rgb file
	 *  args[3] = output wav file
	 */
	public static void main(String[] args) {
		
		Preprocess processor = new Preprocess(args[0], args[1], args[2], args[3]);
		System.out.println(args[0] + "\n" + args[1]);
		processor.findShotBoundaries();
//		for(double[][] frame : processor.errorFrameList) {
//			Utils.writeChannelToDisk("C:\\Users\\Kevin Yu\\Desktop", frame);
//		}
		Utils.displayErrorFrame(processor.errorFrameList.get(1));
		
	}

}

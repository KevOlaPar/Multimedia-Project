package preprocess;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
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

	//for testing
	public List<double[][]> errorFrameList;
	List<Double> entropyList;
	
	public Preprocess(String inputRgb, String inputWav, String outputRgb, String outputWav) {
		inputRgbFile = inputRgb;
		inputWavFile = inputWav;
		outputRgbFile = outputRgb;
		outputWavFile = outputWav;
		
		errorFrameList = new ArrayList<>();
		entropyList = new ArrayList<>();
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

		int frameIndex = 1;

		while(true) {
			cur = frameReader.nextFrame();
			if(cur == null) {
				break;
			}
			double[][] prevY = prev.getYChannel();
			double[][] curY = cur.getYChannel();
			
			//do frame differencing with motion compensation
			double[][] compensatedErrorFrame = MotionCompensation.getMotionCompensatedErrorFrame(prevY, curY);

			//calculate entropy of the error frame, if it is larger than the threshold, mark it as a shot (starting index)
			double entropy = MotionCompensation.getEntropy(compensatedErrorFrame);
			if(entropy > MotionCompensation.ENTROPY_THRESHOLD){
				boundaries.add(frameIndex);
			}
			//entropyList.add(entropy);

//			if(frameIndex % 30 == 0)
//				errorFrameList.add(compensatedErrorFrame);
			frameIndex ++;

			prev = cur;
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
		List<Integer> shotBoundaries = processor.findShotBoundaries();
//		for(double[][] frame : processor.errorFrameList) {
//			Utils.writeChannelToDisk("C:\\Users\\Kevin Yu\\Desktop", frame);
//		}
		System.out.println(shotBoundaries.toString());
//		try {
//			BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\Users\\Kevin Yu\\Desktop\\entropy_abs.txt"));
//			for(double d : processor.entropyList){
//				writer.write(String.valueOf(d));
//				writer.write("\n");
//			}
//			writer.close();
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//
//		Utils.displayErrorFrame(processor.errorFrameList.get(80));
	}

}

package preprocess;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.UnsupportedAudioFileException;

import audio.AudioFrame;
import audio.AudioFrameReader;
import utils.Utils;
import video.Frame;
import video.VideoFrameReader;

/*
 * Preprocess the video.
 * Calculate motion compensation (calculate difference frames, and its entropy).
 * Divide frames into shots and scenes. 
 */
public class Preprocess {
	public static enum Advertisement{
		NONE, STARBUCKS, SUBWAY, MCDONALDS, NFL, AMERICAN_EAGLE, HARD_ROCK_CAFE
	}
	private String inputRgbFile, inputWavFile, outputRgbFile, outputWavFile;
	private Advertisement detectedAd;
	
	public Preprocess(String inputRgb, String inputWav, String outputRgb, String outputWav) {
		inputRgbFile = inputRgb;
		inputWavFile = inputWav;
		outputRgbFile = outputRgb;
		outputWavFile = outputWav;

		detectedAd = Advertisement.NONE;
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

			frameIndex ++;
			prev = cur;
		}
		
		
		return boundaries;
	}
	
	/*
	 * Find audio level boundaries by thresholding audio data in chunks of 15 seconds
	 */
	public List<Integer> findAudioBoundaries(){
		
		List<Integer> boundaries = new ArrayList<>();
		
		try {
			
			AudioFrameReader audioReader = new AudioFrameReader(inputWavFile);
			
			List<Integer> audioLevelList = new ArrayList<>();// all audio levels in chunk size of 5 seconds of data
	        List<Integer> bufferList = new ArrayList<>();
	        
	        //read audio frames and calculate their audio levels
	        while(true){
	            AudioFrame frame = audioReader.nextFrame();
	            if(frame == null){
	                break;
	            }
	            int level = frame.getAverageAudioLevel();
	            bufferList.add(level);

	            // combine 5 seconds of audio data
	            if(bufferList.size() == 150){
	                long total = 0;
	                for(int i : bufferList){
	                    total += i * i;
	                }
	                total /= 150;
	                total = (long) Math.sqrt(total);
	                audioLevelList.add((int) total);
	                bufferList.clear();
	            }
	        }
	        audioReader.closeFile();
	        
	        //thresholding the levels list
	        //look for 3 consecutive chunks (15 seconds) of audio level that exceeds the threshold
	        for(int i = 0; i < audioLevelList.size()-2; i++) {
	        	int exceedCount = 0;
	        	for(int j = 0; j < 3; j++) {
	        		if(audioLevelList.get(i+j) > AudioFrame.AUDIO_LEVEL_THRESHOLD_UPPER) {
	        			exceedCount++;
	        		}
	        	}
	        	if(exceedCount == 3) {
	        		boundaries.add(i * 150);
	        		boundaries.add(i * 150 + 450);
	        	}
	        }
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

		List<Integer> shotBoundaries = processor.findShotBoundaries();
		List<Integer> audioBoundaries = processor.findAudioBoundaries();
		
		System.out.println("shot boundaries: " + shotBoundaries.toString());
		System.out.println("audio boundaries: " + audioBoundaries.toString());
		
		shotBoundaries.retainAll(audioBoundaries);
		System.out.println("Intersections: " + shotBoundaries.toString());

	}

}

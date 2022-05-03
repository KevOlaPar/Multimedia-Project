package preprocess;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.UnsupportedAudioFileException;

import audio.AudioFrame;
import audio.AudioFrameReader;
import media_player2.Audio;
import video.Frame;
import video.Scene;
import video.Shot;
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
	 * Find shot boundaries by calculating the entropy of difference frames in video
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
		//add the last frame index
		boundaries.add(frameIndex);
		
		return boundaries;
	}

	/*
	 * construct shots which contains audio level for each shot
	 */
	public List<Shot> getShots(List<Integer> shotBoundaries) throws UnsupportedAudioFileException, IOException {
		AudioFrameReader reader = new AudioFrameReader(inputWavFile);
		List<Shot> shots = new ArrayList<>();
		System.out.println("total frames audio = " + reader.getTotalNumberOfFrames());

		List<Integer> bufferList = new ArrayList<>();

		int boundaryIndex = 0, frameIndex = 0;
		int left = shotBoundaries.get(boundaryIndex++);
		int right = shotBoundaries.get(boundaryIndex);
		while(true){
			AudioFrame cur = reader.nextFrame();
			if(cur == null){
				shots.add(new Shot(left, right, bufferList));
				break;
			}
			if(frameIndex == right){
				shots.add(new Shot(left, right, bufferList));
				left = right;
				right = shotBoundaries.get(++boundaryIndex);
				bufferList.clear();
			}
			int level = cur.getAverageAudioLevel();
			bufferList.add(level);
			frameIndex++;
		}
		reader.closeFile();

		return shots;
	}

	/*
	 * Compute scenes given the shots
	 */
	public List<Scene> getScenesFromShots(List<Shot> shots){
		List<Scene> scenes = new ArrayList<>();
		List<Shot> buffer = new ArrayList<>();

		Shot prev = shots.get(0);
		buffer.add(prev);
		for(int i=1; i<shots.size(); i++){
			Shot cur = shots.get(i);
			if(prev.isAd() != cur.isAd()){//it is a scene boundary
				scenes.add(new Scene(buffer));
				buffer.clear();
			}
			buffer.add(cur);
			prev = cur;
		}
		scenes.add(new Scene(buffer));
		return scenes;
	}
	
	/*
	 * Find audio level boundaries by thresholding audio data in chunks of 15 seconds.
	 * Deprecated because it does not work for dataset 3.
	 */
	@Deprecated
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
	 * write output rgb file with replaced ads
	 */
	public void writeRgbFile(List<Scene> scenes) throws IOException {
		RandomAccessFile videoOutput = new RandomAccessFile(outputRgbFile, "rw");
		VideoFrameReader frameReader = new VideoFrameReader(inputRgbFile);

		Frame frame;
		byte[] frameBytes = new byte[Frame.FRAME_WIDTH * Frame.FRAME_HEIGHT * 3];
		int sceneIndex = 0;
		Scene curScene = scenes.get(sceneIndex++);
		while((frame = frameReader.nextFrame()) != null){
			int frameIndex = frame.getFrameIndex();
			//if the current frame is the start of an ad scene,
			//write the detected logo ad instead,
			//and set frame index to the end of the ad scene.
			if(frameIndex == curScene.getStartIndex() && curScene.isAd()){
				frameReader.setFrameIndex(curScene.getEndIndex());
				//TODO: write ad file bytes into the output file

				//advancing scene index
				curScene = scenes.get(sceneIndex++);
				continue;
			}
			// if has reached the end of a scene, start the next scene
			else if(frameIndex == curScene.getEndIndex()-1 && sceneIndex != scenes.size()){
				curScene = scenes.get(sceneIndex++);
			}
			//write each color channel separately to the new file
			//TODO: change some pixels to show the bounding box of the logo
			double[][] red = frame.getRedChannel();
			writeChannelToFile(videoOutput, red);
			double[][] green = frame.getGreenChannel();
			writeChannelToFile(videoOutput, green);
			double[][] blue = frame.getBlueChannel();
			writeChannelToFile(videoOutput, blue);
		}

		videoOutput.close();
		frameReader.closeFile();
	}

	private void writeChannelToFile(RandomAccessFile out, double[][] channel) throws IOException {
		byte[] bytes = new byte[Frame.FRAME_WIDTH * Frame.FRAME_HEIGHT];
		int index = 0;

		for(int y = 0; y < Frame.FRAME_HEIGHT; y++) {

			for (int x = 0; x < Frame.FRAME_WIDTH; x++) {

				byte b = (byte)channel[x][y];
				bytes[index++] = b;
			}
		}
		out.write(bytes);
	}

	/*
	 * write output wav file with replaced ads
	 */
	public void writeWavFile(List<Scene> scenes) throws IOException {
		RandomAccessFile audioOutput = new RandomAccessFile(outputWavFile, "rw");
		RandomAccessFile audioInput = new RandomAccessFile(inputWavFile, "r");

		//write header
		byte[] headerBuffer = new byte[44];
		audioInput.read(headerBuffer);
		audioOutput.write(headerBuffer);

		byte[] audioBuffer = new byte[AudioFrame.BYTES_PER_FRAME];
		int sceneIndex = 0, frameIndex = 0;
		Scene curScene = scenes.get(sceneIndex++);
		int bytesRead = 0;
		while((bytesRead = audioInput.read(audioBuffer)) > 0){
			//if the current frame is the start of an ad scene,
			//write the detected ad file instead,
			//and set frame index to the end of the ad scene.
			if(frameIndex == curScene.getStartIndex() && curScene.isAd()){
				frameIndex = curScene.getEndIndex();
				audioInput.skipBytes((curScene.getEndIndex() - curScene.getStartIndex()) * AudioFrame.BYTES_PER_FRAME);
				//TODO: write ad file bytes into the output file

				//advancing scene index
				curScene = scenes.get(sceneIndex++);
				continue;
			}
			// if has reached the end of a scene, start the next scene
			else if(frameIndex == curScene.getEndIndex()-1 && sceneIndex != scenes.size()){
				curScene = scenes.get(sceneIndex++);
			}

			audioOutput.write(audioBuffer, 0, bytesRead);
			frameIndex++;
		}
		//changing file size section of wav header
//		long fileSize = audioOutput.length();
//		audioOutput.seek(4);
//		audioOutput.write(intToByteArray((int)(fileSize-8)));
//		audioOutput.seek(40);
//		audioOutput.write(intToByteArray((int)(fileSize-44)));

		audioInput.close();
		audioOutput.close();
	}

	public static byte[] intToByteArray(int a)
	{
		byte[] ret = new byte[4];
		ret[0] = (byte) (a & 0xFF);
		ret[1] = (byte) ((a >> 8) & 0xFF);
		ret[2] = (byte) ((a >> 16) & 0xFF);
		ret[3] = (byte) ((a >> 24) & 0xFF);
		return ret;
	}

	/*
	 *  args[0] = input rgb file
	 *  args[1] = input wav file
	 *  args[2] = output rgb file
	 *  args[3] = output wav file
	 *  args[4] = entropy threshold for motion compensation
	 *  args[5] = upper audio level threshold
	 *  args[6] = lower audio level threshold
	 */
	public static void main(String[] args) throws IOException, UnsupportedAudioFileException {
		long timeStart = System.currentTimeMillis();
		Preprocess processor = new Preprocess(args[0], args[1], args[2], args[3]);
		MotionCompensation.ENTROPY_THRESHOLD = Double.parseDouble(args[4]);
		AudioFrame.AUDIO_LEVEL_THRESHOLD_UPPER = Integer.parseInt(args[5]);
		AudioFrame.AUDIO_LEVEL_THRESHOLD_LOWER = Integer.parseInt(args[6]);

		List<Integer> shotBoundaries = processor.findShotBoundaries();
		List<Shot> shots = processor.getShots(shotBoundaries);
		List<Scene> scenes = processor.getScenesFromShots(shots);
		System.out.println(scenes.toString());


		//ad detection

		//write new files
		processor.writeRgbFile(scenes);
		processor.writeWavFile(scenes);

		long timeEnd = System.currentTimeMillis();
		long min = ((timeEnd - timeStart)/1000)/60;
		long sec = ((timeEnd - timeStart)/1000)%60;
		System.out.println("time elapsed = " + min + ":" + sec);
	}

}

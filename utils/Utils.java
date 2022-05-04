package utils;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import audio.AudioFrame;
import audio.AudioFrameReader;
import preprocess.MotionCompensation;
import video.Frame;
import video.Scene;
import video.Shot;
import video.VideoFrameReader;

public class Utils {
	public static List<Integer> shotBoundaries = new ArrayList<>();
	public static List<Shot> shots = new ArrayList<>();
	public static void displayFrame(double[][] r, double[][] g, double[][] b) {
		
		BufferedImage img = new BufferedImage(Frame.FRAME_WIDTH, Frame.FRAME_HEIGHT, BufferedImage.TYPE_INT_RGB);
		
		for(int row=0; row<Frame.FRAME_WIDTH; row++) {
			for(int col=0; col<Frame.FRAME_HEIGHT; col++) {
				int pix = 0xff000000 | (((int)r[row][col] & 0xff) << 16) | (((int)g[row][col] & 0xff) << 8) | ((int)b[row][col] & 0xff);
				img.setRGB(row,col,pix);
			}
		}
		
		JFrame frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		JLabel lbIm1 = new JLabel(new ImageIcon(img));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		frame.getContentPane().add(lbIm1, c);

		frame.pack();
		frame.setVisible(true);
	}
	
	public static void displayErrorFrame(double[][] y) {
		displayFrame(y, y, y);
	}
	
	
	/*
	 * helper code to find all error frame entropies.
	 * examine the output file to find entropy threshold.
	 */
	public static void writeVideoErrorFrameEntropiesToDisk(String inputRgbFile, String outputPath) {
		
		VideoFrameReader frameReader = new VideoFrameReader(inputRgbFile);
		System.out.println("total frames video = " + frameReader.getTotalNumberOfFrames());
		Frame prev = frameReader.nextFrame();
		Frame cur = null;

		Utils.shotBoundaries.add(0);
		int frameIndex = 1;
		List<Double> entropyList = new ArrayList<>();
		
		while(true) {
			if(frameIndex%100 == 0)
				System.out.println("processing video frame #" + frameIndex);
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
			if(entropy > MotionCompensation.ENTROPY_THRESHOLD){//change this value according to observed entropies
				Utils.shotBoundaries.add(frameIndex);
			}
			entropyList.add(entropy);

			frameIndex ++;

			prev = cur;
		}

		//add the last frame
		Utils.shotBoundaries.add(frameIndex);
//		List<Integer> copy = new ArrayList<>(shotBoundaries);
//		for(int i=0; i<copy.size()-1; i++){
//			int prevB = copy.get(i);
//			int curB = copy.get(i+1);
//			if(curB -  prevB < 10){
//				shotBoundaries.removeIf( v -> v == prevB);
//			}
//		}
		System.out.println(shotBoundaries.toString());
		//write entropies to file
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));
			double average = 0;
			
			for(int i=0; i<entropyList.size(); i++) {
				double entropy = entropyList.get(i);
				average += entropy;
				writer.write(String.valueOf(i+1) + "\t" + Math.round(entropy * 100.0)/100.0 + "\n");
			}
			System.out.println(average/entropyList.size());
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * helper code to find frame by frame audio level difference.
	 * examine the output file to find threshold.
	 */
	public static void writeAudioLevelsToDisk(String inputWavFile, String outputPath) throws UnsupportedAudioFileException, IOException {
		
		AudioFrameReader reader = new AudioFrameReader(inputWavFile);

        System.out.println("total frames audio = " + reader.getTotalNumberOfFrames());

        List<Integer> audioLevelList = new ArrayList<>();
//		AudioFrame prev = reader.nextFrame();
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
				++boundaryIndex;
				if(boundaryIndex == shotBoundaries.size()){
					break;
				}
				right = shotBoundaries.get(boundaryIndex);
				bufferList.clear();
			}
			int level = cur.getAverageAudioLevel();
			audioLevelList.add(level);
			bufferList.add(level);
			frameIndex++;
        }
        reader.closeFile();

		System.out.println(shots.toString());
		//scene processing
		List<Scene> scenes = getScenes(shots);
		System.out.println(scenes.toString());

        BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));
		for(int i=0; i<audioLevelList.size(); i++){
            int level = audioLevelList.get(i);
            writer.write(String.valueOf(i)+ "\t" + String.valueOf(level) + "\n");
		}
		writer.close();
	}

	public static List<Scene> getScenes(List<Shot> shots){
		List<Scene> scenes = new ArrayList<>();
		List<Shot> buffer = new ArrayList<>();

		Shot prev = shots.get(0);
		buffer.add(prev);
		for(int i=1; i<shots.size(); i++){
			Shot cur = shots.get(i);
			if(getPercentChange(prev, cur) > 89){//it is a scene boundary
				Scene scene = new Scene(buffer);
				if(scene.length() < 300 || scene.length() > 600){
					scene.setIsAd(false);
				}
				scenes.add(scene);
				buffer.clear();
			}
			buffer.add(cur);
			prev = cur;
		}
		Scene scene = new Scene(buffer);
		if(scene.length() < 300 || scene.length() > 600){
			scene.setIsAd(false);
		}
		scenes.add(scene);
		return scenes;
	}

	public static double getPercentChange(Shot prev, Shot cur){
		double level1 = (double)prev.getAudioLevel();
		double level2 = (double)cur.getAudioLevel();
		double denom = level1 > level2 ? level2 : level1;
		return (Math.abs(level2 - level1)/denom)*100;
	}
	/*
	 * run this class before preprocessing to find where the thresholds should be 
	 */
	public static void main(String[] args) {
		MotionCompensation.ENTROPY_THRESHOLD = 35;
		AudioFrame.AUDIO_LEVEL_THRESHOLD_UPPER = 4000;
		AudioFrame.AUDIO_LEVEL_THRESHOLD_LOWER = 3000;
		String inputRgb = args[0], inputWav = args[1], outputRgbTxt = args[2], outputWavTxt = args[3];
		Utils.writeVideoErrorFrameEntropiesToDisk(inputRgb, outputRgbTxt);
		try {
			Utils.writeAudioLevelsToDisk(inputWav, outputWavTxt);
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

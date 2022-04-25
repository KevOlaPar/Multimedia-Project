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
import video.VideoFrameReader;

public class Utils {
	
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
		
		Frame prev = frameReader.nextFrame();
		Frame cur = null;

		int frameIndex = 1;
		List<Double> entropyList = new ArrayList<>();
		
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
			
			entropyList.add(entropy);

			frameIndex ++;

			prev = cur;
		}
		
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
	 * helper code to find audio levels in chunks of 5 seconds.
	 * examine the output file to find threshold.
	 */
	public static void writeAudioLevelsToDisk(String inputWavFile, String outputPath) throws UnsupportedAudioFileException, IOException {
		
		AudioFrameReader reader = new AudioFrameReader(inputWavFile);

        System.out.println("file length = " + reader.getFileLength());
        System.out.println("total frames = " + reader.getTotalNumberOfFrames());

        List<Integer> audioLevelList = new ArrayList<>();
        List<Integer> bufferList = new ArrayList<>();
        int  index = 0;
        while(true){
            AudioFrame frame = reader.nextFrame();
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
            index++;
        }
        reader.closeFile();
        
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));
		for(int i=0; i<audioLevelList.size(); i++){
            int level = audioLevelList.get(i);
            writer.write(String.valueOf(i * 150)+"-"+String.valueOf(i*150 + 149) + "\t" + String.valueOf(level) + "\n");
		}
		writer.close();
	}
	
	/*
	 * run this class before preprocessing to find where the thresholds should be 
	 */
	public static void main(String[] args) {
		//Utils.writeVideoErrorFrameEntropiesToDisk("C:\\Users\\Kevin Yu\\Downloads\\dataset-001\\dataset\\Videos\\data_test1.rgb", "C:\\Users\\Kevin Yu\\Desktop\\EntropyList.txt");
		try {
			Utils.writeAudioLevelsToDisk("C:\\Users\\Kevin Yu\\Downloads\\dataset-002\\dataset2\\Videos\\data_test2.wav", "C:\\Users\\Kevin Yu\\Desktop\\audio_levels.txt");
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

package utils;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import audio.AudioFrame;

import javax.sound.sampled.DataLine.Info;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import video.Frame;
import video.VideoFrameReader;

public class SimpleMediaPlayer {
	
	private AudioInputStream audioStream;
	private VideoFrameReader videoReader;
	private BufferedImage img;
	
	public SimpleMediaPlayer(String videoFile, String audioFile) {
		
		videoReader = new VideoFrameReader(videoFile);
		img = new BufferedImage(Frame.FRAME_WIDTH, Frame.FRAME_HEIGHT, BufferedImage.TYPE_INT_RGB);
		
		try {
			
			InputStream bufferedIn = new BufferedInputStream(new FileInputStream(audioFile));
			audioStream = AudioSystem.getAudioInputStream(bufferedIn);
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void play() {
		
		AudioFormat audioFormat = audioStream.getFormat();
		Info info = new Info(SourceDataLine.class, audioFormat);
		
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
		
		// opens the audio channel
		SourceDataLine dataLine = null;
		try {
		    dataLine = (SourceDataLine) AudioSystem.getLine(info);
		    dataLine.open(audioFormat, AudioFrame.BYTES_PER_FRAME);
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}

		// Starts the music :P
		dataLine.start();

		int readBytes = 0;
		byte[] audioBuffer = new byte[AudioFrame.BYTES_PER_FRAME]; // 3200 bytes of audio corresponds to 1 video frame
		int seconds = 0, count = 0;;
		
		try {
		    while (readBytes != -1) {
		    	readBytes = audioStream.read(audioBuffer, 0, audioBuffer.length);
				if (readBytes >= 0){
				    dataLine.write(audioBuffer, 0, readBytes);
				    Frame videoFrame = videoReader.nextFrame();
				    //if no more frames to read, break
				    if(videoFrame == null) {
				    	break;
				    }
				    
				    img = videoFrame.toBufferedImage();
				    lbIm1.setIcon(new ImageIcon(img));
				    
				    count++;
				    if(count == 30) {//print seconds after every 30 frame
				    	count = 0;
				    	seconds++;
				    	System.out.println(seconds);
				    }
				}
		    }
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
		    // plays what's left and and closes the audioChannel
		    dataLine.drain();
		    dataLine.close();
		    videoReader.closeFile();
		}
	}
	
	public static void main(String[] args) {
		String video = args[0], audio = args[1];
		SimpleMediaPlayer player = new SimpleMediaPlayer(video, audio);
		player.play();
	}

}

package utils;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.*;

import video.Frame;
import video.VideoFrameReader;

public class SimpleMediaPlayer {
	
	private AudioInputStream audioStream;
	private VideoFrameReader videoReader;
	private JLabel videoLabel;
	private JButton playButton;
	private JLabel timeLabel;
	private BufferedImage img;
	private boolean isPlaying;
	private int timeInSeconds = 0;
	private int frameCount; //counts how many frames has passed. resets every 30 frames, to track time.
	private SourceDataLine dataLine;
	
	public SimpleMediaPlayer(String videoFile, String audioFile) {
		
		videoReader = new VideoFrameReader(videoFile);
		img = new BufferedImage(Frame.FRAME_WIDTH, Frame.FRAME_HEIGHT, BufferedImage.TYPE_INT_RGB);
		this.isPlaying = false;
		
		try {
			
			InputStream bufferedIn = new BufferedInputStream(new FileInputStream(audioFile));
			audioStream = AudioSystem.getAudioInputStream(bufferedIn);

			AudioFormat audioFormat = audioStream.getFormat();
			Info info = new Info(SourceDataLine.class, audioFormat);

			// opens the audio channel
			dataLine = null;
			dataLine = (SourceDataLine) AudioSystem.getLine(info);
			dataLine.open(audioFormat, AudioFrame.BYTES_PER_FRAME);

			dataLine.start();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
		initializeUI();
	}

	/*
	 * GUI client of the media player
	 */
	public void initializeUI(){

		JFrame frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		videoLabel = new JLabel(new ImageIcon(img));
		timeLabel = new JLabel("0:00");
		playButton = new JButton("Play");
		playButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(isPlaying){
					isPlaying = !isPlaying;
					playButton.setText("Resume");
				}
				else {
					isPlaying = !isPlaying;
					playButton.setText("Pause");
					/*
					 * play/resume in a new thread so that it does not block my GUI.
					 */
					new Thread(() -> {
						play();
					}).start();
				}
			}
		});

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridwidth = 3;
		c.gridx = 0;
		c.gridy = 0;
		frame.getContentPane().add(videoLabel, c);

		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.LAST_LINE_START;
		c.weightx = 0.5;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 1;
		frame.getContentPane().add(playButton, c);

		c.fill = GridBagConstraints.NONE;
		c.weightx = 0.5;
		c.gridwidth = 2;
		c.gridx = 1;
		c.gridy = 1;
		frame.getContentPane().add(timeLabel, c);


		frame.pack();
		frame.setVisible(true);
	}

	/*
	 * call play() every time you want to start/resume the video/audio play
	 */
	public void play() {

		int readBytes = 0;
		byte[] audioBuffer = new byte[AudioFrame.BYTES_PER_FRAME]; // 3200 bytes of audio corresponds to 1 video frame
		frameCount = 0;
		
		try {
		    while (readBytes != -1 && isPlaying) {
		    	readBytes = audioStream.read(audioBuffer, 0, audioBuffer.length);
				if (readBytes >= 0){
				    dataLine.write(audioBuffer, 0, readBytes);
				    Frame videoFrame = videoReader.nextFrame();
				    //if no more frames to read, break
				    if(videoFrame == null) {
				    	break;
				    }
//					System.out.println("im playing");
				    img = videoFrame.toBufferedImage();
				    videoLabel.setIcon(new ImageIcon(img));

					//update/display time every 30 frame/1 second
				    frameCount++;
				    if(frameCount == 30) {
				    	frameCount = 0;
				    	timeInSeconds++;
						timeLabel.setText(secondsToTimeString(timeInSeconds));
				    }
				}
		    }
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
		    // plays what's left and and closes the audioChannel
		    dataLine.drain();
		}
	}

	//calculate current time in mins + seconds from total seconds
	private String secondsToTimeString(int seconds){
		int min = seconds/60;
		seconds = seconds%60;
		StringBuilder string = new StringBuilder();
		string.append(min);
		string.append(":");
		if(seconds < 10){
			string.append('0');
		}
		string.append(seconds);
		return string.toString();
	}
	public static void main(String[] args) {
		String video = args[0], audio = args[1];
		SimpleMediaPlayer player = new SimpleMediaPlayer(video, audio);
	}

}

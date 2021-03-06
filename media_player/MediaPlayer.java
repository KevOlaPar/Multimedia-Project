package media_player;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import video.VideoFrameReader;

public class MediaPlayer {
  static final int FRAME_WIDTH = 480;
  static final int FRAME_HEIGHT = 270;
  static JLabel label = new JLabel();
  static JFrame frame = new JFrame();
  static JLabel timeLabel = new JLabel();
  int seconds = 0;
  int videoFrameCount; 
  boolean isPlaying;
  VideoFrameReader frameReader;
  Audio audioReader;
  String audioFile;
  String videoFile;
  JButton button;
  BufferedImage image = new BufferedImage(FRAME_WIDTH, FRAME_HEIGHT, BufferedImage.TYPE_3BYTE_BGR);


  public MediaPlayer(String videoFile, String audioFile)
      throws UnsupportedAudioFileException, IOException, LineUnavailableException {
    this.videoFile = videoFile;
    this.audioFile = audioFile;
    initialize();
  }

  private void initialize() throws UnsupportedAudioFileException, IOException, LineUnavailableException {
    this.frameReader = new VideoFrameReader(videoFile);
    this.audioReader = new Audio(audioFile);
    isPlaying = false;
    button = new JButton("play");
    button.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (isPlaying) {
          button.setText("play");
          isPlaying = false;
        } else {
          button.setText("pause");
          new Thread(() -> {
            try {
              play();
            } catch (IOException e1) {
              e1.printStackTrace();
            } catch (UnsupportedAudioFileException ex) {
              throw new RuntimeException(ex);
            } catch (LineUnavailableException ex) {
              throw new RuntimeException(ex);
            }
          }).start();
        }
      }
    });
    showImage();
  }


  public void play() throws IOException, UnsupportedAudioFileException, LineUnavailableException {
    isPlaying = true;
    int result = 0;
    String time;
    while (isPlaying) {
      result = audioReader.writeFrame();
      if (result != -1) {
        image = frameReader.nextFrame().toBufferedImage();
        videoFrameCount++;
        label.setIcon(new ImageIcon(image));
        time = calculateTime();
        timeLabel.setText(time);
      }
      else{
        isPlaying = false;
      }
    }
  }

  private String calculateTime() {
    if(videoFrameCount == 30) {
      videoFrameCount = 0;
      seconds++;
    }
    return convertSeconds(seconds);
  }

  private String convertSeconds(int seconds) {
    StringBuilder sb = new StringBuilder();
    int mins = seconds / 60;
    int sec = seconds % 60;
    sb.append(mins);
    sb.append(":");
    if (sec < 10) {
      sb.append("0");
    }
    sb.append(sec);
    return sb.toString();
  }

  public void pause() throws UnsupportedAudioFileException, IOException, LineUnavailableException {
    isPlaying = false;
  }

  private void showImage() {
    JPanel panel = new JPanel();
    panel.add(button);
    GridBagLayout gLayout = new GridBagLayout();
    frame.getContentPane().setLayout(gLayout);
    label = new JLabel(new ImageIcon(image));
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.BOTH;
    c.anchor = GridBagConstraints.CENTER;
    c.weightx = 0.5;
    c.gridwidth = 3;
    c.gridx = 0;
    c.gridy = 0;
    frame.getContentPane().add(label, c);
    c.fill = GridBagConstraints.CENTER;
    c.weightx = 0.5;
    c.gridwidth = 1;
    c.gridx = 0;
    c.gridy = 1;
    frame.getContentPane().add(panel, c);
    c.fill = GridBagConstraints.CENTER;
    c.weightx = 0.5;
    c.gridx = 1;
    c.gridy = 1;
    c.gridwidth = 2;
    frame.getContentPane().add(timeLabel, c);
    frame.pack();
    frame.setVisible(true);
  }

  public static void main(String[] args) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
    String videoFile = args[0];
    String audioFile = args[1];
    new MediaPlayer(videoFile, audioFile);
  }

}

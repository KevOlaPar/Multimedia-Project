package media_player2;

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
          try {
            pause();
          } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e1) {
            e1.printStackTrace();
          }
        } else {
          button.setText("pause");
          new Thread(() -> {
            try {
              play();
            } catch (IOException e1) {
              e1.printStackTrace();
            }
          }).start();
        }
      }
    });
    showImage();
  }


  public void play() throws IOException {
    isPlaying = true;
    audioReader.play();
    int result = 0;
    while (isPlaying) {
      result = audioReader.writeFrame();
      if (result != -1) {
        image = frameReader.nextFrame().toBufferedImage();
        label.setIcon(new ImageIcon(image));
      }
    }
  }


  public void pause() throws UnsupportedAudioFileException, IOException, LineUnavailableException {
    isPlaying = false;
    audioReader.pause();
  }

  private void showImage() {
    JPanel panel = new JPanel();
    panel.add(button);
    GridBagLayout gLayout = new GridBagLayout();
    frame.getContentPane().setLayout(gLayout);
    label = new JLabel(new ImageIcon(image));
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.HORIZONTAL;
    c.anchor = GridBagConstraints.CENTER;
    c.ipadx = 0;
    c.ipady = 0;
    c.weightx = 0;
    c.gridx = 0;
    c.gridy = 0;
    frame.getContentPane().add(label, c);
    c.fill = GridBagConstraints.HORIZONTAL;
    c.anchor = GridBagConstraints.CENTER;
    c.weightx = 0.5;
    c.gridx = 0;
    c.gridy = 1;
    frame.getContentPane().add(panel, c);
    frame.pack();
    frame.setVisible(true);
  }

  public static void main(String[] args) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
    String videoFile = "/Users/biolajohnson/Documents/Documents - Abiola’s MacBook Pro/assignments/576/project/dataset/Videos/data_test1.rgb";
    String audioFile = "/Users/biolajohnson/Documents/Documents - Abiola’s MacBook Pro/assignments/576/project/dataset/Videos/data_test1.wav";
    MediaPlayer media = new MediaPlayer(videoFile, audioFile);
    media.play();
  }

}

package media_player;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.lang.Thread;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JSlider;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Button;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MediaPlayer implements ActionListener {
  SoundReader audio;
  VideoReader video;
  JFrame frame;
  JLabel label;
  JSlider slider;
  JPanel panel;
  String globalStatus;
  int currentVideoFrame;
  Thread videoThread;
  Thread audioThread;
  Button button;
  String videoFile;
  String audioFile;
  BufferedImage image = new BufferedImage(480, 270, BufferedImage.TYPE_INT_RGB);

  public MediaPlayer(String videoFile, String audioFile)
      throws InterruptedException, UnsupportedAudioFileException, IOException, LineUnavailableException {
    this.label = new JLabel();
    globalStatus = "start";
    currentVideoFrame = 0;
    this.slider = new JSlider(0, 100);
    slider.setValue(0);
    button = new Button("start");
    button.setActionCommand("start");
    buttonConfigurations();
    this.videoFile = videoFile;
    this.audioFile = audioFile;
    showMedia();
  }

  public void initialize() throws UnsupportedAudioFileException, IOException, LineUnavailableException {
    this.audio = new SoundReader(audioFile);
    this.video = new VideoReader(this.audio, label, videoFile);
    this.audioThread = new Thread(this.audio);
    this.videoThread = new Thread(this.video);
  }

  public void pause() {
    audio.pause();
    button.setActionCommand("pause");
    button.setLabel("play");
  }

  public void resume()
      throws InterruptedException, IOException, UnsupportedAudioFileException, LineUnavailableException {
    audio.resumeAudio();
    button.setActionCommand("play");
    button.setLabel("pause");
  }

  private void moveSlider() {
    double total = audio.getTotalFrame() / 1.0;
    long curr = audio.getCurrentFrame();
    double percent = (curr / total) * 100;
    slider.setValue((int) percent);
  }

  public void play() throws InterruptedException {
    videoThread.start();
    audioThread.start();
    button.setActionCommand("play");
    button.setLabel("pause");
  }

  public void buttonConfigurations() {
    button.addActionListener(this);
  }

  private void showMedia() {
    JPanel panelButton = new JPanel();
    panelButton.add(button);
    frame = new JFrame();
    panel = new JPanel();
    panel.add(slider);
    GridBagLayout gLayout = new GridBagLayout();
    frame.getContentPane().setLayout(gLayout);
    label = new JLabel(new ImageIcon(image));
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.HORIZONTAL;
    c.weightx = 0.5;
    c.gridx = 1;
    c.gridy = 1;
    frame.getContentPane().add(panel, c);
    c.fill = GridBagConstraints.HORIZONTAL;
    c.weightx = 0.5;
    c.gridx = 0;
    c.gridy = 1;
    frame.getContentPane().add(panelButton, c);
    c.fill = GridBagConstraints.HORIZONTAL;
    c.ipadx = 0;
    c.ipady = 0;
    c.weightx = 0;
    c.gridwidth = 2;
    c.gridx = 0;
    c.gridy = 0;
    frame.getContentPane().add(label, c);
    frame.pack();
    frame.setVisible(true);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    System.out.println(e);
    if (e.getActionCommand().equals("play")) {
      pause();
    } else if (e.getActionCommand().equals("pause")) {
      try {
        resume();
      } catch (InterruptedException | IOException | UnsupportedAudioFileException | LineUnavailableException e1) {
        e1.printStackTrace();
      }
    } else if (e.getActionCommand().equals("start")) {
      try {
        initialize();
        play();
      } catch (InterruptedException | UnsupportedAudioFileException | IOException | LineUnavailableException e1) {
        e1.printStackTrace();
      }
      globalStatus = "play";
    }
  }

  public static void main(String[] args)
      throws InterruptedException, UnsupportedAudioFileException, IOException, LineUnavailableException {
    new MediaPlayer("data/data_test1.rgb", "data/data_test1.wav");
  }

  public class MyThread extends Thread {
    public void run() {
      int old = -1;
      while (true) {
        int videoFrame = audio.getFrame() / 1600;
        System.out.println(videoFrame);
        moveSlider();
        try {
          if (old < videoFrame) {
            video.readImageRGB(videoFrame);
            old = videoFrame;
          }
        } catch (InterruptedException | IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

}

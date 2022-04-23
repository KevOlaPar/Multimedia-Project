package media_player;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.lang.Thread;

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
  Thread thread1;
  Thread thread2;
  Thread threadGen;
  Button button;

  public MediaPlayer(String videoFile, String audioFile)
      throws InterruptedException, UnsupportedAudioFileException, IOException, LineUnavailableException {
    this.video = new VideoReader(videoFile, 30, 10, 480, 270);
    //this.audio = new SoundReader(audioFile);
    this.thread1 = new Thread(this.video);
  //  this.thread2 = new Thread(this.audio);
    this.threadGen = new Thread();
    globalStatus = "start";
    currentVideoFrame = 0;
    this.slider = new JSlider(0, 100);
    slider.setValue(0);
    button = new Button("start");
    button.setActionCommand("start");
    buttonConfigurations();
    showMedia();
  }


  public void controlPlayer() throws InterruptedException {
    switch (globalStatus) {
      case "play":
      Thread.yield();
        play();
      case "pause":
        pauseVideo();
      case "resume":
        resumeVideo();
      case "start":
        initialize();
    }
  }

  public void initialize() throws InterruptedException {
    video.readImageRGB(0);
    label.setIcon(new ImageIcon(video.getImage()));
  }


  public void pauseVideo() {
    currentVideoFrame = video.getCurrentFrame();
    globalStatus = "pause";
    button.setActionCommand("play");
  }
  
  public void resumeVideo() throws InterruptedException {
    play();
  }

  private void moveSlider() {
    double total = video.getTotalFrameSize() / 1.0;
    int curr = video.getCurrentFrame();
    double percent = (curr / total) * 100;
    slider.setValue((int) percent);
  }

  public void play() throws InterruptedException {
    globalStatus = "play";
    for (int i = currentVideoFrame; i < video.getTotalFrameSize(); i++) {
      video.readImageRGB(i);
      label.setIcon(new ImageIcon(video.getImage()));
      moveSlider();
      Thread.sleep(33);
      if (globalStatus.equals("pause")) {
        break;
      }
    }
  }

public void buttonConfigurations(){
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
    label = new JLabel(new ImageIcon(video.image));
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



  public static void main(String[] args) throws InterruptedException, UnsupportedAudioFileException, IOException, LineUnavailableException {
    // new MediaPlayer(args[0], args[1]);
    MediaPlayer player = new MediaPlayer("data/data_test1.rgb", "data/data_test1.wav");
    player.controlPlayer();

  }
  @Override
    public void actionPerformed(ActionEvent e) {
      System.out.println(e);
      if (e.getActionCommand().equals("play")) {
        pauseVideo();
        button.setActionCommand("pause");
        button.setLabel("pause");
      }else if(e.getActionCommand().equals("pause")){
        try {
          resumeVideo();
          button.setActionCommand("play");
          button.setLabel("play");
        } catch (InterruptedException e1) {
          e1.printStackTrace();
        }
      } else if (e.getActionCommand().equals("start")) {
        try {
          play();
          button.setActionCommand("play");
          button.setLabel("play");
        } catch (InterruptedException e1) {
          e1.printStackTrace();
        }
        globalStatus = "play";
      }
    }
}


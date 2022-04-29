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
import java.io.IOException;




public class MediaPlayer {
  static final int FRAME_WIDTH = 480;
  static final int FRAME_HEIGHT = 270;
  static JLabel label = new JLabel();
  static JFrame frame = new JFrame();
  String status;
  Frame frameReader;
  Audio audioReader;
  String audioFile;
  String videoFile;
  JButton button;
  
  public MediaPlayer(String videoFile, String audioFile) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
    this.videoFile = videoFile;
    this.audioFile = audioFile;
    initialize();
  }

  private void initialize() throws UnsupportedAudioFileException, IOException, LineUnavailableException {
    this.frameReader = new Frame(videoFile);
    this.audioReader = new Audio(audioFile);
    status = "pause";
    button = new JButton("play");
    button.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (status.equals("play")) {
          button.setText("pause");
          status = "pause";
          try {
            audioReader.pause();
          } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e1) {
            e1.printStackTrace();
          }
        } else if (status.equals("pause")) {
          button.setText("play");
          status = "play";
          try {
            play();
          } catch (IOException e1) {
            e1.printStackTrace();
          }
        }
      }
    });
    showImage();
  }
  
  public void play() throws IOException {
    int result;
    audioReader.play();
    while (status.equals("play")) {
      result = audioReader.writeFrame();
      if (result < 0) {
        frameReader.close();
        break;
      }
      frameReader.writeFrame();
      label.setIcon(new ImageIcon(frameReader.getFrame()));
    }
  }


  private void showImage() {
    JPanel panel = new JPanel();
    panel.add(button);
    GridBagLayout gLayout = new GridBagLayout();
    frame.getContentPane().setLayout(gLayout);
    label = new JLabel(new ImageIcon(frameReader.getFrame()));
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
    String videoFile = "/Users/biolajohnson/Documents/assignments/576/media player/data/data_test1.rgb";
    String audioFile = "/Users/biolajohnson/Documents/assignments/576/media player/data/data_test1.wav";
    MediaPlayer mediaPlayer = new MediaPlayer(videoFile, audioFile);
    mediaPlayer.play();
  }

}

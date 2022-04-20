import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.lang.Thread;

public class MediaPlayer {
  Sound audio;
  Video video;

  public MediaPlayer(String videoFile, String audioFile)
      throws InterruptedException, UnsupportedAudioFileException, IOException, LineUnavailableException {
    this.video = new Video(videoFile, 30, 1 * 60, 480, 270);
    this.audio = new Sound(audioFile);
  }

  public void play() {
    Thread threadOne = new Thread(this.video);
    Thread threadTwo = new Thread(this.audio);
    threadOne.start();
    threadTwo.start();
  }

  public static void main(String[] args) throws InterruptedException, UnsupportedAudioFileException, IOException, LineUnavailableException {
    MediaPlayer player = new MediaPlayer(args[0], args[1]);
    player.play();
  }
}

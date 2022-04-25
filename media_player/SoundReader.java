package media_player;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class SoundReader implements Runnable {
  String filePath;
  Clip clip;
  String status;
  AudioInputStream audioInputStream;
  long currentFrame;

  public SoundReader(String filePath) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
    this.filePath = filePath;
    audioInputStream = AudioSystem.getAudioInputStream(new File(filePath).getAbsoluteFile());
    clip = AudioSystem.getClip();
    clip.open(audioInputStream);
  }

  public boolean isPlaying() {
    return clip.isRunning();
  }

  public void run() {
    play();
  }

  public void play() {
    clip.start();
    status = "play";
  }

  public int getTotalFrame() {
    return clip.getFrameLength();
  }

  public int getCurrentFrame() {
    return clip.getFramePosition();
  }

  public void pause() {
    if (status.equals("paused")){
      System.out.println("Already paused...");
      return;
    }
    status = "paused";
    this.currentFrame = this.clip.getMicrosecondPosition();
    clip.stop();
  }

  public void resumeAudio() throws IOException, UnsupportedAudioFileException, LineUnavailableException {
    if (status.equals("play")) {
      System.out.println("Already playing...");
      return;
    }
    clip.close();
    resetAudioStream();
    clip.setMicrosecondPosition(currentFrame);
    this.play();
  }

  public int getFrame() {
    System.out.println("get frame: " + clip.getFramePosition());
    return getCurrentFrame();
  }

  public void restart() throws IOException, UnsupportedAudioFileException, LineUnavailableException {
    clip.stop();
    clip.close();
    resetAudioStream();
    currentFrame = 0L;
    clip.setMicrosecondPosition(0);
    this.play();
  }

  public void stop() {
    clip.stop();
    clip.close();
    status = "stop";
  }

  public void jumpTo(long where) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
    if (where > 0 && where < clip.getMicrosecondLength()) {
      clip.close();
      currentFrame = where;
      resetAudioStream();
      clip.setMicrosecondPosition(currentFrame);
      this.play();
    }
  }

  private void resetAudioStream() throws IOException, UnsupportedAudioFileException, LineUnavailableException {
    audioInputStream = AudioSystem.getAudioInputStream(new File(this.filePath).getAbsoluteFile());
    clip.open(audioInputStream);
  }

  public void goToChoice(int choice) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
    switch (choice) {
      case 1:
        pause();
        break;
      case 2:
       resumeAudio();
        break;
      case 3:
        restart();;
        break;
      case 4:
        stop();
        break;
      case 5:
        System.out.println("Enter time: " + 0 + clip.getMicrosecondLength());
        Scanner scanner = new Scanner(System.in);
        long where = scanner.nextLong();
        jumpTo(where);
    }
  }
}

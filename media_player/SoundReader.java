package media_player;
import audio.AudioFrameReader;
import javax.sound.sampled.*;
import java.io.IOException;


interface SoundDelegate {
  int audioFrameChanged(long frameno);
}

public class SoundReader implements Runnable, TrackerDelegate {
  String filePath;
  Clip clip;
  String status;
  AudioFrameReader audioFrameReader;
  SoundDelegate delegate;
  Tracker tracker;

  public SoundReader(SoundDelegate delegate, String filePath) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
    this.filePath = filePath;
    this.delegate = delegate;
    this.audioFrameReader = new AudioFrameReader(filePath);
    clip = AudioSystem.getClip();
    clip.open(audioFrameReader.getAudioStream());
    tracker = new Tracker(this);
  }

  public boolean isPlaying() {
    return clip.isRunning();
  }

  public void run() {
    play();
  }

  public void play() {
    tracker.start();
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
    clip.stop();
  }

  public void resumeAudio() throws IOException, UnsupportedAudioFileException, LineUnavailableException {
    if (status.equals("play")) {
      System.out.println("Already playing...");
      return;
    }
    clip.start();
  }

  public int getFrame() {
    System.out.println("get frame: " + clip.getFramePosition());
    return getCurrentFrame();
  }

  public void restart() throws IOException, UnsupportedAudioFileException, LineUnavailableException {
    jumpTo(0);
  }

  public void stop() {
    clip.stop();
    status = "stop";
    clip.close();
    tracker.close();
  }

  public void jumpTo(long where) {
    if (where >= 0 && where < clip.getMicrosecondLength()) {
      clip.stop();
      clip.setMicrosecondPosition(where);
      clip.start();
    }
  }

  @Override
  public void track() {
    delegate.audioFrameChanged(clip.getFramePosition());
  }
}

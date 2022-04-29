package media_player2;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.DataLine.Info;

public class Audio {
  String file;
  AudioInputStream audioInputStream;
  InputStream inputStream;
  final int BUFFER_SIZE = 3200;
  SourceDataLine dataLine = null;
  byte[] buffer = new byte[BUFFER_SIZE];
  int where;

  public Audio(String file) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
    this.file = file;
    this.inputStream = new BufferedInputStream(new FileInputStream(file));
    initialize();
  }

  public void initialize() throws UnsupportedAudioFileException, IOException, LineUnavailableException {
    InputStream bufferedIn = new BufferedInputStream(inputStream);
    audioInputStream = AudioSystem.getAudioInputStream(bufferedIn);
    AudioFormat audioFormat = audioInputStream.getFormat();
    Info info = new Info(SourceDataLine.class, audioFormat);
    this.dataLine = (SourceDataLine) AudioSystem.getLine(info);
    this.dataLine.open(audioFormat, BUFFER_SIZE);
  }


  public void pause() throws UnsupportedAudioFileException, IOException, LineUnavailableException {
    dataLine.stop();
  }

  public int writeFrame() throws IOException {
    int read = inputStream.read(buffer, 0, buffer.length);
    if (read != -1) {
      dataLine.write(buffer, 0, read);
    } else {
        System.out.println("Audio is done");
        dataLine.drain();
        dataLine.close();
        return -1;
      }
      return 0;
    }

    public void play() throws IOException {
    this.dataLine.start();
  }
  
}

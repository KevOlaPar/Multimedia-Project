package media_player2;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;


public class Frame {
  final int HEIGHT = 270;
  final int WIDTH = 480;
  BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
  BufferedInputStream inputStream;  

  public Frame(String file) throws IOException {
    this.inputStream = new BufferedInputStream(new FileInputStream(file));
    initialize();
  }

  public void initialize() throws IOException{
    writeFrame();
  }

  public void writeFrame() throws IOException {
    int checkMark = 0;
    byte[] frame = new byte[480 * 270 * 3];
    int read = inputStream.read(frame);
    if (read == -1) {
      close();
    } else {
      for (int height = 0; height < 270; height++) {
        for (int width = 0; width < 480; width++) {
          byte r = frame[checkMark];
          byte g = frame[checkMark + 480 * 270];
          byte b = frame[checkMark + 480 * 270 * 2];
          int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
          image.setRGB(width, height, pix);
          checkMark++;
        }
      }
    }
  }

  public void close() throws IOException {
      inputStream.close();
  }

  public BufferedImage getFrame() {
    return image;
  }
  
}

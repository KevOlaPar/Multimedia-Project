package media_player;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

/**
  Video class for playing sequence of images
 */

public class VideoReader implements Runnable {
  List<List<Integer>> framesList;
  RandomAccessFile rawFile;
  BufferedImage image;
  SoundReader audio;
  String status;
  JLabel label;
  byte[] videoFramesBuffer;
  int buffer;
  private int numberOfFrames;
  private int currentFrame;
  final int fps = 30;
  final int lengthInSec = 300;
  final int width = 480;
  final int height = 270;


  public VideoReader(SoundReader audio, JLabel label, String videoPath) {
    this.numberOfFrames = fps * lengthInSec;
    this.currentFrame = 0;
    this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    this.rawFile  = readVideoRGB(width, height, videoPath);
    this.status = "init";
    this.label = label;
    this.audio = audio;
  }

  public void initialize() throws InterruptedException, IOException {
    readImageRGB(0);
    label.setIcon(new ImageIcon(getImage()));
  }
  
  public void run() {
    while (true) {
      try {
        readImageRGB(0);
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
      label.setIcon(new ImageIcon(getImage()));
      try {
        Thread.sleep(25);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
   }
  }

  public BufferedImage getImage() {
    return image;
  }
  
  public void readNextFrame(int frameNumber) throws InterruptedException, IOException {
    readImageRGB(frameNumber);
  }
  
  public int getCurrentFrame() {
    return this.currentFrame;
  }

  public int getTotalFrameSize() {
    return this.numberOfFrames;
  }

  public void readImageRGB(int frameNumber) throws InterruptedException, IOException {
    this.currentFrame = frameNumber;
    int frameLength = this.width * this.height * 3;
    byte[] frame = new byte[frameLength];
    int result = rawFile.read(frame);
    if (result < 0) {
      return;
    }
    int ind = 0;
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        byte r = frame[ind];
        byte g = frame[ind + height * width];
        byte b = frame[ind + height * width * 2];
        int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
        image.setRGB(x, y, pix);
        ind++;
      }
    }
  }

  public RandomAccessFile readVideoRGB(int width, int height, String imgPath) {
    RandomAccessFile videoRawBytes = null;
    try {
      File file = new File(imgPath);
      videoRawBytes = new RandomAccessFile(file, "r");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    return videoRawBytes;
  }
  // public List<List<Integer>> preProcess() {
  //   List<List<Integer>> frames = new ArrayList<>();
  //   int max = 480 * 270 * 3;
  //   List<Integer> frame = new ArrayList<>(max);
  //   for (int index = 0; index < max; index++) {
  //     frame.add(0);
  //   }
  //   int i = 0;
  //   int j = 0;
  //   int videoLength = videoFramesBuffer.length;

  //   while (j < videoLength) {
  //     int k = j - i;
  //     if (k + 1 == max) {
  //       frames.add(new ArrayList<>(frame));
  //       j++;
  //       i = j;
  //     } else {
  //       frame.set(k, (int) videoFramesBuffer[j]);
  //       j++;
  //     }
  //   }
  //   return frames;
  // }
  
  class AsyncVideoReader extends Thread {

    public void run() {
      read();
    }
  
    public void read() {
      
    }
}
}

package media_player;
import video.Frame;
import video.VideoFrameReader;

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
  BufferedImage image;
  String status;
  JLabel label;
  VideoFrameReader videoFrameReader;
  private int numberOfFrames;
  private int currentFrame;
  final int fps = 30;
  final int lengthInSec = 300;
  final int width = 480;
  final int height = 270;


  public VideoReader(JLabel label, String videoPath) {
    this.numberOfFrames = fps * lengthInSec;
    this.currentFrame = 0;
    this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    this.status = "init";
    this.label = label;
    this.videoFrameReader = new VideoFrameReader(videoPath);
  }

  public void initialize() throws InterruptedException, IOException {
    readImageRGB(0);
    label.setIcon(new ImageIcon(getImage()));
  }
  
  public void run() {
    while (true) {
      label.setIcon(new ImageIcon(getImage()));
      try {
        Thread.sleep(3);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
   }
  }

  public BufferedImage getImage() {
    return image;
  }

  public void changeFrame(int frameNumber)  {
    try {
      readImageRGB(frameNumber);
    } catch (InterruptedException | IOException e) {
      e.printStackTrace();
    }
  }
  
  public int getCurrentFrame() {
    return this.currentFrame;
  }

  public int getTotalFrameSize() {
    return this.numberOfFrames;
  }

  public void readImageRGB(int frameNumber) throws InterruptedException, IOException {
    if(currentFrame != frameNumber) {
      this.currentFrame = frameNumber;
      Frame frame = videoFrameReader.getFrame(currentFrame);
      image = frame.toBufferedImage();
    }
  }
}

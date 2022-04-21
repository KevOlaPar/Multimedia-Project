package media_player;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
  Video class for playing sequence of images
 */

public class Video implements Runnable {
  BufferedImage image;
  JLabel label;
  JFrame frame;
  byte[] videoFramesBuffer;
  int width;
  int height;
  int frameNumber;
  String status;
  List<List<Integer>> framesList;

  public Video(String videoPath, int fps, int lengthInSec, int width, int height) {
    this.width = width;
    this.height = height;
    this.frameNumber = fps * lengthInSec;
    this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    long len = width * height * 3 * fps * lengthInSec;
    byte[] bytes = new byte[(int) len];
    this.videoFramesBuffer = readVideoRGB(width, height, videoPath, bytes);
    this.framesList = preProcess();
    showImage();
  }
  
  public void run() {
    try {
      playVideoFrame();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public void playVideo() throws InterruptedException {
    this.status = "play";
    for (int i = 1; i < frameNumber; i++) {
      readImageRGB(i);
      TimeUnit.MILLISECONDS.sleep(33);
      refresh();
    }
  }
  /**Alternative way to read frames used with the framelist instant variable amd its the reason for the preprocess step. */
  public void playVideoFrame() throws InterruptedException {
    this.status = "play";
    for (int i = 0; i < framesList.size(); i++) {
      showPicture(i);
      TimeUnit.MILLISECONDS.sleep(33);
      refresh();
    }
  }

  /**
   * Read Image RGB from a video buffer
   */
  public BufferedImage readImageRGB(int frameNumber) {
    int frameLength = this.width * this.height * 3;

    byte[] frame = new byte[frameLength];

    for (int i = 0; i < frameLength; i++) {
      frame[i] = videoFramesBuffer[(frameNumber * frameLength) + i];
    }
    int ind = 0;
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          // byte a = 0;
          byte r = frame[ind];
          byte g = frame[ind + height * width];
          byte b = frame[ind + height * width * 2];

          int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
          // int pix = ((a << 24) + (r << 16) + (g << 8) + b);

          image.setRGB(x, y, pix);
          ind++;
        }
      }
    return image;
  }

  public byte[] readVideoRGB(int width, int height, String imgPath, byte[] bytes) {
    try {
      File file = new File(imgPath);
      RandomAccessFile raf = new RandomAccessFile(file, "r");
      raf.read(bytes);
      raf.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return bytes;
  }

  public void refresh() {
    frame.repaint();
  }

  private void showImage() {
    frame = new JFrame();
    GridBagLayout gLayout = new GridBagLayout();
    frame.getContentPane().setLayout(gLayout);

    label = new JLabel(new ImageIcon(image));

    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.HORIZONTAL;
    c.anchor = GridBagConstraints.CENTER;
    c.ipadx = 200;
    c.ipady = 150;
    c.weightx = 0.5;
    c.gridx = 0;
    c.gridy = 0;

    c.fill = GridBagConstraints.HORIZONTAL;
    c.anchor = GridBagConstraints.CENTER;
    c.weightx = 0.5;
    c.gridx = 1;
    c.gridy = 0;

    frame.getContentPane().add(label, c);

    frame.pack();
    frame.setVisible(true);

  }
  
  /**
   * Preprocessing the frames
   * read the rgb in a buffer, parse the rgb to be in 480 x 270 frames
   * and store the data in a list of length frameLength
   * send each frame to be rendered or set into the image buffer and refresh per fps
   * */
  public List<List<Integer>> preProcess() {
    List<List<Integer>> frames = new ArrayList<>();
    int max = 480 * 270 * 3;
    List<Integer> frame = new ArrayList<>(max);
    for (int index = 0; index < max; index++) {
      frame.add(0);
    }
    int i = 0;
    int j = 0;
    int videoLength = videoFramesBuffer.length;

    while (j < videoLength) {
      int k = j - i;
      if (k + 1 == max) {
        frames.add(new ArrayList<>(frame));
        j++;
        i = j;
      } else {
        frame.set(k, (int) videoFramesBuffer[j]);
        j++;
      }
    }
    return frames; 
  }

  /**
   * get the first frame from the list of frames and display
   * read it to the buffer image object, then render
   */
  public void showPicture(int i) {
    List<Integer> frame = framesList.get(i);
    int ind = 0;
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int r = frame.get(ind);
        int g = frame.get(ind + height * width);
        int b = frame.get(ind + height * width * 2);
        int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
        image.setRGB(x, y, pix);
        ind++;
      }
    }
  }

  public static void main(String[] args) throws InterruptedException {
    Video video = new Video("media_player/data/data_test1.rgb", 30, 10, 480, 270);
    video.playVideoFrame();
  }
}

package video;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

/*
 * The VideoFrameReader class provides methods for sequential reads from a video(.rgb) file, by calling nextFrame()
 * You can jump to specific frame index and start sequential reading from that index by calling setFrameIndex(int index)
 */
public class ImageReader {

    public static BufferedImage getImage(int width, int height, String filename) {
        try
        {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            int frameLength = width*height;

            File file = new File(filename);
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            raf.seek(0);

            long len = 3L *frameLength;
            byte[] bytes = new byte[(int) len];

            raf.read(bytes);

            int ind = 0;
            for(int y = 0; y < height; y++)
            {
                for(int x = 0; x < width; x++)
                {
                    byte r = bytes[ind];
                    byte g = bytes[ind+frameLength];
                    byte b = bytes[ind+frameLength*2];

                    int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    image.setRGB(x,y,pix);
                    ind++;
                }
            }
            raf.close();
            return image;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    // for testing purpose
    public static void main(String[] args) {

        BufferedImage img = ImageReader.getImage(Frame.FRAME_WIDTH, Frame.FRAME_HEIGHT,"/Users/parthivmangukiya/Downloads/dataset3/Brand Images/ae_logo.rgb");

        JFrame frame = new JFrame();
        GridBagLayout gLayout = new GridBagLayout();
        frame.getContentPane().setLayout(gLayout);

        JLabel lbIm1 = new JLabel(new ImageIcon(img));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 0;

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 1;
        frame.getContentPane().add(lbIm1, c);

        frame.pack();
        frame.setVisible(true);
    }

}

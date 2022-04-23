package detection;

import video.Frame;
import video.ImageReader;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageProcessor {

    public static boolean isImagePresent(BufferedImage container, BufferedImage image) {

        return false;
    }

    public static Border getBoundary(BufferedImage container, BufferedImage image) {

        return null;
    }

    public static void main(String[] args) {
        BufferedImage img = ImageReader.getImage(video.Frame.FRAME_WIDTH, Frame.FRAME_HEIGHT,"/Users/parthivmangukiya/Downloads/dataset3/Brand Images/ae_logo.rgb");

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

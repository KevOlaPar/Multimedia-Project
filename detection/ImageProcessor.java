package detection;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import video.Frame;
import video.ImageReader;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.stream.Stream;

import video.VideoFrameReader;

public class ImageProcessor {

    public static int isImagePresent(Frame frame, Logo logo) {
        double hsvFrame[][][] = toHSV(frame.getRGB());
        double hsvLogo[][][] = logo.getHSV();

        hsvFrame = cutFrame(hsvFrame, 10);


        double[] h = Arrays.stream(hsvFrame[0])
                .flatMapToDouble(Arrays::stream)
                .toArray();
        int[] histH = Histogram.calculateHist(h, new int[]{10,178}, 180);


        return 0;
    }

    public static double[][][] toHSV(double[][][] rgbFrame) {
        float hsb[] = new float[3];
        int width = rgbFrame[0].length;
        int height = rgbFrame[0][0].length;
        double hsv[][][] = new double[3][width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                Color.RGBtoHSB((int)rgbFrame[0][i][j], (int)rgbFrame[0][i][j], (int)rgbFrame[0][i][j], hsb);
                hsv[0][i][j] = hsb[0];
                hsv[1][i][j] = hsb[1];
                hsv[2][i][j] = hsb[2];
            }
        }
        return hsv;
    }

    private static double[][][] cutFrame(double[][][] hsv, int percentage) {
        int w = hsv[0].length;
        int h = hsv[0][0].length;
        int wcut = (w*percentage)/100;
        int hcut = (h*percentage)/100;
        double[][][] hsvCut = new double[3][w-wcut][h-hcut];
        int newW = hsvCut[0].length;
        int newH = hsvCut[0][0].length;
        for (int i = 0; i < newW; i++) {
            for (int j = 0; j < newH; j++) {
                hsvCut[0][i][j] = hsv[0][i+wcut][j+hcut];
                hsvCut[1][i][j] = hsv[0][i+wcut][j+hcut];
                hsvCut[2][i][j] = hsv[0][i+wcut][j+hcut];
            }
        }
        return hsvCut;
    }

    public static Border getBoundary(Frame frame, Logo logo) {

        return null;
    }

    public static void main(String[] args) {

        VideoFrameReader videoFrameReader = new VideoFrameReader("/Users/parthivmangukiya/Downloads/dataset2/Videos/data_test2.rgb");
        BufferedImage img = ImageReader.getImage(video.Frame.FRAME_WIDTH, Frame.FRAME_HEIGHT,"/Users/parthivmangukiya/Downloads/dataset2/Brand Images/Mcdonalds_logo.bmp");

        int frameNo = 300;
        Logo logo =  new Logo(new Pair<>("/Users/parthivmangukiya/Downloads/dataset2/Brand Images/Mcdonalds_logo.bmp", null));
        int value = ImageProcessor.isImagePresent(videoFrameReader.getFrame(frameNo), logo);
        System.out.println(value);

        JFrame frame = new JFrame();
        GridBagLayout gLayout = new GridBagLayout();
        frame.getContentPane().setLayout(gLayout);

        JLabel lbIm1 = new JLabel(new ImageIcon(videoFrameReader.getFrame(frameNo).toBufferedImage()));

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

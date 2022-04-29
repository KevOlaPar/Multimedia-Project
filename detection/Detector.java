package detection;

import video.ImageReader;
import video.VideoFrameReader;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.List;
import video.Frame;

class Pair<T, U> {
    T first;
    U second;

    public Pair(T first, U second) {
        this.first = first;
        this.second = second;
    }
}

class Logo {

    public static int LOGO_WIDTH = 480;
    public static int LOGO_HEIGHT = 270;

    BufferedImage image;
    Pair<String, String> paths;
    double hsv[][][];

    public Logo(Pair<String, String> paths) {
        this.image = ImageReader.getImage(LOGO_WIDTH, LOGO_HEIGHT, paths.first);;
        this.paths = paths;
    }

    public double[][][] getRGB() {
        double rgb[][][] = new double[3][LOGO_WIDTH][LOGO_HEIGHT];

        for (int i = 0; i < LOGO_WIDTH; i++) {
            int mask = 255;
            for (int j = 0; j < LOGO_HEIGHT; j++) {
                int raw = image.getRGB(i, j);
                rgb[2][i][j] = raw & mask;
                mask <<= 8;
                rgb[1][i][j] = raw & mask;
                mask <<= 8;
                rgb[0][i][j] = raw & mask;
            }
        }
        return rgb;
    }

    public double[][][] getHSV() {
        if(hsv == null) {
            hsv = ImageProcessor.toHSV(getRGB());
        }
        return hsv;
    }
}

public class Detector {

    List<Logo> logos;


    VideoFrameReader videoReader;
    List<Pair<String, String>> logosPath;

    public Detector(List<Pair<String, String>> logosPath, VideoFrameReader videoReader) {
        this.logosPath = logosPath;

        logos = new ArrayList<>();

        for(Pair p: logosPath) {
            logos.add(new Logo(p));
        }

        this.videoReader = videoReader;
    }

    public DetectorResult detect() {
        int totalSec = videoReader.getTotalNumberOfFrames() / 30;
        for (int i=0;i<totalSec;i++) {
            Logo logo = detectLogo(i);
        }
        return null;
    }

    private SortedMap<Integer, Border> segmentVideo(Logo logo) {
        SortedMap<Integer, Border>  map = new TreeMap<>();
        int totalFrames = videoReader.getTotalNumberOfFrames();
        videoReader.setFrameIndex(0);
        Frame f = videoReader.nextFrame();
        while (f != null) {
//            if(isLogoPresent(f, logo)) {
                int frameNo = videoReader.getCurrentFrameIndex();
                Border border = segmentImage(f, logo);
                map.put(frameNo, border);
                videoReader.nextFrame();
//            }
        }
        return map;
    }

    private Logo detectLogo(int sec) {
        int startFrame = sec * 30;
        int endFrame = startFrame + 30 - 1;
        int i = startFrame;
        int[] counts = new int[logos.size()];
        while (i <= endFrame) {
            Frame frame = videoReader.getFrame(i);
            for (int j = 0; j < logos.size(); j++) {
                Logo logo = logos.get(j);
                counts[j] += isLogoPresent(frame, logo);
                i += 4;
            }
        }

        Logo detectedImage = null;
        int maxConfirmation = 0;
        for (int j = 0; j < logos.size(); j++) {
            if(counts[j] > 0 && maxConfirmation < counts[j]) {
                detectedImage = logos.get(j);
                maxConfirmation = counts[j];
            }
        }
        System.out.println("Detected Logo " + detectedImage.paths.first + " on second " + i + " with max confirmations " + maxConfirmation);
        return detectedImage;
    }

    private int isLogoPresent(Frame f, Logo logo) {
        return ImageProcessor.isImagePresent(f, logo);
    }

    private Border segmentImage(Frame f, Logo logo) {
        return ImageProcessor.getBoundary(f, logo);
    }

}

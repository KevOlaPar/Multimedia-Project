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

public class Detector {

    List<Pair<BufferedImage, Pair<String, String>>> logos;

    public static int LOGO_WIDTH = 480;
    public static int LOGO_HEIGHT = 270;
    VideoFrameReader videoReader;
    List<Pair<String, String>> logosPath;

    public Detector(List<Pair<String, String>> logosPath, VideoFrameReader videoReader) {
        this.logosPath = logosPath;

        List<Pair<BufferedImage, Pair<String, String>>> logos = new ArrayList<>();

        for(Pair p: logosPath) {
            BufferedImage img = ImageReader.getImage(LOGO_WIDTH, LOGO_HEIGHT, (String)p.first);
            Pair pair = new Pair<>(img, p);
            logos.add(pair);
        }

        this.videoReader = videoReader;
    }

    public DetectorResult detect() {
        Pair<BufferedImage, Pair<String, String>> detectedImage = detectLogo();

        if(detectedImage != null) {
            SortedMap<Integer, Border> borders = segmentVideo(detectedImage.first);
            return new DetectorResult(detectedImage.first, detectedImage.second.second, borders);
        } else {
            return null;
        }
    }

    private SortedMap<Integer, Border> segmentVideo(BufferedImage img) {
        SortedMap<Integer, Border>  map = new TreeMap<>();
        int totalFrames = videoReader.getTotalNumberOfFrames();
        videoReader.setFrameIndex(0);
        Frame f = videoReader.nextFrame();
        while (f != null) {
            if(isLogoPresent(f, img)) {
                int frameNo = videoReader.getCurrentFrameIndex();
                Border border = segmentImage(f, img);
                map.put(frameNo, border);
                videoReader.nextFrame();
            }
        }
        return map;
    }

    private Pair<BufferedImage, Pair<String, String>> detectLogo() {
        Pair<BufferedImage, Pair<String, String>> detectedImage = null;
        int maxConfirmation = 0;
        for(Pair<BufferedImage, Pair<String, String>> p: logos) {
            BufferedImage logo = (BufferedImage) p.first;
            int confirmation = 0;
            int totalFrames = videoReader.getTotalNumberOfFrames();
            int i = 0;
            while (i < totalFrames) {
                videoReader.setFrameIndex(i);
                Frame frame = videoReader.nextFrame();
                if(isLogoPresent(frame, logo)) {
                    confirmation++;
                }
                i+=3;
            }

            if(confirmation > 0 && maxConfirmation < confirmation) {
                detectedImage = p;
                maxConfirmation = confirmation;
            }
        }
        return detectedImage;
    }

    private boolean isLogoPresent(Frame f, BufferedImage logo) {
        return false;
    }

    private Border segmentImage(Frame f, BufferedImage img) {
        return null;
    }

}

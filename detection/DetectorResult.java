package detection;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.SortedMap;

class Point {
    int x;
    int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
}

class Border {

    Point leftTop;
    Point leftBottom;
    Point rightTop;
    Point rightBottom;

    public Border(Point leftTop, Point leftBottom, Point rightTop, Point rightBottom) {
        this.leftTop = leftTop;
        this.leftBottom = leftBottom;
        this.rightTop = rightTop;
        this.rightBottom = rightBottom;
    }
}

class LogoResult {
    BufferedImage logo;
    String adPath;
    SortedMap<Integer, Border> borders;

    public LogoResult(BufferedImage logo, String addPath, SortedMap<Integer, Border> borders) {
        this.logo = logo;
        this.adPath = addPath;
        this.borders = borders;
    }
}

class DetectorResult {
    SortedMap<Integer, LogoResult> logoResults;

    public DetectorResult(SortedMap<Integer, LogoResult> logoResults) {
        this.logoResults = logoResults;
    }
}

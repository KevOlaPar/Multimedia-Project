package video;

import java.util.List;

public class Scene {
    private int startIndex;
    private int endIndex;
    private boolean isAd;

    public Scene(List<Shot> shots){
        this.isAd = shots.get(0).isAd();
        startIndex = shots.get(0).getStartIndex();
        endIndex = shots.get(shots.size()-1).getEndIndex();
    }

    public int length(){
        return endIndex - startIndex;
    }

    public boolean isAd(){
        return isAd;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    @Override
    public String toString() {
        return "[" + startIndex + ", " + endIndex + ")" + "\t" + "isAd = " + isAd + "\n";
    }
}

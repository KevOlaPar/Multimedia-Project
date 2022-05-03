package video;

import audio.AudioFrame;

import java.util.List;

/*
 * shot frame index [start, end), not including end index.
 */
public class Shot {
    private int startIndex;
    private int endIndex;
    private int audioLevel;

    public Shot(int start, int end, List<Integer> audioLevels){
        startIndex = start;
        endIndex = end;
        computeAudioLevel(audioLevels);
    }

    private void computeAudioLevel(List<Integer> audioLevels){
        long total = 0;
        for(int level : audioLevels){
            total += level * level;
        }
        total /= audioLevels.size();
        total = (long)Math.sqrt(total);
        this.audioLevel = (int)total;
    }

    public boolean isAd(){
        if(length() > 600){// not an ad if longer than 600 frames
            return false;
        }
        //compare only upper
        if(AudioFrame.AUDIO_LEVEL_THRESHOLD_LOWER == 0){
            return this.audioLevel > AudioFrame.AUDIO_LEVEL_THRESHOLD_UPPER;
        }
        //compare only lower
        else if (AudioFrame.AUDIO_LEVEL_THRESHOLD_UPPER == 0){
            return this.audioLevel < AudioFrame.AUDIO_LEVEL_THRESHOLD_LOWER;
        }
        //both are not 0, compare both
        else{
            return this.audioLevel > AudioFrame.AUDIO_LEVEL_THRESHOLD_UPPER || this.audioLevel < AudioFrame.AUDIO_LEVEL_THRESHOLD_LOWER;
        }
    }
    public int length(){
        return endIndex - startIndex;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public int getAudioLevel() {
        return audioLevel;
    }

    @Override
    public String toString(){
        return "[" + startIndex + ", " + endIndex + ")" + "\t" + audioLevel + "\n";
    }
}

package audio;

import java.util.Arrays;

/*
 * Auido Sampling Rate: 48000 HZ (samples/second)
 * Bits per sample: 16 (2 bytes/sample)
 * 48,000/30 = 1,600 samples/video frame
 * 96,000/30 = 3,200 bytes/video frame
 * wav files have a header of size 44 bytes
 */
public class AudioFrame {
    public static final int BYTES_PER_FRAME = 3200;
    public static final int SAMPLES_PER_FRAME = 1600;
    public static final int BITS_PER_SAMPLE = 16;
    public static final int BYTES_PER_SAMPLE = 2;
    public static int AUDIO_LEVEL_THRESHOLD_UPPER = 2000;
    public static int AUDIO_LEVEL_THRESHOLD_LOWER = 1000;
    private int frameIndex; // the frame index in terms of video frame
    private byte[] frameBytes; // size = 3200
    private int[] frameSamples; // size = 1600

    public AudioFrame(byte[] bytes, int frameIndex){
        this.frameBytes = bytes;
        this.frameIndex = frameIndex;
        this.frameSamples = convertBytesToSamples(frameBytes);
    }

    /*
     * convert bytes into samples.
     * Since the data is 16 bits/sample, so 2 bytes/sample.
     */
    public int[] convertBytesToSamples(byte[] bytes){

        int[] samples = new int[SAMPLES_PER_FRAME];

        //combine 2 bytes into 1 sample
        for(int i = 0; i < BYTES_PER_FRAME; i += 2){
            //wav format uses little-endian so the MSB comes after LSB
            //dont need to & 0xff for the left most bits because java does sign extention
            //and we need to preserve the sign value
            samples[i/2] = ((bytes[i] & 0xff)) | (bytes[i+1] << 8);
        }
        return samples;
    }

    /*
     * Calculate the average audio level in current frame
     * using root-mean-square method
     */
    public int getAverageAudioLevel(){
        long total = 0;
        for(int sample : frameSamples){
            total += sample * sample;
        }

        total /= SAMPLES_PER_FRAME;
        total = (long) Math.sqrt(total);

        return (int) total;
    }

    public int getFrameIndex() {
        return frameIndex;
    }

    public byte[] getFrameBytes(){
        return frameBytes;
    }

    public int[] getFrameSamples(){
        return frameSamples;
    }
}

package audio;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * Auido Sampling Rate: 48000 HZ (samples/second)
 * Bits per sample: 16 (2 bytes/sample)
 * 48,000/30 = 1,600 samples/video frame
 * 96,000/30 = 3,200 bytes/video frame
 * wav files have a header of size 44 bytes
 */
public class AudioFrameReader {
    private AudioInputStream audioStream;
    private File audioFile;
    private long fileLength; // total length of file in bytes
    private int currentFrameIndex;

    public AudioFrameReader(String filename) throws UnsupportedAudioFileException, IOException {
        audioFile = new File(filename);
        audioStream = AudioSystem.getAudioInputStream(audioFile);

        System.out.println(audioStream.getFormat().toString());

        fileLength = audioStream.available();
        //audioStream.skip(44);
        currentFrameIndex = 0;
    }

    /*
     *  read the next audio frame from file
     *
     *  @return returns a AudioFrame, or null if there are no more frames to read
     */
    public AudioFrame nextFrame() throws IOException {

        byte[] frameBytes = new byte[AudioFrame.BYTES_PER_FRAME];

        int numBytesRead = audioStream.read(frameBytes);
        //return null if it is end of the file
        if(numBytesRead == -1){
            return null;
        }

        AudioFrame frame = new AudioFrame(frameBytes, currentFrameIndex);

        return frame;

    }

    public void setFrameIndex(int index) throws IOException {
        audioStream.reset();
        audioStream.skip((long)AudioFrame.BYTES_PER_FRAME * (long)index);
        currentFrameIndex = index;
    }

    public void closeFile(){
        try {
            audioStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public long getFileLength(){
        return fileLength;
    }

    public int getTotalNumberOfFrames(){
        return (int)(fileLength/AudioFrame.BYTES_PER_FRAME);
    }

    public AudioInputStream getAudioStream() {
        return audioStream;
    }

    public static void main(String[] args){

        //test
        String filename = "C:\\Users\\Kevin Yu\\Downloads\\dataset-001\\dataset\\Videos\\data_test1.wav";
        try {
            AudioFrameReader reader = new AudioFrameReader(filename);

            System.out.println("file length = " + reader.getFileLength());
            System.out.println("total frames = " + reader.getTotalNumberOfFrames());

            List<Integer> audioLevelList = new ArrayList<>();
            List<int[]> samplesList = new ArrayList<>();
            List<Integer> bufferList = new ArrayList<>();
            int  index = 0;
            while(true){
                AudioFrame frame = reader.nextFrame();
                if(frame == null){
                    break;
                }
                int level = frame.getAverageAudioLevel();
                bufferList.add(level);

                // combine 5 seconds of audio data
                if(bufferList.size() == 150){
                    long total = 0;
                    for(int i : bufferList){
                        total += i * i;
                    }
                    total /= 150;
                    total = (long) Math.sqrt(total);
                    audioLevelList.add((int) total);
                    bufferList.clear();
                }
                index++;
            }
            reader.closeFile();

            BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\Users\\Kevin Yu\\Desktop\\audio_levels.txt"));
			for(int i=0; i<audioLevelList.size(); i++){
                int level = audioLevelList.get(i);
                writer.write(String.valueOf(i * 150)+"-"+String.valueOf(i*150 + 149) + "\t" + String.valueOf(level) + "\n");
			}
			writer.close();

            writer = new BufferedWriter(new FileWriter("C:\\Users\\Kevin Yu\\Desktop\\audio_samples.txt"));
            for(int i=0; i<samplesList.size(); i++){
                int[] samples = samplesList.get(i);
                writer.write(String.valueOf(i) + "\t" + Arrays.toString(samples) + "\n");
            }
            writer.close();

        } catch (UnsupportedAudioFileException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}

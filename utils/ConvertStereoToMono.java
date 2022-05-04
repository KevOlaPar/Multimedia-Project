package utils;

import audio.AudioFrame;
import video.Scene;

import java.io.IOException;
import java.io.RandomAccessFile;

public class ConvertStereoToMono {
    public static void main(String[] args) throws IOException {
        String header = "C:\\Users\\Kevin Yu\\Downloads\\dataset-001\\dataset\\Videos\\data_test1.wav";
        String file = "C:\\Users\\Kevin Yu\\Downloads\\dataset-004\\test1.wav";
        String file_out = "C:\\Users\\Kevin Yu\\Downloads\\dataset-004\\test2.wav";
        RandomAccessFile audioOutput = new RandomAccessFile(file_out, "rw");
        RandomAccessFile audioInput = new RandomAccessFile(file, "r");
        RandomAccessFile headerInput = new RandomAccessFile(header, "r");

        //write header
        byte[] headerBuffer = new byte[44];
        headerInput.read(headerBuffer);
        audioOutput.write(headerBuffer);
        audioInput.skipBytes(44);
        headerInput.close();

        byte[] audioBuffer = new byte[AudioFrame.BYTES_PER_FRAME];
        int frameIndex = 0;
        int bytesRead = 0;
        while ((bytesRead = audioInput.read(audioBuffer)) != -1){
            audioOutput.write(audioBuffer, 0, bytesRead);
            //audioInput.skipBytes(AudioFrame.BYTES_PER_FRAME);
            frameIndex++;
            if(frameIndex == 9003){
                break;
            }
        }
        System.out.println("total frames = " + frameIndex);

        audioOutput.close();
        audioInput.close();
    }
}

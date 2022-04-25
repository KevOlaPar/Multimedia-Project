package video;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

/*
 * The VideoFrameReader class provides methods for sequential reads from a video(.rgb) file, by calling nextFrame()
 * You can jump to specific frame index and start sequential reading from that index by calling setFrameIndex(int index)
 */
public class VideoFrameReader {
	
	private RandomAccessFile videoFile;
	private long fileLength;
	private int currentFrameIndex;
	
	public BufferedImage img;
	
	
	public VideoFrameReader(String filename) {
		
		try {
			videoFile = new RandomAccessFile(filename, "r");
			fileLength = videoFile.length();
			currentFrameIndex = 0;
		} catch (FileNotFoundException e) {
			System.out.println("Error: file not found");
			e.printStackTrace();
		} catch (IOException ioe) {
			System.out.println("Error: cannot get length of the video file");
			ioe.printStackTrace();
		}
		
//		img = new BufferedImage(Frame.FRAME_WIDTH, Frame.FRAME_HEIGHT, BufferedImage.TYPE_INT_RGB);
	}
	
	
	/*
	 *  read the next frame from file
	 *  
	 *  @return returns a Frame, or null if there are no more frames to read
	 */
	public Frame nextFrame() {
		
		double[][] red = new double[Frame.FRAME_WIDTH][Frame.FRAME_HEIGHT];
		double[][] green = new double[Frame.FRAME_WIDTH][Frame.FRAME_HEIGHT];
		double[][] blue = new double[Frame.FRAME_WIDTH][Frame.FRAME_HEIGHT];
		
		byte[] frameBytes = new byte[Frame.FRAME_WIDTH * Frame.FRAME_HEIGHT * 3];
		
		try {
			
			int readFlag = videoFile.read(frameBytes);
			
			//reached end of the file, return null
			if(readFlag == -1) {
				return null;
			}
			
			int ind = 0;
			for(int y = 0; y < Frame.FRAME_HEIGHT; y++){
				
				for(int x = 0; x < Frame.FRAME_WIDTH; x++){
					
					//byte a = 0;
					byte r = frameBytes[ind];
					byte g = frameBytes[ind+Frame.FRAME_HEIGHT*Frame.FRAME_WIDTH];
					byte b = frameBytes[ind+Frame.FRAME_HEIGHT*Frame.FRAME_WIDTH*2]; 
					
					red[x][y] = (r & 0xff);
					green[x][y] = (g & 0xff);
					blue[x][y] = (b & 0xff);
					
					// for testing purpose
//					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
//					img.setRGB(x,y,pix);
					
					ind++;
				}
			}
			
		} catch(FileNotFoundException fnfe) {
			System.out.println("Error: file not found");
			fnfe.printStackTrace();
		} catch (IOException ioe) {
			System.out.println("Error: seek index failed");
			ioe.printStackTrace();
		}
		
		//after reading all RGB values, construct and return the frame
		Frame frame = new Frame(red, green, blue, currentFrameIndex);
		
		currentFrameIndex++;
		
		return frame;
	}

	public int getCurrentFrameIndex() {
		return currentFrameIndex;
	}

	// remember to close file every time to avoid memory leak
	public void closeFile() {
		try {
			videoFile.close();
		} catch (IOException e) {
			System.out.println("Error: closing video file failed");
			e.printStackTrace();
		}
	}
	
	
	/*
	 * set the frame index to a specific position
	 * 
	 * @param: the frame index for next frame read
	 */
	public void setFrameIndex(int index) {
		try {
			
			// index * frame_size
			videoFile.seek(index * Frame.FRAME_WIDTH * Frame.FRAME_HEIGHT * 3);
			currentFrameIndex = index;
			
		} catch (IOException e) {
			System.out.println("Error: seek index failed");
			e.printStackTrace();
		}
	}
	
	
	public long getFileLength() {
		return fileLength;
	}
	
	public int getTotalNumberOfFrames() {
		return (int)(fileLength/(Frame.FRAME_WIDTH * Frame.FRAME_HEIGHT * 3));
	}
	
	// for testing purpose
	public static void main(String[] args) {
		
		VideoFrameReader reader = new VideoFrameReader("data/data_test1.rgb");
		
		reader.img = new BufferedImage(Frame.FRAME_WIDTH, Frame.FRAME_HEIGHT, BufferedImage.TYPE_INT_RGB);
		System.out.println("Total # of Frames = " + String.valueOf(reader.getTotalNumberOfFrames()));
		
		//jump to a specific starting frame index
		reader.setFrameIndex(3000);
		reader.nextFrame();
		
		JFrame frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		JLabel lbIm1 = new JLabel(new ImageIcon(reader.img));

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
		
		// play the video by "count" number of frames, 30 fps.
		int count = 0;
		while(count < 3000) {
			reader.nextFrame();
			lbIm1.setIcon(new ImageIcon(reader.img));
			count++;
			try {
				Thread.sleep(33);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		reader.closeFile();
	}

}

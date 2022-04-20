package utils;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import video.Frame;

public class Utils {
	
	public static void displayFrame(double[][] r, double[][] g, double[][] b) {
		
		BufferedImage img = new BufferedImage(Frame.FRAME_WIDTH, Frame.FRAME_HEIGHT, BufferedImage.TYPE_INT_RGB);
		
		for(int row=0; row<Frame.FRAME_WIDTH; row++) {
			for(int col=0; col<Frame.FRAME_HEIGHT; col++) {
				int pix = 0xff000000 | (((int)r[row][col] & 0xff) << 16) | (((int)g[row][col] & 0xff) << 8) | ((int)b[row][col] & 0xff);
				img.setRGB(row,col,pix);
			}
		}
		
		JFrame frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		JLabel lbIm1 = new JLabel(new ImageIcon(img));

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
	
	public static void displayErrorFrame(double[][] y) {
		displayFrame(y, y, y);
	}
	
	public static void writeChannelToDisk(String destination, double[][] val) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(destination + "\\channel_values.txt"));
			for(int i=0; i<val.length; i++) {
				for(int j=0; j<val[0].length; j++) {
					writer.append(val[i][j] + " ");
				}
				writer.append("\n");
			}
			writer.append("\n");
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
}

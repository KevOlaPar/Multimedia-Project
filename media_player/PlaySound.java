package media_player;
import java.io.IOException;
import java.util.Scanner;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class PlaySound {
  public static void main(String[] args) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
    try
    {
        SoundReader audioPlayer = new SoundReader(args[0]);

          
        audioPlayer.play();
        Scanner scanner = new Scanner(System.in);
          
        while (true)
        {
            System.out.println("1. pause");
            System.out.println("2. resume");
            System.out.println("3. restart");
            System.out.println("4. stop");
            System.out.println("5. Jump to specific time");
            int choice = scanner.nextInt();
            audioPlayer.goToChoice(choice);
            if (choice == 4)
            break;
        }
        scanner.close();
    } 
      
    catch (Exception ex) 
    {
        System.out.println("Error with playing sound.");
        ex.printStackTrace();
      
      }

  }
}
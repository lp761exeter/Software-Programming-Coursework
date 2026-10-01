import java.util.Scanner;
import java.util.Queue;
import java.util.LinkedList;
import java.io.File;
import java.io.FileNotFoundException;

public class CardGame 
{
    static Scanner scan = new Scanner(System.in);
    static Queue<Integer> pack;
    static int players;
    static String deckFile;

    public static void main(String[] args) throws FileNotFoundException
    {
        System.out.println("Please enter the number of Players:");
        players = scan.nextInt();
        System.out.println("Please enter the pack file name:");
        deckFile = scan.next();

        boolean found = false;
        while (!found)
        {
            try
            {
                pack = loadPack(deckFile);
                found = true;
            }
            catch (FileNotFoundException e)
            {
                System.out.println("File not found. Please try again");
            }
        }
        
        System.out.println(pack);

    }

    /*
    Accepts a FileName string
    returns an Integer Queue from the file loaded with that string name
    files are loaded from the Decks directory
     */
    public static Queue<Integer> loadPack(String fileName) throws FileNotFoundException
    {
        Queue<Integer> pack = new LinkedList<>();

        // if the user doesn't specify the .txt, this will autocomplete it
        if (fileName.length()>=4 && !fileName.substring(fileName.length()-4).equals(".txt"))
        {
            fileName+=".txt";
        }

        // load file with error handling
        File packFile = new File("Decks", fileName);
        Scanner fileScanner = new Scanner(packFile);
        while (fileScanner.hasNextLine())
        {
            String line = fileScanner.nextLine().trim();
            if (!line.isEmpty())
            {
                pack.add(Integer.parseInt(line));
            }
        }
        fileScanner.close();

        return pack;
    }
}
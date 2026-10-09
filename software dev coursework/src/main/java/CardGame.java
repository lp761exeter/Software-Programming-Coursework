import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.InputMismatchException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;

public class CardGame 
{
    static Scanner scan = new Scanner(System.in);
    static int nextId = 1;
    static Queue<Card> pack;
    static List<Queue<Card>> decks = new ArrayList<>();
    static int playersNum;
    static Player[] players;


    public static void main(String[] args)
    {
        playersNum = getNumPlayersInput();
        pack = getPackFromFile();
        pack = shuffleQueue(pack);
        players = makePlayers(playersNum);
        dealCardsToPlayers();
        makeDecks();


        printAllDecks();
        printAllHands();

    }

    /*
    Populates all decks evenly from the pack object
    should be used after dealing hands to players
    should be used to initialice the game
     */
    public static void makeDecks()
    {
        for (int i = 0; i<playersNum; i++)
        {
            Queue<Card> deck = new LinkedList<>();
            decks.add(deck);
        }
        int index = 0;
        while (!pack.isEmpty())
        {
            decks.get(index).add(pack.remove());
            index++;
            if (index>decks.size()-1)
            {
                index = 0;
            }
        }
    }

    public static void printAllDecks()
    {
        for (int i = 0; i<decks.size(); i++)
        {
            System.out.println(decks.get(i));
        }
    }

    /*
    prints all players' hands
     */
    public static void printAllHands()
    {
        for (int i = 0; i<players.length; i++)
        {
            players[i].printHand();
        }
    }

    /*
    Deals hands from the pack object to each players' hands
    should be used to initialize the game
     */
    public static void dealCardsToPlayers()
    {
        for (int i = 0; i<4; i++)
        {
            for (int u = 0; u<players.length; u++)
            {
                players[u].addToHand(pack.remove());
            }
        }
    }

    /*
    Creates a Player array to populate the global array
    creates each player with a unique ID
    should be used to initialize the game
     */
    public static Player[] makePlayers(int numPlayers)
    {
        Player[] result = new Player[numPlayers];
        for (int i = 0; i<numPlayers; i++)
        {
            result[i] = new Player(nextId);
            nextId++;
        }
        return result;
    }

    /*
    Accepts a Queue of Cards and shuffles the elements by using the Collections library
     */
    public static Queue<Card> shuffleQueue(Queue<Card> pack)
    {
        Queue<Card> result = new LinkedList<>();
        List<Card> cards = new ArrayList<>(pack);
        Collections.shuffle(cards);
        result.addAll(cards);
        return result;
    }

    /*
    Prompts the user to input the name of the required pack file
    loads the pack file from the name they provide
    Utilizes the loadPack function
    Handles invalid inputs
     */
    public static Queue<Card> getPackFromFile()
    {
        boolean valid = false;
        Queue<Card> input = null;
        while (!valid)
        {
            System.out.println("Please enter the pack file name");
            String fileName = scan.nextLine();
            try 
            {
                input = loadPack(fileName);
                valid = true;
            }
            catch (FileNotFoundException e)
            {
                System.out.println("Invalid input or file not found. Please try again");
            }
        }
        return input;
    }

    /*
    Prompts the user to input the number of players
    saves the input in the global variable
    handles invalid inputs
     */
    public static int getNumPlayersInput()
    {
        boolean valid = false;
        int input = -1;
        while (!valid)
        {

            System.out.println("Please enter the number of Players:");
            try 
            {
                input = scan.nextInt();
                scan.nextLine();
                if (input<1)
                {
                    System.out.println("Number of players cannot be less than 1. Please try again");
                }
                else
                {
                    valid = true;
                }
            }
            catch (InputMismatchException e)
            {
                System.out.println("Input must be a valid number. Please try again");
                scan.next();
            }

        }
        return input;
    }

    /*
    Accepts a FileName string
    returns a Card Queue from the file loaded with that string name
    files are loaded from the Decks directory
     */
    public static Queue<Card> loadPack(String fileName) throws FileNotFoundException
    {
        Queue<Card> pack = new LinkedList<>();

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
                pack.add(new Card(Integer.parseInt(line)));
            }
        }
        fileScanner.close();

        return pack;
    }
}
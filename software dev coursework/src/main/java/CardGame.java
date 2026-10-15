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
    // globals
    static Scanner scan = new Scanner(System.in);
    static int nextId = 1;
    static Queue<Card> pack;
    static List<Deck> decks = new ArrayList<>();
    static int playersNum;
    static Player[] players;
    static int tick = 0;
    static int tickLimit = 10000;

    public static void main(String[] args)
    {
        // initialization
        playersNum = getNumPlayersInput();
        pack = getPackFromFile();

        // There needs to be at least 5 times the number of players for the game to be playable
        // i.e. each player needs 4 cards and each deck needs 1 card
        // program dies if this condition is not met
        int requiredCards = 5*playersNum;
        if (pack.size() < requiredCards)
        {
            System.out.println("Not enough cards to deal four cards to each player and at least one card to each deck. Game cannot start.");
            return;
        }

        pack = (askToShuffle()? shuffleQueue(pack): pack);
        players = makePlayers(playersNum);
        dealCardsToPlayers();
        dealCardsToDecks();
        assignDecksToPlayers();
        System.out.println("Current game setup:");
        printGameState();
        // end initialization

        // game loop. runs until game is won
        int winner = checkForWinners();
        while (winner == -1 && tick < tickLimit)
        {
            tick();
            winner = checkForWinners();
        }

        // if the above while loop is escaped while winner == -1, then the tick limit was reached
        // if not, write results to output files
        // if so, report tick limit reached
        if (winner != -1)
        {
            for (int i = 0; i<players.length; i++)
            {
                players[i].reportWinner(winner);
            }
            for (int i = 0; i<decks.size(); i++)
            {
                decks.get(i).writeFinalContents();
            }
            System.out.println("The game has been won by Player " + winner);
        }
        else
        {
            System.out.println("The game stopped after reaching the tick limit of " + tickLimit + ".");
        }

    }
    
    public static void tick()
    {
        for (int i = 0; i<players.length; i++)
        {
            Player currentPlayer = players[i];
            Card[] hand = currentPlayer.getHand();
            int discardPos = -1;
            for (int u = 0; u<hand.length; u++)
            {
                if (hand[u].getValue()!=currentPlayer.getId())
                {
                    discardPos = u;
                    u = hand.length;
                }
            }
            currentPlayer.discard(discardPos);
            currentPlayer.draw();
        }
        tick++;
        for (Player player : players)
        {
            player.writeCurrentHand();
        }
        printGameState();
    }

    /*
    returns the id of any player that has won in the current game state
    if there is more than one winner, it will return the one with the highest id.
    if there are no winners, it will return -1.
     */
    public static int checkForWinners()
    {   
        int result = -1;
        for (int i = 0; i<players.length; i++)
        {   
            boolean winner = true;
            Card[] hand = players[i].getHand();
            for (int u = 0; u<hand.length-1; u++)
            {
                if (hand[u].getValue() != hand[u+1].getValue())
                {
                    winner = false;
                }
            }
            if (winner)
            {
                result = players[i].getId();
            }
        }
        return result;
    }

    public static void printGameState()
    {
        System.out.println("Current tick: "+tick);
        printAllDecks();
        printAllHands();
        System.out.println();
    }

    /*
    Assigns the draw and discard decks to each player. 
    Player with id 1 is treated specially since they need the last deck
    this method should be used after both the players and decks have been initialized
     */
    public static void assignDecksToPlayers()
    {   
        // the first player's draw deck must be the last deck in the list
        players[0].assignDecks(decks.get(decks.size()-1), decks.get(0));

        // the rest of the players are assigned the other decks in order
        int index = 0;
        for (int i = 1; i<players.length; i++)
        {
            players[i].assignDecks(decks.get(index), decks.get(index+1));
            index++;
        }
    }

    /*
    asks the user if they want to shuffle the deck
    return a true if so and a false if not
    used with shuffleQueue to initialize the game
     */
    public static boolean askToShuffle()
    {
        boolean valid = false;
        boolean result = false;

        System.out.println("Would you like to shuffle the pack? Y/N (Makes game non-deterministic)");
        while (!valid)
        {
            String input = scan.nextLine();
            input = input.toLowerCase();

            if (input.equals("y") || input.equals("yes"))
            {
                result = true;
                valid = true;
            }
            if (input.equals("n") || input.equals("no"))
            {
                result = false;
                valid = true;
            }
            if (!valid)
            {
                System.out.println("Invalid input. Please enter Y for yes or N for no.");       
            }
        }
        return result;
    }

    /*
    Creates decks and addes them to the global decks object
    Populates all decks in a round robin fashion. takes cards from the global pack object
    should be used after dealing hands to players
    should be used to initialice the game
     */
    public static void dealCardsToDecks()
    {
        for (int i = 0; i<playersNum; i++)
        {
            decks.add(new Deck(i + 1));
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

    /*
    prints the contents of all of the decks
     */
    public static void printAllDecks()
    {
        for (int i = 0; i<decks.size(); i++)
        {
            Deck deck = decks.get(i);
            List<Card> cards = deck.getCards();
            String print = "Deck "+deck.getId()+": ";
            for (int u = 0; u<cards.size(); u++)
            {
                print += cards.get(u).getValue() + ", ";
            }
            System.out.println(print);
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
    Deals hands from the pack object to each players' hands in a round robin fashion 
    (each player gets one card at a time)
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
    probably slow. oh well
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
    returns a queue of cards to represent a stack of cards
    Utilizes the loadPack method
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
    handles invalid inputs such as letters, negative numbers, and too large numbers 
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
    If the file is not found, the error is caught and the user is reprompted via the getPackFromFile method
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

        // load file from the decks folder. Create the pack object from each line
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
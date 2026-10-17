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

    // main executor function
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
        Player.resetOutputFiles();
        Deck.resetOutputFiles();
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

        System.out.println("\nPress enter to exit");
        scan.nextLine();

    }
    
    /**
     * Runs one game tick by creating one thread for each player. A shared monitor makes each
     * worker wait until all workers have started, then releases them together. The method joins
     * every worker before incrementing the tick and recording and printing the resulting game
     * state. Any failure raised by a player turn is propagated after all threads finish.
     *
     * - Throws IllegalStateException if the calling thread is interrupted while coordinating turns,
     *   or if a player thread fails with a checked exception
     * - Throws RuntimeException if a player thread raises a runtime exception
     * - Throws Error if a player thread raises an error
     */
    public static void tick()
    {
        Object turnGate = new Object();
        int[] readyPlayers = {0};
        boolean[] startTurns = {false};
        Throwable[] failures = new Throwable[players.length];
        Thread[] turnThreads = new Thread[players.length];

        for (int i = 0; i < players.length; i++)
        {
            final int playerIndex = i;
            Player player = players[i];
            turnThreads[i] = new Thread(() ->
            {
                synchronized (turnGate)
                {
                    readyPlayers[0]++;
                    turnGate.notifyAll();
                    while (!startTurns[0])
                    {
                        try
                        {
                            turnGate.wait();
                        }
                        catch (InterruptedException e)
                        {
                            Thread.currentThread().interrupt();
                            failures[playerIndex] = e;
                            return;
                        }
                    }
                }

                try
                {
                    player.run();
                }
                catch (Throwable failure)
                {
                    failures[playerIndex] = failure;
                }
            }, "player-" + player.getId());
            turnThreads[i].start();
        }

        boolean interrupted = false;
        synchronized (turnGate)
        {
            while (readyPlayers[0] < players.length)
            {
                try
                {
                    turnGate.wait();
                }
                catch (InterruptedException e)
                {
                    interrupted = true;
                    break;
                }
            }
            startTurns[0] = true;
            turnGate.notifyAll();
        }

        for (Thread turnThread : turnThreads)
        {
            boolean joined = false;
            while (!joined)
            {
                try
                {
                    turnThread.join();
                    joined = true;
                }
                catch (InterruptedException e)
                {
                    interrupted = true;
                }
            }
        }

        if (interrupted)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for player turns");
        }

        for (Throwable failure : failures)
        {
            if (failure instanceof RuntimeException)
            {
                throw (RuntimeException) failure;
            }
            if (failure instanceof Error)
            {
                throw (Error) failure;
            }
            if (failure != null)
            {
                throw new IllegalStateException("A player turn failed", failure);
            }
        }

        tick++;
        for (Player player : players)
        {
            player.writeCurrentHand();
        }
        printGameState();
    }

    /**
     * Checks every player's hand for four cards with the same value. If more than one player has
     * a winning hand, the player with the highest ID is selected because the result is updated
     * while scanning players in array order.
     *
     * - Returns the winning player's ID, or -1 if no player currently has a winning hand
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

    /**
     * Prints the current tick number, the contents of every deck, and every player's hand to
     * standard output. This method reports the current state without modifying game data.
     */
    public static void printGameState()
    {
        System.out.println("Current tick: "+tick);
        printAllDecks();
        printAllHands();
        System.out.println();
    }

    /**
     * Prints each deck's ID and the values of its cards to standard output. Deck contents are
     * obtained as snapshots so that the deck's internal queue is not exposed.
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

    /**
     * Prints the hand of each player to standard output in player-array order.
     */
    public static void printAllHands()
    {
        for (int i = 0; i<players.length; i++)
        {
            players[i].printHand();
        }
    }

    /**
     * Assigns each player a draw deck and a discard deck in the game's circular arrangement.
     * Player zero draws from the final deck and discards to the first; each remaining player
     * draws from the next deck and discards to the deck after it. Both the player array and deck
     * list must already be initialized with matching counts.
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

    /**
     * Creates one deck per player, appends the new decks to the global deck list, then distributes
     * every card remaining in the global pack among those decks in round-robin order. Call this
     * after dealing the initial player hands; the global pack is empty when this method returns.
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

    /**
     * Deals four cards to every player from the global pack in round-robin order, giving each
     * player one card per pass. The pack must contain at least four cards per player.
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

    /**
     * Creates a player array of the requested size. Each new player receives the next unique
     * value from the global ID counter and initializes its own output file.
     *
     * - Number of players: number of players to create; expected to be positive
     * - Returns an array containing the newly created players
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

    /**
     * Copies the supplied pack into a list, randomizes the list, and returns a new queue containing
     * the shuffled cards. The supplied queue is not modified.
     *
     * - Pack: queue of cards to shuffle
     * - Returns a new queue containing the same cards in randomized order
     */
    public static Queue<Card> shuffleQueue(Queue<Card> pack)
    {
        Queue<Card> result = new LinkedList<>();
        List<Card> cards = new ArrayList<>(pack);
        Collections.shuffle(cards);
        result.addAll(cards);
        return result;
    }

    /**
     * Repeatedly prompts for a pack filename and loads it from the Decks directory.
     * A missing file is reported and causes another prompt; file contents that cannot be parsed
     * as card values are not treated as a filename retry.
     *
     * - Returns a queue containing the cards read from the selected pack file
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

    /**
     * Prompts until the user enters a positive integer player count. Non-numeric input and values
     * below one are reported and retried. The accepted value is returned; the caller stores it
     * in the global player-count field.
     *
     * - Returns the positive number of players entered by the user
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

    /**
     * Prompts until the user answers yes or no to whether the pack should be shuffled.
     *
     * - Returns true for a yes response, or false for a no response
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

    /**
     * Reads a pack file from the Decks directory and creates one card for each non-empty
     * line. If the filename does not end in .txt, that extension is appended. Each
     * non-empty line must contain an integer card value.
     *
     * - Filename: filename to load, with or without the .txt extension
     * - Returns a queue of cards in the same order as their lines in the file
     * - Throws FileNotFoundException if the requested file does not exist or cannot be opened
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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.stream.Stream;

public class Player implements Runnable
{
    private int id;
    private ArrayList<Card> hand = new ArrayList<>();
    private Deck drawDeck;
    private Deck discardDeck;

    /**
     * Creates a player with the supplied ID and initializes that player's action log. The player's
     * draw and discard decks are assigned separately after the decks have been created.
     *
     * - ID: unique identifier to associate with this player
     */
    public Player(int i)
    {
        id = i;
        initializeOutputFile();
    }

    /*
    returns the player's id
     */
    public int getId()
    {
        return id;
    }

    /*
    returns an array representation of the hand object
     */
    public synchronized Card[] getHand() 
    {
        Card[] result = new Card[4];
        for (int i = 0; i<result.length; i++)
        {
            result[i] = hand.get(i);
        }
        return result;
    }

    public void assignDecks(Deck draw, Deck discard)
    {
        drawDeck = draw;
        discardDeck = discard;
    }

    /**
     * Removes the next card from the assigned draw deck, adds it to this player's hand, and logs
     * the draw. This helper performs only a draw; a normal game turn should use the run method to
     * keep the paired discard and draw atomic.
     *
     * - Throws NoSuchElementException if the draw deck is empty
     */
    public synchronized void draw()
    {
        Card card = drawDeck.remove();
        hand.add(card);
        writeAction("player "+id+" draws a " + card.getValue() + " from deck " + drawDeck.getId());
    }

    /**
     * Executes one player turn as a discard followed by a draw. The first card in the hand whose
     * value differs from this player's ID is discarded. Both involved decks are locked in
     * ascending deck-ID order while the cards and hand are updated, preventing conflicting player
     * turns from interleaving their deck operations. The player monitor protects this player's
     * hand for the duration of the turn.
     *
     * - Throws NoSuchElementException if the draw deck is empty
     * - Throws IndexOutOfBoundsException if no card in the hand differs from this player's ID
     * - Throws UncheckedIOException if either action cannot be appended to the player log
     */
    @Override
    public synchronized void run()
    {
        int discardPos = -1;
        for (int i = 0; i < hand.size(); i++)
        {
            if (hand.get(i).getValue() != id)
            {
                discardPos = i;
                break;
            }
        }

        Deck firstDeck = drawDeck.getId() < discardDeck.getId() ? drawDeck : discardDeck;
        Deck secondDeck = firstDeck == drawDeck ? discardDeck : drawDeck;
        synchronized (firstDeck)
        {
            synchronized (secondDeck)
            {
                Card discardedCard = hand.get(discardPos);
                Card drawnCard = drawDeck.remove();
                discardDeck.add(discardedCard);
                hand.remove(discardPos);
                hand.add(drawnCard);
                writeAction("player " + id + " discards a " + discardedCard.getValue()
                        + " to deck " + discardDeck.getId());
                writeAction("player " + id + " draws a " + drawnCard.getValue()
                        + " from deck " + drawDeck.getId());
            }
        }
    }

    /**
     * Moves the card at the specified hand position to this player's discard deck and records the
     * discard action. This method is provided separately from the run method for direct use and
     * testing; it does not perform a draw.
     *
     * - Position: zero-based position of the card to discard
     * - Throws IndexOutOfBoundsException if the position is outside the hand
     */
    public synchronized void discard(int pos)
    {
        Card card = hand.get(pos);
        discardDeck.add(card);
        hand.remove(pos);
        writeAction("player "+id+" discards a " + card.getValue() + " to deck " + discardDeck.getId());
    }

    /**
     * Adds a card directly to the hand for initialization or testing. This operation does not move 
     * the card from a deck and does not write an action to the log. For those purposes, please use draw()
     *
     * - Card: card to add when the hand has available capacity
     */
    public synchronized void addToHand(Card c)
    {
        hand.add(c);
    }

    /**
     * Removes the card at the specified hand position for testing or setup. This
     * operation does not move the card to a deck and does not write an action to the log. 
     * For those purposes, please use discard()
     * 
     * - Position: zero-based position of the card to remove
     * - Throws IndexOutOfBoundsException if the position is outside the hand
     */
    public synchronized void deleteCard(int pos)
    {
        hand.remove(pos);
    }

    /**
     * Prints this player's ID and the values of the cards currently in the hand to standard
     * output.
     */
    public synchronized void printHand()
    {
        String result = "Player "+id+" hand: ";
        for (int i = 0; i<hand.size(); i++)
        {
            Card card = hand.get(i);
            result+=""+card.getValue()+", ";
        }
        System.out.println(result);
    }

    /**
     * Formats the player's current hand and appends it to the player's action log. Synchronization
     * ensures the hand is not being modified by this player's turn while it is read.
     *
     * - Throws UncheckedIOException if the hand record cannot be written
     */
    public synchronized void writeCurrentHand()
    {
        String line = "Player "+id+" hand: ";
        for (int i = 0; i<hand.size(); i++)
        {
            line+=hand.get(i).getValue()+" ";
        }
        writeAction(line);
    }

    /**
     * Appends a game-result message to this player's action log, indicating either that this
     * player won or that another player was announced as the winner.
     *
     * - Winner ID: ID of the player declared as the winner
     * - Throws UncheckedIOException if the result message cannot be written
     */
    public void reportWinner(int winnerId)
    {
        if (id == winnerId)
        {
            writeAction("Player " + id + " wins");
        }
        else
        {
            writeAction("player " + winnerId + " has informed player " + id + " that player " + winnerId + " has won");
        }
    }

    /**
     * Removes player log files left by an earlier game. The current game's players create fresh
     * files after this method returns, so the output directory contains one player log per player
     * rather than retaining files for players from a larger previous game. Other files in the
     * output directory are left untouched.
     *
     * - Throws UncheckedIOException if the output directory cannot be listed or a player log
     *   cannot be deleted
     */
    public static void resetOutputFiles()
    {
        Path outputDirectory = getOutputDirectory();
        try
        {
            Files.createDirectories(outputDirectory);
            try (Stream<Path> outputFiles = Files.list(outputDirectory))
            {
                for (Path outputFile : outputFiles.toList())
                {
                    String fileName = outputFile.getFileName().toString();
                    if (fileName.matches("player_[0-9]+\\.txt") && Files.isRegularFile(outputFile))
                    {
                        Files.delete(outputFile);
                    }
                }
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Unable to reset player output files in "
                    + outputDirectory, e);
        }
    }

    /**
     * Appends one action line to this player's output file. The file is expected to have been
     * initialized by the constructor.
     *
     * - Action: text describing the action to record, without a trailing line separator
     * - Throws UncheckedIOException if the action cannot be appended to the file
     */
    private void writeAction(String action)
    {
        writeOutput(action + System.lineSeparator(), StandardOpenOption.APPEND);
    }

    /**
     * Creates the player-output directory if needed and creates or clears the player's log file
     * for a new game.
     *
     * - Throws UncheckedIOException if the directory or output file cannot be created
     */
    private void initializeOutputFile()
    {
        Path outputDirectory = getOutputDirectory();
        createOutputDirectory(outputDirectory);
        writeOutput("", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static Path getOutputDirectory()
    {
        return Path.of("player_outputs");
    }

    private Path getOutputFile()
    {
        return getOutputDirectory().resolve("player_" + id + ".txt");
    }

    /**
     * Writes content to this player's output file using the supplied file-open options.
     *
     * - Contents: text to write
     * - Options: control whether the file is created, appended to, or truncated
     * - Throws UncheckedIOException if the output cannot be written
     */
    private void writeOutput(String contents, StandardOpenOption... options)
    {
        Path outputFile = getOutputFile();
        try
        {
            Files.writeString(outputFile, contents, options);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Unable to write player output file " + outputFile, e);
        }
    }

    /**
     * Creates the output directory if it does not already exist.
     *
     * - Output directory: directory to create
     * - Throws UncheckedIOException if the directory cannot be created
     */
    private static void createOutputDirectory(Path outputDirectory)
    {
        try
        {
            Files.createDirectories(outputDirectory);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Unable to create output directory " + outputDirectory, e);
        }
    }

}

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

public class Player 
{
    private int id;
    private ArrayList<Card> hand = new ArrayList<>();
    private Deck drawDeck;
    private Deck discardDeck;

    public Player(int i)
    {
        id = i;
        initializeOutputFile();
    }

    /*
    Assigns the draw and discard decks to the player
    Can't be done in the constructor since the decks are created after the players
     */
    public void assignDecks(Deck draw, Deck discard)
    {
        drawDeck = draw;
        discardDeck = discard;
    }

    /*
    removes a card from the drawDeck and adds it to the hand
    if the hand is somehow full or the drawDeck is somehow empty, cry
     */
    public void draw()
    {
        Card card = drawDeck.remove();
        hand.add(card);
        writeAction("player "+id+" draws a " + card.getValue() + " from deck " + drawDeck.getId());
    }

    /*
    adds a card to the discardDeck and removes it from the hand
    if the pos is somehow invalid, cry
     */
    public void discard(int pos)
    {
        Card card = hand.get(pos);
        discardDeck.add(card);
        hand.remove(pos);
        writeAction("player "+id+" discards a " + card.getValue() + " to deck " + discardDeck.getId());
    }

    public void writeCurrentHand()
    {
        String line = "Player "+id+" hand: ";
        for (int i = 0; i<hand.size(); i++)
        {
            line+=hand.get(i).getValue()+" ";
        }
        writeAction(line);
    }

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


    /*
    writes the draw and discard actions to the player's output file
     */
    private void writeAction(String action)
    {
        Path outputFile = getOutputFile();
        String line = action + System.lineSeparator();

        try
        {
            Files.writeString(outputFile, line, StandardOpenOption.APPEND);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Unable to write player action to " + outputFile, e);
        }
    }

    /*
    if the output file or directory do not exist in the current directory, this function will create them
     */
    private void initializeOutputFile()
    {
        Path outputDirectory = Path.of("player_outputs");
        Path outputFile = getOutputFile();

        try
        {
            Files.createDirectories(outputDirectory);
            Files.writeString(outputFile, "", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Unable to initialize player output file " + outputFile, e);
        }
    }

    /*
    returns the path of the player's associated output file
     */
    private Path getOutputFile()
    {
        return Path.of("player_outputs").resolve("player_" + id + ".txt");
    }

    /*
    adds a card to the player's hand
    THIS IS NOT DRAW
    for initialization and testing
     */
    public void addToHand(Card c)
    {
        if (hand.size()>=4)
        {
            System.out.println("Hand is full");
        }
        else
        {
            hand.add(c);
        }
    }

    /*
    deletes a card in a given position
    THIS IS NOT DISCARD
    for testing
     */
    public void deleteCard(int pos)
    {
        hand.remove(pos);
    }

    /*
    prints a list of the player's cards in hand
     */
    public void printHand()
    {
        String result = "Player "+id+" hand: ";
        for (int i = 0; i<hand.size(); i++)
        {
            Card card = hand.get(i);
            result+=""+card.getValue()+", ";
        }
        System.out.println(result);
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
    public Card[] getHand() 
    {
        Card[] result = new Card[4];
        for (int i = 0; i<result.length; i++)
        {
            result[i] = hand.get(i);
        }
        return result;
    }
}

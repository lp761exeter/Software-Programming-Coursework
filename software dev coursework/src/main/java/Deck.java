import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Deck
{
    private int id;
    private Queue<Card> cards = new LinkedList<>();

    public Deck(int i)
    {
        id = i;
    }

    public int getId()
    {
        return id;
    }

    public void add(Card card)
    {
        cards.add(card);
    }

    public Card remove()
    {
        return cards.remove();
    }

    /*
    converts the queue into an arraylist and returns it
     */
    public List<Card> getCards()
    {
        return new ArrayList<>(cards);
    }

    /*
    writes the final contents of the deck to a dedicated text file on game end
    if the file or directory does not exist, it will be created
     */
    public void writeFinalContents()
    {
        Path outputDirectory = Path.of("deck_outputs");
        Path outputFile = outputDirectory.resolve("deck_" + id + ".txt");
        String contents = "deck" + id + " contents: ";
        List<Card> finalCards = getCards();
        for (int i = 0; i < finalCards.size(); i++)
        {
            contents += finalCards.get(i).getValue()+" ";
        }
        contents += System.lineSeparator();

        try
        {
            Files.createDirectories(outputDirectory);
            Files.writeString(outputFile, contents, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Unable to write final contents for deck " + id, e);
        }
    }
}

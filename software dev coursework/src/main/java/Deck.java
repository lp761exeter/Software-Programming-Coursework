import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Stream;

public class Deck
{
    private int id;
    private Queue<Card> cards = new LinkedList<>();

    /**
     * Creates an empty deck with the specified ID.
     *
     * - ID: unique identifier used to distinguish this deck in game output
     */
    public Deck(int i)
    {
        id = i;
    }

    public int getId()
    {
        return id;
    }

    public synchronized List<Card> getCards()
    {
        return new ArrayList<>(cards);
    }

    /**
     * Adds a card to the end of this deck's queue. Synchronization protects the queue when
     * multiple player turns access this deck concurrently.
     *
     * - Card: card to place at the back of the deck
     */
    public synchronized void add(Card card)
    {
        cards.add(card);
    }

    /**
     * Removes and returns the card at the front of this deck's queue. Synchronization protects the
     * queue when multiple player turns access this deck concurrently.
     *
     * - Returns: the card previously at the front of the deck
     * - Throws NoSuchElementException if the deck is empty
     */
    public synchronized Card remove()
    {
        return cards.remove();
    }

    /**
     * Removes deck output files left by an earlier game while leaving unrelated files untouched.
     *
     * - Throws UncheckedIOException if the output directory cannot be listed or a deck output
     *   file cannot be deleted
     */
    public static void resetOutputFiles()
    {
        Path outputDirectory = getOutputDirectory();
        try
        {
            Files.createDirectories(outputDirectory);
            Stream<Path> outputFiles = Files.list(outputDirectory);
            for (Path outputFile : outputFiles.toList())
            {
                String fileName = outputFile.getFileName().toString();
                if (fileName.matches("deck_[0-9]+\\.txt") && Files.isRegularFile(outputFile))
                {
                    Files.delete(outputFile);
                }
            }
            outputFiles.close();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Unable to reset deck output files in "+ outputDirectory, e);
        }
    }

    /**
     * Writes a snapshot of this deck's current card values to its dedicated file, replacing any
     * previous contents. The output directory and file are created if necessary.
     *
     * - Throws UncheckedIOException if the directory or final-contents file cannot be written
     */
    public void writeFinalContents()
    {
        String contents = "deck" + id + " contents: ";
        List<Card> finalCards = getCards();
        for (int i = 0; i < finalCards.size(); i++)
        {
            contents += finalCards.get(i).getValue()+" ";
        }
        contents += System.lineSeparator();

        Path outputDirectory = getOutputDirectory();
        createOutputDirectory(outputDirectory);
        writeOutput(contents, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static Path getOutputDirectory()
    {
        return Path.of("deck_outputs");
    }

    private Path getOutputFile()
    {
        return getOutputDirectory().resolve("deck_" + id + ".txt");
    }

    /**
     * Writes content to this deck's output file using the supplied file-open options.
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
            throw new UncheckedIOException("Unable to write deck output file " + outputFile, e);
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

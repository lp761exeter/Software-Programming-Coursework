import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

class DeckTest
{
    @Test
    void returnsIdAndStartsEmpty()
    {
        Deck deck = new Deck(12);

        assertEquals(12, deck.getId());
        assertTrue(deck.getCards().isEmpty());
    }

    @Test
    void addsAndRemovesCardsInQueueOrder()
    {
        Deck deck = new Deck(1);
        Card first = new Card(4);
        Card second = new Card(9);

        deck.add(first);
        deck.add(second);

        assertSame(first, deck.remove());
        assertSame(second, deck.remove());
        assertThrows(NoSuchElementException.class, deck::remove);
    }

    @Test
    void getCardsReturnsAnIndependentSnapshot()
    {
        Deck deck = new Deck(2);
        Card card = new Card(3);
        deck.add(card);

        List<Card> snapshot = deck.getCards();
        snapshot.clear();

        assertEquals(1, deck.getCards().size());
        assertSame(card, deck.getCards().get(0));
        assertNotSame(snapshot, deck.getCards());
    }

    @Test
    void writesFinalContentsReplacingPreviousFile() throws Exception
    {
        Deck deck = new Deck(7);
        deck.add(new Card(2));
        deck.add(new Card(5));
        Path outputFile = Path.of("deck_outputs", "deck_7.txt");
        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, "old contents");

        deck.writeFinalContents();

        assertEquals("deck7 contents: 2 5 " + System.lineSeparator(), Files.readString(outputFile));
    }

    @Test
    void resetOutputFilesRemovesOnlyDeckOutputFiles() throws Exception
    {
        Path outputDirectory = Path.of("deck_outputs");
        Files.createDirectories(outputDirectory);
        Path deckFile = outputDirectory.resolve("deck_21.txt");
        Path unrelatedFile = outputDirectory.resolve("notes.txt");
        Path nonFileDeckEntry = outputDirectory.resolve("deck_22.txt");
        Files.writeString(deckFile, "generated");
        Files.writeString(unrelatedFile, "keep");
        Files.createDirectories(nonFileDeckEntry);

        Deck.resetOutputFiles();

        assertFalse(Files.exists(deckFile));
        assertEquals("keep", Files.readString(unrelatedFile));
        assertTrue(Files.isDirectory(nonFileDeckEntry));
    }
}

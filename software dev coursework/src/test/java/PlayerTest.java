import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PlayerTest
{
    private final PrintStream originalOut = System.out;
    private final ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();

    @AfterEach
    void restoreOutput()
    {
        System.setOut(originalOut);
    }

    // Checks that construction preserves the player ID and creates its output log.
    @Test
    void constructorAndGetIdPreserveId()
    {
        Player player = new Player(31);

        assertEquals(31, player.getId());
        assertTrue(Files.exists(Path.of("player_outputs", "player_31.txt")));
    }

    // Checks hand ordering, snapshot isolation, card removal, and rejection of an invalid index.
    @Test
    void handOperationsMaintainCardsAndReturnASnapshot()
    {
        Player player = new Player(32);
        Card[] cards = {new Card(1), new Card(2), new Card(3), new Card(4)};
        for (Card card : cards)
        {
            player.addToHand(card);
        }

        Card[] hand = player.getHand();
        assertArrayEquals(cards, hand);
        hand[0] = new Card(99);
        assertSame(cards[0], player.getHand()[0]);

        player.deleteCard(1);
        player.addToHand(cards[1]);
        assertArrayEquals(new Card[] {cards[0], cards[2], cards[3], cards[1]}, player.getHand());
        assertThrows(IndexOutOfBoundsException.class, () -> player.deleteCard(4));
    }

    // Checks that drawing takes the top card, logs it, and fails when the draw deck is empty.
    @Test
    void drawMovesTopCardIntoHandAndLogsAction() throws Exception
    {
        Player player = new Player(33);
        player.addToHand(new Card(1));
        player.addToHand(new Card(2));
        player.addToHand(new Card(3));
        Deck draw = new Deck(1);
        Deck discard = new Deck(2);
        Card drawn = new Card(8);
        draw.add(drawn);
        player.assignDecks(draw, discard);

        player.draw();

        assertSame(drawn, player.getHand()[3]);
        assertTrue(draw.getCards().isEmpty());
        assertTrue(Files.readString(Path.of("player_outputs", "player_33.txt"))
                .contains("player 33 draws a 8 from deck 1"));
        assertThrows(NoSuchElementException.class, player::draw);
    }

    // Checks that a turn discards the first unwanted card, draws one card, and retains four cards.
    @Test
    void runDiscardsFirstUnwantedCardDrawsAndKeepsFourCards()
    {
        Player player = new Player(4);
        Card unwanted = new Card(7);
        Card[] matching = {new Card(4), new Card(4), new Card(4)};
        for (Card card : new Card[] {unwanted, matching[0], matching[1], matching[2]})
        {
            player.addToHand(card);
        }
        Deck draw = new Deck(2);
        Deck discard = new Deck(1);
        Card drawn = new Card(9);
        draw.add(drawn);
        player.assignDecks(draw, discard);

        player.run();

        assertArrayEquals(new Card[] {matching[0], matching[1], matching[2], drawn}, player.getHand());
        assertSame(unwanted, discard.remove());
        assertTrue(draw.getCards().isEmpty());
        String log = readPlayerLog(4);
        assertTrue(log.contains("player 4 discards a 7 to deck 1"));
        assertTrue(log.contains("player 4 draws a 9 from deck 2"));
    }

    // Checks that a winning hand cannot take a turn and neither deck is changed.
    @Test
    void runRejectsAHandWithNoDiscardCandidate()
    {
        Player player = new Player(5);
        for (int i = 0; i < 4; i++)
        {
            player.addToHand(new Card(5));
        }
        Deck draw = new Deck(1);
        Deck discard = new Deck(2);
        draw.add(new Card(8));
        player.assignDecks(draw, discard);

        assertThrows(IndexOutOfBoundsException.class, player::run);
        assertEquals(1, draw.getCards().size());
        assertTrue(discard.getCards().isEmpty());
    }

    // Checks that discard moves the selected card and invalid positions are rejected.
    @Test
    void discardMovesSelectedCardAndRejectsInvalidPosition()
    {
        Player player = new Player(34);
        Card discarded = new Card(6);
        player.addToHand(discarded);
        Deck draw = new Deck(1);
        Deck discard = new Deck(2);
        player.assignDecks(draw, discard);

        player.discard(0);

        assertThrows(IndexOutOfBoundsException.class, player::getHand);
        assertSame(discarded, discard.remove());
        assertThrows(IndexOutOfBoundsException.class, () -> player.discard(0));
    }

    // Checks that the hand is printed with the player's ID and card values.
    @Test
    void printHandWritesTheFormattedHandToStandardOutput()
    {
        Player player = new Player(35);
        player.addToHand(new Card(2));
        player.addToHand(new Card(3));
        System.setOut(new PrintStream(capturedOut));

        player.printHand();

        assertTrue(capturedOut.toString().contains("Player 35 hand: 2, 3, "));
    }

    // Checks that the current hand and winner notifications are appended to player logs.
    @Test
    void writeCurrentHandAndReportWinnerAppendLogEntries()
    {
        Player winner = new Player(36);
        for (int value : new int[] {6, 6, 6, 6})
        {
            winner.addToHand(new Card(value));
        }
        Player other = new Player(37);

        winner.writeCurrentHand();
        winner.reportWinner(36);
        other.reportWinner(36);

        assertTrue(readPlayerLog(36).contains("Player 36 hand: 6 6 6 6 "));
        assertTrue(readPlayerLog(36).contains("Player 36 wins"));
        assertTrue(readPlayerLog(37).contains("player 36 has informed player 37"));
    }

    // Checks that reset removes player log files but preserves unrelated files and directories.
    @Test
    void resetOutputFilesRemovesOnlyPlayerLogs() throws Exception
    {
        Path outputDirectory = Path.of("player_outputs");
        Files.createDirectories(outputDirectory);
        Path playerFile = outputDirectory.resolve("player_99.txt");
        Path unrelatedFile = outputDirectory.resolve("notes.txt");
        Path nonFilePlayerEntry = outputDirectory.resolve("player_100.txt");
        Files.writeString(playerFile, "generated");
        Files.writeString(unrelatedFile, "keep");
        Files.createDirectories(nonFilePlayerEntry);

        Player.resetOutputFiles();

        assertFalse(Files.exists(playerFile));
        assertEquals("keep", Files.readString(unrelatedFile));
        assertTrue(Files.isDirectory(nonFilePlayerEntry));
    }

    private static String readPlayerLog(int playerId)
    {
        try
        {
            return Files.readString(Path.of("player_outputs", "player_" + playerId + ".txt"));
        }
        catch (java.io.IOException e)
        {
            throw new AssertionError(e);
        }
    }
}

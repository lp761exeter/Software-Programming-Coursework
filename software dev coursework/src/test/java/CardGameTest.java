import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CardGameTest
{
    private Scanner originalScanner;
    private PrintStream originalOut;
    private ByteArrayOutputStream capturedOut;

    @BeforeEach
    void setUp()
    {
        originalScanner = CardGame.scan;
        originalOut = System.out;
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut));
        CardGame.scan = new Scanner(InputStream.nullInputStream());
        CardGame.nextId = 1;
        CardGame.pack = new LinkedList<>();
        CardGame.decks = new ArrayList<>();
        CardGame.playersNum = 0;
        CardGame.players = new Player[0];
        CardGame.tick = 0;
        CardGame.tickLimit = 10000;
    }

    @AfterEach
    void tearDown()
    {
        CardGame.scan = originalScanner;
        System.setOut(originalOut);
    }

    @Test
    void checkForWinnersReturnsNoWinnerOrHighestWinningPlayerId()
    {
        Player first = playerWithHand(1, 1, 1, 2, 1);
        Player second = playerWithHand(2, 2, 2, 3, 2);
        CardGame.players = new Player[] {first, second};

        assertEquals(-1, CardGame.checkForWinners());

        first = playerWithHand(1, 1, 1, 1, 1);
        second = playerWithHand(2, 2, 2, 2, 2);
        CardGame.players = new Player[] {first, second};
        assertEquals(2, CardGame.checkForWinners());
    }

    @Test
    void tickRunsEachPlayerThenIncrementsAndPrintsState()
    {
        Player player = playerWithHand(1, 8, 1, 1, 1);
        Deck draw = new Deck(1);
        Deck discard = new Deck(2);
        draw.add(new Card(1));
        player.assignDecks(draw, discard);
        CardGame.players = new Player[] {player};

        CardGame.tick();

        assertEquals(1, CardGame.tick);
        assertEquals(8, discard.getCards().get(0).getValue());
        assertTrue(capturedOut.toString().contains("Current tick: 1"));
    }

    @Test
    void tickPropagatesWorkerFailuresWithoutIncrementingTick()
    {
        Player player = playerWithHand(1, 8, 1, 1, 1);
        player.assignDecks(new Deck(1), new Deck(2));
        CardGame.players = new Player[] {player};

        assertThrows(java.util.NoSuchElementException.class, CardGame::tick);

        assertEquals(0, CardGame.tick);
    }

    @Test
    void printMethodsWriteAllDecksHandsAndGameState()
    {
        Player player = playerWithHand(3, 3, 4, 3, 3);
        Deck deck = new Deck(5);
        deck.add(new Card(7));
        CardGame.players = new Player[] {player};
        CardGame.decks = new ArrayList<>(List.of(deck));

        CardGame.printAllDecks();
        CardGame.printAllHands();
        CardGame.printGameState();

        String output = capturedOut.toString();
        assertTrue(output.contains("Deck 5: 7, "));
        assertTrue(output.contains("Player 3 hand: 3, 4, 3, 3, "));
        assertTrue(output.contains("Current tick: 0"));
    }

    @Test
    void assignDecksToPlayersUsesCircularDeckArrangement()
    {
        CardGame.players = new Player[] {new Player(1), new Player(2), new Player(3)};
        CardGame.decks = new ArrayList<>(List.of(new Deck(1), new Deck(2), new Deck(3)));
        CardGame.decks.get(0).add(new Card(10));
        CardGame.decks.get(1).add(new Card(20));
        CardGame.decks.get(2).add(new Card(30));

        CardGame.assignDecksToPlayers();
        CardGame.players[0].addToHand(new Card(9));
        CardGame.players[0].addToHand(new Card(1));
        CardGame.players[0].addToHand(new Card(1));
        CardGame.players[0].addToHand(new Card(1));
        CardGame.players[0].run();
        CardGame.players[1].addToHand(new Card(8));
        CardGame.players[1].addToHand(new Card(2));
        CardGame.players[1].addToHand(new Card(2));
        CardGame.players[1].addToHand(new Card(2));
        CardGame.players[1].run();
        CardGame.players[2].addToHand(new Card(7));
        CardGame.players[2].addToHand(new Card(3));
        CardGame.players[2].addToHand(new Card(3));
        CardGame.players[2].addToHand(new Card(3));
        CardGame.players[2].run();

        assertEquals(List.of(9), deckValues(CardGame.decks.get(0)));
        assertEquals(List.of(8), deckValues(CardGame.decks.get(1)));
        assertEquals(List.of(7), deckValues(CardGame.decks.get(2)));
    }

    @Test
    void dealsFourCardsToEachPlayerInRoundRobinOrder()
    {
        CardGame.players = new Player[] {new Player(1), new Player(2)};
        CardGame.pack = cards(1, 2, 3, 4, 5, 6, 7, 8);

        CardGame.dealCardsToPlayers();

        assertArrayEquals(values(1, 3, 5, 7), handValues(CardGame.players[0]));
        assertArrayEquals(values(2, 4, 6, 8), handValues(CardGame.players[1]));
        assertTrue(CardGame.pack.isEmpty());
    }

    @Test
    void dealsRemainingPackAcrossNewDecksRoundRobin()
    {
        CardGame.playersNum = 2;
        CardGame.pack = cards(10, 11, 12, 13, 14);

        CardGame.dealCardsToDecks();

        assertEquals(List.of(1, 2), CardGame.decks.stream().map(Deck::getId).collect(Collectors.toList()));
        assertEquals(List.of(10, 12, 14), deckValues(CardGame.decks.get(0)));
        assertEquals(List.of(11, 13), deckValues(CardGame.decks.get(1)));
        assertTrue(CardGame.pack.isEmpty());
    }

    @Test
    void makePlayersAssignsIncreasingIds()
    {
        Player[] players = CardGame.makePlayers(3);

        assertArrayEquals(new int[] {1, 2, 3},
                Arrays.stream(players).mapToInt(Player::getId).toArray());
        assertEquals(4, CardGame.nextId);
    }

    @Test
    void shuffleQueueReturnsSameCardsWithoutChangingInput()
    {
        Queue<Card> original = cards(1, 2, 3, 4);
        List<Card> originalOrder = new ArrayList<>(original);

        Queue<Card> shuffled = CardGame.shuffleQueue(original);

        assertEquals(originalOrder, new ArrayList<>(original));
        assertEquals(original.size(), shuffled.size());
        for (Card card : original)
        {
            assertTrue(shuffled.contains(card));
        }
        assertFalse(shuffled == original);
    }

    @Test
    void loadPackAcceptsOptionalExtensionAndSkipsBlankLines() throws Exception
    {
        Files.createDirectories(Path.of("Decks"));
        Files.writeString(Path.of("Decks", "test-pack.txt"), " 4 \n\n-2\n9\n");

        assertEquals(List.of(4, -2, 9), cardValues(CardGame.loadPack("test-pack")));
        assertEquals(List.of(4, -2, 9), cardValues(CardGame.loadPack("test-pack.txt")));
    }

    @Test
    void loadPackReportsMissingFilesAndRejectsInvalidValues()
    {
        assertThrows(FileNotFoundException.class, () -> CardGame.loadPack("missing-pack.txt"));

        writePack("invalid-pack.txt", "not an integer\n");
        assertThrows(NumberFormatException.class, () -> CardGame.loadPack("invalid-pack.txt"));
    }

    @Test
    void getPackFromFileRetriesMissingName()
    {
        writePack("interactive-pack.txt", "1\n2\n");
        CardGame.scan = new Scanner("missing-interactive-pack\ninteractive-pack\n");

        Queue<Card> result = CardGame.getPackFromFile();

        assertEquals(List.of(1, 2), cardValues(result));
        assertTrue(capturedOut.toString().contains("Invalid input or file not found"));
    }

    @Test
    void getNumPlayersInputRetriesNonNumericAndNonPositiveValues()
    {
        CardGame.scan = new Scanner("word\n0\n3\n");

        assertEquals(3, CardGame.getNumPlayersInput());
        assertTrue(capturedOut.toString().contains("Input must be a valid number"));
        assertTrue(capturedOut.toString().contains("Number of players cannot be less than 1"));
    }

    @Test
    void askToShuffleAcceptsCaseInsensitiveYesAndRetriesInvalidInput()
    {
        CardGame.scan = new Scanner("maybe\nYES\n");

        assertTrue(CardGame.askToShuffle());
        assertTrue(capturedOut.toString().contains("Invalid input"));
    }

    @Test
    void askToShuffleAcceptsNo()
    {
        CardGame.scan = new Scanner("n\n");

        assertFalse(CardGame.askToShuffle());
    }

    @Test
    void mainCompletesAWinningOnePlayerGame() throws Exception
    {
        writePack("winning-pack.txt", "1\n1\n1\n1\n8\n");
        CardGame.scan = new Scanner("1\nwinning-pack\nn\n\n");

        CardGame.main(new String[0]);

        assertTrue(capturedOut.toString().contains("The game has been won by Player 1"));
        assertTrue(Files.readString(Path.of("player_outputs", "player_1.txt")).contains("Player 1 wins"));
        assertEquals("deck1 contents: 8 " + System.lineSeparator(),
                Files.readString(Path.of("deck_outputs", "deck_1.txt")));
    }

    private static Player playerWithHand(int id, int... values)
    {
        Player player = new Player(id);
        fillHand(player, values);
        return player;
    }

    private static void fillHand(Player player, int... values)
    {
        for (int i = 0; i < 4; i++)
        {
            player.addToHand(new Card(values[i]));
        }
    }

    private static Queue<Card> cards(int... values)
    {
        Queue<Card> result = new LinkedList<>();
        for (int value : values)
        {
            result.add(new Card(value));
        }
        return result;
    }

    private static int[] handValues(Player player)
    {
        return Arrays.stream(player.getHand()).mapToInt(Card::getValue).toArray();
    }

    private static List<Integer> cardValues(Queue<Card> cards)
    {
        return cards.stream().map(Card::getValue).collect(Collectors.toList());
    }

    private static List<Integer> deckValues(Deck deck)
    {
        return deck.getCards().stream().map(Card::getValue).collect(Collectors.toList());
    }

    private static int[] values(int... values)
    {
        return values;
    }

    private static void writePack(String fileName, String contents)
    {
        try
        {
            Files.createDirectories(Path.of("Decks"));
            Files.writeString(Path.of("Decks", fileName), contents);
        }
        catch (java.io.IOException e)
        {
            throw new AssertionError(e);
        }
    }

}

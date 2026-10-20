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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    // resets the scanner input and output streams to avoid test collisions
    @AfterEach
    void tearDown()
    {
        CardGame.scan = originalScanner;
        System.setOut(originalOut);
    }

    // Checks that no winner is reported for nonmatching hands and the highest-ID winner is selected.
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

    // Checks that a tick runs the player's turn, increments the tick, and prints the new state.
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

    // Checks that worker failures propagate and do not advance the game tick.
    @Test
    void tickPropagatesWorkerFailuresWithoutIncrementingTick()
    {
        Player player = playerWithHand(1, 8, 1, 1, 1);
        player.assignDecks(new Deck(1), new Deck(2));
        CardGame.players = new Player[] {player};

        assertThrows(java.util.NoSuchElementException.class, CardGame::tick);

        assertEquals(0, CardGame.tick);
    }

    // Checks that concurrent turns retain four cards per hand and conserve all cards.
    @Test
    void concurrentTickPreservesFourCardHandsAndAllCards()
    {
        Player first = playerWithHand(1, 1, 1, 1, 7);
        Player second = playerWithHand(2, 2, 2, 2, 8);
        Deck firstDeck = new Deck(1);
        Deck secondDeck = new Deck(2);
        firstDeck.add(new Card(11));
        secondDeck.add(new Card(22));
        first.assignDecks(secondDeck, firstDeck);
        second.assignDecks(firstDeck, secondDeck);
        CardGame.players = new Player[] {first, second};
        CardGame.decks = new ArrayList<>(List.of(firstDeck, secondDeck));
        List<Integer> originalCards = List.of(1, 1, 1, 7, 2, 2, 2, 8, 11, 22);

        CardGame.tick();

        List<Integer> cardsAfterTick = new ArrayList<>();
        for (Player player : CardGame.players)
        {
            assertEquals(4, player.getHand().length);
            cardsAfterTick.addAll(Arrays.stream(player.getHand())
                    .map(Card::getValue).collect(Collectors.toList()));
        }
        cardsAfterTick.addAll(deckValues(firstDeck));
        cardsAfterTick.addAll(deckValues(secondDeck));

        assertEquals(originalCards.stream().sorted().collect(Collectors.toList()),
                cardsAfterTick.stream().sorted().collect(Collectors.toList()));
        assertEquals(1, CardGame.tick);
    }

    // Checks that an empty draw deck leaves the failed player's hand and decks unchanged.
    @Test
    void failedTurnWithEmptyDrawDeckLeavesHandAndDiscardDeckUnchanged()
    {
        Player player = playerWithHand(3, 9, 3, 3, 3);
        Card[] originalHand = player.getHand();
        Deck draw = new Deck(1);
        Deck discard = new Deck(2);
        Card existingDiscard = new Card(12);
        discard.add(existingDiscard);
        player.assignDecks(draw, discard);

        assertThrows(java.util.NoSuchElementException.class, player::run);

        assertArrayEquals(originalHand, player.getHand());
        assertEquals(List.of(existingDiscard), discard.getCards());
        assertTrue(draw.getCards().isEmpty());
    }

    // Checks that printing methods include each deck, hand, and the current tick.
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

    // Checks that players draw from and discard to the expected circularly assigned decks.
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

    // Checks that four cards are dealt to each player in round-robin order.
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

    // Checks that remaining pack cards are distributed across new decks in round-robin order.
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

    // Checks that created players receive sequential IDs and advance the ID counter.
    @Test
    void makePlayersAssignsIncreasingIds()
    {
        Player[] players = CardGame.makePlayers(3);

        assertArrayEquals(new int[] {1, 2, 3},
                Arrays.stream(players).mapToInt(Player::getId).toArray());
        assertEquals(4, CardGame.nextId);
    }

    // Checks that shuffling returns the same cards in a new queue without changing the input.
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

    // Checks optional filename extensions, whitespace trimming, and blank-line skipping.
    @Test
    void loadPackAcceptsOptionalExtensionAndSkipsBlankLines() throws Exception
    {
        Files.createDirectories(Path.of("Decks"));
        Files.writeString(Path.of("Decks", "test-pack.txt"), " 4 \n\n-2\n9\n");

        assertEquals(List.of(4, -2, 9), cardValues(CardGame.loadPack("test-pack")));
        assertEquals(List.of(4, -2, 9), cardValues(CardGame.loadPack("test-pack.txt")));
    }

    // Checks that missing pack files and non-integer card values are rejected.
    @Test
    void loadPackReportsMissingFilesAndRejectsInvalidValues()
    {
        assertThrows(FileNotFoundException.class, () -> CardGame.loadPack("missing-pack.txt"));

        writePack("invalid-pack.txt", "not an integer\n");
        assertThrows(NumberFormatException.class, () -> CardGame.loadPack("invalid-pack.txt"));
    }

    // Checks that an unknown filename is reported and prompts for another pack name.
    @Test
    void getPackFromFileRetriesMissingName()
    {
        writePack("interactive-pack.txt", "1\n2\n");
        CardGame.scan = new Scanner("missing-interactive-pack\ninteractive-pack\n");

        Queue<Card> result = CardGame.getPackFromFile();

        assertEquals(List.of(1, 2), cardValues(result));
        assertTrue(capturedOut.toString().contains("Invalid input or file not found"));
    }

    // Checks that nonnumeric and nonpositive player counts are rejected before a valid count.
    @Test
    void getNumPlayersInputRetriesNonNumericAndNonPositiveValues()
    {
        CardGame.scan = new Scanner("word\n0\n3\n");

        assertEquals(3, CardGame.getNumPlayersInput());
        assertTrue(capturedOut.toString().contains("Input must be a valid number"));
        assertTrue(capturedOut.toString().contains("Number of players cannot be less than 1"));
    }

    // Checks that shuffle input is case-insensitive and invalid answers are retried.
    @Test
    void askToShuffleAcceptsCaseInsensitiveYesAndRetriesInvalidInput()
    {
        CardGame.scan = new Scanner("maybe\nYES\n");

        assertTrue(CardGame.askToShuffle());
        assertTrue(capturedOut.toString().contains("Invalid input"));
    }

    // Checks that a negative shuffle response returns false.
    @Test
    void askToShuffleAcceptsNo()
    {
        CardGame.scan = new Scanner("n\n");

        assertFalse(CardGame.askToShuffle());
    }

    // Checks immediate-win detection, winner reporting, and generation of final deck output.
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

    // Checks that reaching the tick limit still writes the final deck contents.
    @Test
    void mainWritesFinalDeckContentsWhenTickLimitIsReached() throws Exception
    {
        writePack("tick-limit-pack.txt", "1\n2\n3\n4\n5\n");
        CardGame.scan = new Scanner("1\ntick-limit-pack\nn\n\n");
        CardGame.tickLimit = 1;

        CardGame.main(new String[0]);

        assertTrue(capturedOut.toString().contains("The game stopped after reaching the tick limit of 1."));
        assertEquals("deck1 contents: 2 " + System.lineSeparator(),
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

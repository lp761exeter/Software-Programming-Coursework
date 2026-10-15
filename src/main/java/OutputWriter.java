import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Queue;

public class OutputWriter {

    private final int playerId;
    private final PrintWriter writer;

    public OutputWriter(int playerId) throws IOException {
        this.playerId = playerId;
        this.writer = new PrintWriter(new FileWriter("player_" + playerId + "_output.txt"));
    }

    public void writeInitialHand(int[] hand) {
        writer.println("player " + playerId + " initial hand: " + format(hand));
        writer.flush();
    }

    public void writeDraw(int cardValue, int deckId) {
        writer.println("player " + playerId + " draws a " + cardValue + " from deck " + deckId);
        writer.flush();
    }

    public void writeDiscard(int cardValue, int deckId) {
        writer.println("player " + playerId + " discards a " + cardValue + " to deck " + deckId);
        writer.flush();
    }

    public void writeCurrentHand(int[] hand) {
        writer.println("player " + playerId + " current hand is " + format(hand));
        writer.flush();
    }

    public void writeWin(int[] finalHand) {
        writer.println("player " + playerId + " wins");
        writer.println("player " + playerId + " exits");
        writer.println("player " + playerId + " final hand: " + format(finalHand));
        writer.flush();
    }

    public void writeLose(int winnerId, int[] finalHand) {
        writer.println("player " + playerId + " has informed player " + winnerId
                + " that player " + winnerId + " has won");
        writer.println("player " + playerId + " exits");
        writer.println("player " + playerId + " hand: " + format(finalHand));
        writer.flush();
    }

    public void close() {
        writer.close();
    }
    public static void writeDeck(int deckId, Queue<Card> deck) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("deck ").append(deckId).append(" contents:");
        for (Card c : deck) {
            sb.append(" ").append(c.getValue());
        }
        try (PrintWriter pw = new PrintWriter(new FileWriter("deck_" + deckId + "_output.txt"))) {
            pw.println(sb.toString());
        }
    }

    private static String format(int[] hand) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hand.length; i++) {
            if (i > 0) sb.append(" ");
            sb.append(hand[i]);
        }
        return sb.toString();
    }
}
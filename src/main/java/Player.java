import java.util.ArrayList;
import java.util.Queue;

public class Player 
{
    private int id;
    ArrayList<Card> hand = new ArrayList<>();
    Queue<Card> drawDeck;
    Queue<Card> discardDeck;

    public Player(int i)
    {
        id = i;
    }

    

    /*
    Assigns the draw and discard decks to the player
    Can't be done in the constructor since the decks are created after the players
     */
    public void assignDecks(Queue<Card> draw, Queue<Card> discard)
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
        hand.add(drawDeck.remove());
    }

    /*
    adds a card to the discardDeck and removes it from the hand
    if the pos is somehow invalid, cry
     */
    public void discard(int pos)
    {
        discardDeck.add(hand.get(pos));
        hand.remove(pos);
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
    public Queue<Card> getDrawDeck() {
        return drawDeck;
    }

    public Queue<Card> getDiscardDeck() {
        return discardDeck;
        
    }
}



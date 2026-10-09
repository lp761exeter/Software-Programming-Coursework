import java.util.ArrayList;

public class Player 
{
    private int id;
    ArrayList<Card> hand = new ArrayList<>();

    public Player(int i)
    {
        id = i;
    }

    // returns an array of the hand object 
    public Card[] getHand() 
    {
        Card[] result = new Card[4];
        for (int i = 0; i<result.length; i++)
        {
            result[i] = hand.get(i);
        }
        return result;
    }

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

    public Card discard(int pos)
    {
        Card card = hand.get(pos);
        hand.remove(pos);
        return card;
        
    }

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
}

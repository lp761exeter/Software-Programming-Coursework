import java.util.ArrayList;

public class Player 
{
    ArrayList<Card> hand = new ArrayList<>();

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
            
        }

        hand.add(c);
    }
}

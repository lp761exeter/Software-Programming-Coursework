import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class CardTest
{
    // Checks that a card retains the value supplied to its constructor.
    @Test
    void storesAndReturnsItsValue()
    {
        Card card = new Card(17);

        assertEquals(17, card.getValue());
    }
}

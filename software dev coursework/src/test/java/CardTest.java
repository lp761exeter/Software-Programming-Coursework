import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class CardTest
{
    @Test
    void storesAndReturnsItsValue()
    {
        Card card = new Card(17);

        assertEquals(17, card.getValue());
    }
}

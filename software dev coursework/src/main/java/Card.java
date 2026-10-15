// not really sure why this is a necessary class
// the CA specification says it must exist and must be "thread safe"
// the only attribute of this class is its value....... why does it need to exist when i can just use an int

public class Card 
{
    private int value;
    
    public Card(int v)
    {
        value = v;
    }

    public int getValue()
    {
        return value;
    }
}
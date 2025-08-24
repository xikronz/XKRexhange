package Server;
import java.math.BigDecimal;

public class Price implements Comparable<Price>{
    /* Represents the IMMUTABLE Price of an asset with a fixed tick increment that implements Comparable class to be used in a Priority Queue
     * Default tickSize is 0.01 dollars or 1 cent
     * CLASS INVARIANT: Instances of this object MUST be a multiple of self.tickSize otherwise IllegalArgumentException
     */
    private BigDecimal tick = new BigDecimal("0.01");
    private final BigDecimal value;

    public Price (BigDecimal p) throws IllegalArgumentException{
        if (p.remainder(this.tick).compareTo(BigDecimal.ZERO)!= 0) throw new IllegalArgumentException("Price must be an increment if tick size");
        this.value = p;
    }

    public void setTick(BigDecimal tickSize){
        this.tick = tickSize;
    }

    public BigDecimal getValue(){
        return this.value;
    }

    /** Compares two price objects by their values (BigDecimals)
     * Requires o to be a Price object
     * @param o: the reference object with which to compare.
     * @return int < 0 if this.value < o.value; int = 0 if this.value == o.value; int > 0 if this.value > o.value;
     */
    @Override
    public int compareTo(Price o){
        return this.getValue().compareTo(o.getValue());
    }

    /** Checks whether the given object is equal to this Price.
     *  Two price objects are said to be equal if thier {@code value} BigDecimal fields are equal
     * @param o   the reference object with which to compare.
     * @return {@code true} if the object has the same class and the same value {@code false} otherwise
     */
    @Override
    public boolean equals(Object o){
        if (!this.getClass().equals(o.getClass())) return false;
        return this.value.equals(((Price) o).value);
    }

    /**
     * Simple HashCode override that uses Price.value (BigDecimal's) hash function
     * @return hashcode representing this Price object
     */
    @Override
    public int hashCode(){
        return this.value.hashCode();
    }
}

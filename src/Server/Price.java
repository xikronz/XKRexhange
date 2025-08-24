package Server;
import java.math.BigDecimal;

public class Price {
    /* Represents the Price of an asset with a fixed tick increment
     * Default tickSize is 0.01 dollars or 1 cent
     * CLASS INVARIANT: Instances of this object MUST be a multiple of self.tickSize otherwise
     */
    private BigDecimal tick = new BigDecimal("0.01");
    private BigDecimal value;
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
}

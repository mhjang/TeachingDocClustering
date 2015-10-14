package Classify.Baseline;

/**
 * Created by mhjang on 10/8/15.
 */
public class ACLRatio extends RandomRatio{
    public ACLRatio() {
        this.TABLE = 4.0;
        this.CODE = this.TABLE + 0.6;
        this.FORMULA = this.CODE + 5.0;
        this.MISC = this.FORMULA + 6.4;
        this.TEXT = 100.0;
    }
}


package Classify.Baseline;

/**
 * Created by mhjang on 10/8/15.
 */
public class SlideRatio extends RandomRatio {
    public SlideRatio() {
        this.TABLE = 1.4;
        this.CODE = this.TABLE + 14.8;
        this.FORMULA = this.CODE + 0.4;
        this.MISC = this.FORMULA + 9.8;
        this.TEXT = 100;
    }
}

package Classify.liblinear;

/**
 * Created by mhjang on 7/21/14.
 */

public class FragmentIndex {
    private final int start;
    private final int end;

    public FragmentIndex(int start_, int end_) {
        this.start = start_;
        this.end = end_;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }
}
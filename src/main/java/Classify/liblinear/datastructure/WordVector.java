package Classify.liblinear.datastructure;

/**
 * Created by mhjang on 9/26/15.
 */
public class WordVector {
    double[] v;
    public WordVector(double[] d) {
        this.v = d;
    }
    public WordVector() {
        v = new double[100];
    }
    public double doubleAt(int i) {
        if(v == null) return 0.0;
        else return v[i];
    }
    public void addVector(WordVector p) {
        for(int i=0; i<v.length; i++) {
            v[i] += p.doubleAt(i);
        }
    }
    public void print() {
        for(int i=0; i<v.length; i++) {
            System.out.print(v[i] + " ");
        }
        System.out.println();
    }
    public double[] getWordVector() {
        return v;
    }
}

package Classify.liblinear;

import Classify.liblinear.datastructure.WordVector;
import simple.io.myungha.SimpleFileReader;

import java.util.HashMap;

/**
 * Created by mhjang on 9/25/15.
 */
public class VectorLookUp {
    HashMap<String, WordVector> lookup = new HashMap<String, WordVector>();
    public VectorLookUp() {
        try {
            SimpleFileReader sr = new SimpleFileReader("/Users/mhjang/Documents/workspace/NontextCompDetection/pdfcorpus-utf8.txt.vec");
            while(sr.hasMoreLines()) {
                String line = sr.readLine();
                String[] tokens = line.split(" ");
                String word = tokens[0];
                double[] vector = new double[100];
                for(int i=0; i<tokens.length - 1; i++) {
                    vector[i] = Double.parseDouble(tokens[i+1]);
                }
                lookup.put(word.toLowerCase(), new WordVector(vector));
        //        System.out.println(word);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public WordVector getWordVector(String word) {
        if(!lookup.containsKey(word))
            return null;
        else
            return lookup.get(word);

    }

    public static void main(String[] args) {
        WordVector wv = new WordVector();
        wv.print();
        double[] wv2 = new double[200];

    }
}

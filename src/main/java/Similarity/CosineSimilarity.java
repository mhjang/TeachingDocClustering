package Similarity;

import Clustering.Document;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * Created by mhjang on 2/1/14.
 */
public class CosineSimilarity  {
    /**
     * I think, eventually, unigrams/bigrams/trigrams should have different weights if matched because for ex.,
     * having a matching "trigram" is much more stronger indicator to the higher topic similarity than having a matching "unigram" obviously.
     * But for the current version of the baseline, let's just treat all terms equally.
     * Written in 2014/2/1 5:54 pm
     * @param d1
     * @param d2
     * @return
     */
    public static double CosineSimilarity(Document d1, Document d2) {
        double similarity = CosineSimilarity.CosineSimilarity(d1.getAllGrams(), d2.getAllGrams(), d1.getTermFrequencyMap(), d2.getTermFrequencyMap());
        return similarity;

    }

    public static double BinaryCosineSimilarity(Document d1, Document d2) {
        double similarity = CosineSimilarity.BinaryCosineSimilarity(d1.getAllGrams(), d2.getAllGrams());
        return similarity;

    }

    /**
     * computes cosine similarity between two documents on the given signature terms
     * @param d1
     * @param d2
     * @param signature
     * @return
     */
    public static double TFIDFCosineSimilarityForSignature(Document d1, Document d2, LinkedList<String> signature) {
        // vectorization
        int vecLen = signature.size();

        Double[] v1 = new Double[vecLen];
        Double[] v2 = new Double[vecLen];
        for(int i=0; i<vecLen; i++) {
            String label = signature.get(i);
            v1[i] = d1.getTFIDF(label);
            v2[i] = d2.getTFIDF(label);

        }
        // similarity calculatin between two vectors
        double sum = 0.0, sizeOfV1 = 0.0, sizeOfV2 = 0.0;
        for(int i=0; i<vecLen; i++) {
            sum += v1[i] * v2[i];
            sizeOfV1 += v1[i] * v1[i];
            sizeOfV2 += v2[i] * v2[i];
        }
        double similarity = (double)sum / (Math.sqrt(sizeOfV1) * (Math.sqrt(sizeOfV2)));
        return similarity;
    }



    /**
     * Represents a document as TF-IDF vector
     * @param d1
     * @param d2
     * @return
     */
    public static double TFIDFCosineSimilarity(Document d1, Document d2) {
        // vectorization
        HashSet<String> keys = new HashSet<String>();
        keys.addAll(d1.getAllGrams());
        keys.addAll(d2.getAllGrams());
        int vecLen = keys.size();
        String[] labelList = new String[vecLen];
        labelList = keys.toArray(labelList);

        Double[] v1 = new Double[vecLen];
        Double[] v2 = new Double[vecLen];
        for(int i=0; i<vecLen; i++) {
            String label = labelList[i];
            v1[i] = d1.getTFIDF(label);
            v2[i] = d2.getTFIDF(label);

        }
/*
        System.out.println("--------" + d1.getName() + "-----");
        for(int i=0; i<vecLen; i++) {
            System.out.print(v1[i]+", ");
        }
        System.out.println();
        System.out.println("--------" + d2.getName() + "-----");
        for(int i=0; i<vecLen; i++) {
            System.out.print(v2[i] + ", ");
        }

*/

        // similarity calculatin between two vectors
        double sum = 0.0, sizeOfV1 = 0.0, sizeOfV2 = 0.0;
        for(int i=0; i<vecLen; i++) {
            sum += v1[i] * v2[i];
            sizeOfV1 += v1[i] * v1[i];
            sizeOfV2 += v2[i] * v2[i];
        }
        double similarity = (double)sum / (Math.sqrt(sizeOfV1) * (Math.sqrt(sizeOfV2)));
        return similarity;
    }

    /**
     * This method is almost identical to "BinaryConsineSimilarity" except for the part that assigning the term frequency to the vectors,
     * whereas it just assigns '1' if the term exists in "BinaryCosineSimilarity" method
     * Note that the code is the same except for the star-marked lines
     * written in 2014/2/1 5:47pm
     * @param l1
     * @param l2
     * @param tf1 term frequency map for l1
     * @param tf2 term frequency map for l2
     * @return
     */
    public static double CosineSimilarity(LinkedList<String> l1, LinkedList<String> l2, HashMap<String, Integer> tf1, HashMap<String, Integer> tf2) {
        // vectorization
        HashSet<String> keys = new HashSet<String>();
        keys.addAll(l1);
        keys.addAll(l2);
        int vecLen = keys.size();
        String[] labelList = new String[vecLen];
        labelList = keys.toArray(labelList);

        Integer[] v1 = new Integer[vecLen];
        Integer[] v2 = new Integer[vecLen];
        for(int i=0; i<vecLen; i++) {
            String label = labelList[i];
            if(l1.contains(label))
                //* differ from BinaryCosineSimilarity
                v1[i] = tf1.get(label);
            else
                v1[i] = 0;
            if(l2.contains(label))
                //*
                v2[i] = tf2.get(label);
            else
                v2[i] = 0;
        }

        // similarity calculatin between two vectors
        int sum = 0, sizeOfV1 = 0, sizeOfV2 = 0;
        for(int i=0; i<vecLen; i++) {
            sum += v1[i] * v2[i];
            sizeOfV1 += v1[i] * v1[i];
            sizeOfV2 += v2[i] * v2[i];
        }
        double similarity = (double)sum / (Math.sqrt(sizeOfV1) * (Math.sqrt(sizeOfV2)));
        return similarity;
    }






    // for binary
    public static double BinaryCosineSimilarity(LinkedList<String> l1, LinkedList<String> l2) {
        // vectorization
        HashSet<String> keys = new HashSet<String>();
        keys.addAll(l1);
        keys.addAll(l2);
        int vecLen = keys.size();
        String[] labelList = new String[vecLen];
        labelList = keys.toArray(labelList);

        Integer[] v1 = new Integer[vecLen];
        Integer[] v2 = new Integer[vecLen];
        for(int i=0; i<vecLen; i++) {
            String label = labelList[i];
            if(l1.contains(label))
                v1[i] = 1;
            else
                v1[i] = 0;
            if(l2.contains(label))
                v2[i] = 1;
            else
                v2[i] = 0;
        }

        // similarity calculatin between two vectors
        int sum = 0, sizeOfV1 = 0, sizeOfV2 = 0;
        for(int i=0; i<vecLen; i++) {
            sum += v1[i] * v2[i];
            sizeOfV1 += v1[i] * v1[i];
            sizeOfV2 += v2[i] * v2[i];
        }
        double similarity = (double)sum / (Math.sqrt(sizeOfV1) * (Math.sqrt(sizeOfV2)));
        return similarity;
    }

}

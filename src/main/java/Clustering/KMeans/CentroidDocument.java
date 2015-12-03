package Clustering.KMeans;

import Clustering.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by mhjang on 5/14/14.
 */
public class CentroidDocument extends Document {
    static int TFIDFVECTOR = 0;
    static int BINARYVECTOR = 1;
    /** The maximum number of terms that are kept in the vector; default is all **/
    static int maxFeatureNum = Integer.MAX_VALUE;
    public CentroidDocument(Document d) {
        super(d.getName(), d.getUnigrams(), d.getBigrams(), d.getTrigrams());
        termTFIDFMap = d.getTFIDFMap();
    }

    public CentroidDocument(Document d, int k) {
        super(d.getName(), d.getUnigrams(), d.getBigrams(), d.getTrigrams());
        termTFIDFMap = d.getTFIDFMap();
        maxFeatureNum = k;
    }

    public CentroidDocument(String docName, LinkedList<String> unigrams, LinkedList<String> bigrams, LinkedList<String> trigrams) {
        super(docName, unigrams, bigrams, trigrams);
        termTFIDFMap = this.getTFIDFMap();

    }
    public CentroidDocument(LinkedList<String> unigrams) {
        super(unigrams);
        termTFIDFMap = this.getTFIDFMap();
    }



    /**
     *
     * @param option = {CentroidDocument.TFIDFVECTOR, BINARYVECTOR}
     */
    public void updateNewCentroid(Document d, int option) {
        HashSet<String> keys = new HashSet<String>();
        keys.addAll(d.getAllGrams());
        keys.addAll(this.getAllGrams());
        int vecLen = keys.size();
        String[] labelList = new String[vecLen];
        labelList = keys.toArray(labelList);
        //     System.out.println(keys.size());
        // vectorization
        for(int i=0; i<vecLen; i++) {
            String label = labelList[i];
            double vectorscore = 0.0;
            if(option == CentroidDocument.TFIDFVECTOR) {
                vectorscore += d.getTFIDF(label);
            }
            else if(option == CentroidDocument.BINARYVECTOR) {
                // has to be implemented
            }

            // centroid itself
            if(option == CentroidDocument.TFIDFVECTOR) {
                vectorscore += this.getTFIDF(label);
            }
            else if(option == CentroidDocument.BINARYVECTOR) {
                // has to be implemented
            }
            vectorscore = vectorscore / (double)vecLen;
            updateVector(label, vectorscore);
        }

    }


    /**
     *
     * @param cluster
     * @param option = {CentroidDocument.TFIDFVECTOR, BINARYVECTOR}
     */
    public void updateNewCentroid(LinkedList<Document> cluster, int option) {
        HashSet<String> keys = new HashSet<String>();
        for(Document d: cluster) {
            keys.addAll(d.getAllGrams());
        }
        keys.addAll(this.getAllGrams());
        int vecLen = keys.size();
        String[] labelList = new String[vecLen];
        labelList = keys.toArray(labelList);
   //     System.out.println(keys.size());
        // vectorization
        for(int i=0; i<vecLen; i++) {
            String label = labelList[i];
            double vectorscore = 0.0;
            for(Document d: cluster) {
                if(option == CentroidDocument.TFIDFVECTOR) {
                    vectorscore += d.getTFIDF(label);
                }
                else if(option == CentroidDocument.BINARYVECTOR) {
                    // has to be implemented
                }
            }
            // centroid itself
            if(option == CentroidDocument.TFIDFVECTOR) {
                vectorscore += this.getTFIDF(label);
            }
            else if(option == CentroidDocument.BINARYVECTOR) {
                // has to be implemented
            }
            vectorscore = vectorscore / (double)vecLen;
            updateVector(label, vectorscore);
       //     System.out.print(vectorscore + "\t");
        }
        // test print
   /*     for(int i=0; i<((vecLen>100)?100:vecLen); i++) {
            String label = labelList[i];
            System.out.print(label + "("+ this.getTFIDF(label) + ") ");
        }
        System.out.println();
        */
    //    selectTopTFIDFFeatures(20);
    }

    /**
     * remove all features but k number of top TF-IDF terms
     * @param k
     */
    public void selectTopTFIDFFeatures(int k) {
        LinkedList<Map.Entry<String, Double>> topTFIDFTerms = this.getTopTermsTFIDF(k);
        HashMap<String, Double> newTFIDFMAp = new HashMap<String, Double>();
        for(Map.Entry<String, Double> e : topTFIDFTerms) {
            newTFIDFMAp.put(e.getKey(), e.getValue());
        }
        this.setTFIDFMap(newTFIDFMAp);
    }


    public void updateVector(String term, double score) {
        if(!unigrams.contains(term))
            unigrams.add(term);
        this.getTFIDFMap().put(term, score);
    }


}

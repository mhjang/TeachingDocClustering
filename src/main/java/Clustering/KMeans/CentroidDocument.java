package Clustering.KMeans;

import Clustering.*;

import java.util.HashSet;
import java.util.LinkedList;

/**
 * Created by mhjang on 5/14/14.
 */
public class CentroidDocument extends Document {
    static int TFIDFVECTOR = 0;
    static int BINARYVECTOR = 1;

    public CentroidDocument(Document d) {
        super(d.getName(), d.getUnigrams(), d.getBigrams(), d.getTrigrams());
        termTFIDFMap = d.getTFIDFMap();
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
        System.out.println(this.getTFIDFMap().size());
        // test print
        for(int i=0; i<((vecLen>100)?100:vecLen); i++) {
            String label = labelList[i];
     //       System.out.print(this.getTFIDF(label) + " ");
        }
        System.out.println();
    }



    public void updateVector(String term, double score) {
        if(!unigrams.contains(term))
            unigrams.add(term);
        this.getTFIDFMap().put(term, score);
    }


}

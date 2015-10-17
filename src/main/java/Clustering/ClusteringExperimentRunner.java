package Clustering;

import Classify.liblinear.datastructure.DatasetDir;
import Clustering.KMeans.KMeansClustering;
import TeachingDocParser.Tokenizer;
import TermScoring.LanguageModeling.LanguageModeling;
import TermScoring.TFIDF.TFIDFCalculator;
import evaluation.ClusteringFMeasure;

import java.io.*;
import java.util.*;

/**
 * Created by mhjang on 10/15/15.
 */
public class ClusteringExperimentRunner {

    DocumentCollection dc;
    HashMap<String, Document> clusterFeatureMap;



    public void comparePerformanceByVectorSize(int min, int max, int gap) throws Exception {
        TFIDFCalculator tfidf = new TFIDFCalculator(true);
        tfidf.calulateTFIDF(TFIDFCalculator.LOGTFIDF, DatasetDir.CLUSTER_goldstandard_manual, Tokenizer.BIGRAM, false);
        DocumentCollection dc = tfidf.getDocumentCollection();
        LanguageModeling lm = new LanguageModeling(dc, 0.7, 0.2);
        for(int i = min; i<=max; i+=gap) {
       //     System.out.print("k = " + i + "\t");
            lm.TFIDFBaselineRun(i);
            KMeansClustering kmeans = new KMeansClustering("./goldstandard/dsa_docseed", false, dc);
            HashMap<String, LinkedList<String>> clustersWithoutNoise = kmeans.convertToTopicMap(kmeans.clusterRun(10, 0.05));
            ClusteringFMeasure cfm = new ClusteringFMeasure(clustersWithoutNoise, "./goldstandard/dsa_docseed", "./goldstandard/goldstandard_v2.csv", dc);
            cfm.compAccuracyOnlyItemsInGold();
        }
    }
    public static void main(String[] args) throws Exception {

        ClusteringExperimentRunner cer = new ClusteringExperimentRunner();
        cer.comparePerformanceByVectorSize(5, 100, 5);


    }

}

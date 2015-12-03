package Clustering;

import Classify.liblinear.datastructure.DatasetDir;
import Clustering.KMeans.KMeansClustering;
import TeachingDocParser.Tokenizer;
import TermScoring.LanguageModeling.LanguageModeling;
import TermScoring.TFIDF.TFIDFCalculator;
import evaluation.ClusteringFMeasure;
import evaluation.ClusteringFMeasure2;

import java.io.*;
import java.util.*;

/**
 * Created by mhjang on 10/15/15.
 */
public class ClusteringExperimentRunner {

    DocumentCollection dc;
    HashMap<String, Document> clusterFeatureMap;



    public void comparePerformanceByVectorSize(int min, int max, int gap) throws Exception {
        TFIDFCalculator tfidf = new TFIDFCalculator(false);
        tfidf.calulateTFIDF(TFIDFCalculator.LOGTFIDF, DatasetDir.CLUSTER_OS_original, Tokenizer.BIGRAM, false);
        DocumentCollection dc = tfidf.getDocumentCollection();
        LanguageModeling lm = new LanguageModeling(dc, 0.7, 0.2);
  //      for(int i = min; i<=max; i+=gap) {
            lm.TFIDFBaselineRun(30);
            KMeansClustering kmeans = new KMeansClustering("./goldstandard/os_topics", false, dc);
   //         HashMap<String, LinkedList<String>> clustersWithoutNoise = kmeans.convertToTopicMap(kmeans.clusterRun(10, 0.05));
            ClusteringFMeasure2 cfm = new ClusteringFMeasure2(kmeans.clusterRun(10, 0.05), "./goldstandard/os_goldstandard");
            cfm.compAccuracyOnlyItemsInGold();
  //      }
    }
    public static void main(String[] args) throws Exception {

        ClusteringExperimentRunner cer = new ClusteringExperimentRunner();
        cer.comparePerformanceByVectorSize(5, 100, 10);


    }

}

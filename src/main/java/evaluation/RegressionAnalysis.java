package evaluation;

import Classify.liblinear.datastructure.DatasetDir;
import Clustering.Document;
import Similarity.CosineSimilarity;
import TeachingDocParser.Tokenizer;
import TermScoring.TFIDF.TFIDFCalculator;
import simple.io.myungha.DirectoryReader;
import simple.io.myungha.SimpleFileWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by mhjang on 10/1/15.
 * Generate a file that contains cosine similarity and binary category match
 * for regression analysis
 *
 * cosine similarity \t category match
 */

public class RegressionAnalysis {
    public static void main(String[] args) {
        String dir = DatasetDir.CLUSTER_OS_removed;
        TFIDFCalculator tfidf = null;
        try {
            tfidf = new TFIDFCalculator(false);
            tfidf.calulateTFIDF(TFIDFCalculator.LOGTFIDF, dir, Tokenizer.BIGRAM, false);
            HashMap<String, Document> documentSet = tfidf.documentSet;
            ClusteringFMeasure eval = new ClusteringFMeasure(null, "./goldstandard/os_topics", "./goldstandard/os_goldstandard", null);

            SimpleFileWriter sw = new SimpleFileWriter("os_regression_removed.txt");
            DirectoryReader dr = new DirectoryReader(dir);
            ArrayList<String> filelist = dr.getFileNameList();
            for(int i=0; i<filelist.size(); i++) {
                Document d1 = documentSet.get(filelist.get(i).toLowerCase());
                for(int j=i+1; j<filelist.size(); j++) {
                    String fileB = filelist.get(j);
                    Document d2 = documentSet.get(filelist.get(j).toLowerCase());
                    double similarity = CosineSimilarity.TFIDFCosineSimilarity(d1, d2);
                    int label = eval.isSameClass(d1.getName(), d2.getName())? 1: 0;
                    System.out.println(d1.getName() + "\t" + d2.getName() + "\t" + similarity + "\t" + label);
                    sw.writeLine(similarity + "\t" + label);
                }
            }

        sw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }



    }
}

package Clustering.KMeans;

import Clustering.Document;
import Clustering.DocumentCollection;
import TeachingDocParser.Stemmer;
import TeachingDocParser.StopWordRemover;
import TeachingDocParser.Tokenizer;
import TermScoring.LanguageModeling.LanguageModeling;
import TermScoring.TFIDF.TFIDFCalculator;
import evaluation.ACLClusteringFMeasure;
import evaluation.ClusteringFMeasure;

import java.io.*;
import java.util.*;

/**
 * Created by mhjang on 9/28/14.
 */
public class ACLClustering {
    public static void main(String[] arsg) throws IOException {
        // Setting the parameters

        int firstTopK = 100;
        double clusteringThreshold = 0.02;
        int infrequentTermThreshold = 0;
        int kgramsTermThreshold = 1;

        double[] alpha = {0.1, 0.2, 0.3, 0.4};
        double beta = 0.5;


        // parameter: whether to use Google N-gram
        TFIDFCalculator tfidf = new TFIDFCalculator(true);
        tfidf.calulateTFIDF(TFIDFCalculator.LOGTFIDF, "/Users/mhjang/Desktop/clustering/extracted", Tokenizer.UNIGRAM, false);

        DocumentCollection dc = tfidf.getDocumentCollection();

        System.out.println("Documents features ready");


        /**
         * Applying language modeling
         * After this method, dc.unigram contains upto top K terms sorted by language modeling
         * by default, k = 50
         * NOTE that this method NULLIFIES bigrams and trigrams.
         */
        LanguageModeling lm = new LanguageModeling(dc, 30, 0.7, 0.2);
//        lm.run();
        lm.selectHighTFTerms();
//      lm.TFIDFBaselineRun();

        Integer clusterLabelIndex = 0;
        HashMap<String, Integer> clusterLabelMap = new HashMap<String, Integer>();

        String goldstandardDir = "/Users/mhjang/Desktop/clearnlp/dataset/acl/goldstandard.txt";
        String clusterTopicsDir = "/Users/mhjang/Desktop/clearnlp/dataset/acl/clustertopics.txt";




        KMeansClustering kmeans = new KMeansClustering(constructSeedDocuments(clusterTopicsDir, dc), dc);
        LinkedList<LinkedList<Document>> clusters = kmeans.clusterRun(10, 0.05);
        ACLClusteringFMeasure cfm = new ACLClusteringFMeasure(clusters, goldstandardDir);
        cfm.computeAccuracy();

    }



    // 2014/9/25
    // We don't really need hashmap keys, but had to make a fake key to fit in KMeansClustering constructor's parameters
    public static HashMap<String, Document> constructSeedDocuments(String goldDir, DocumentCollection dc) {
        HashMap<String, Document> topics = new HashMap<String, Document>();
        Stemmer stemmer = new Stemmer();
//        HashMap<String, HashSet<String>> goldstandard = readGoldstandardACLDataset("/Users/mhjang/Desktop/clearnlp/dataset/acl/goldstandard.txt");
        try {
            BufferedReader br = new BufferedReader(new FileReader(goldDir));
            String line;
            HashMap<String, Integer> topicClusterMap = new HashMap<String, Integer>();
            LinkedList<String> topicTokens = new LinkedList<String>();
            HashMap<String, Integer> topicTokensFreq = new HashMap<String, Integer>();

            int clusterId = 0;
            while ((line = br.readLine()) != null) {
                if (line.trim().length() > 0) {

                    line = line.toLowerCase().trim();
                    line = line.replace(":", "");
                    line = line.replace("-", " ");
                    line = line.replace("&", "");

                    String[] tokens = line.split("\\/| |\\,");

                    for (int i = 0; i < tokens.length; i++) {
                        String token = tokens[i].trim();
                        if(token.length() > 0) {
                            token = stemmer.stemString(token);
                            // if this term had already appeared before, just add the frequency count
                            if(topicTokensFreq.containsKey(token))
                                topicTokensFreq.put(token, topicTokensFreq.get(token) + 1);
                                // if this term appears for the first time, initiate the count and add it to the word list
                            else {
                                topicTokensFreq.put(token, 1);
                                topicTokens.add(token);
                            }
                        }
                    }

                } else {
                    if (topicTokens.size() > 0) {
                        //           document.printTerms();
                        clusterId++;
                        Document topicDoc = new Document(topicTokens);

                        topicDoc.setTermFrequency(topicTokensFreq);
                        TFIDFCalculator.calculateTFIDFGivenCollection(topicDoc, dc, TFIDFCalculator.LOGTFIDF);

                        topics.put(String.valueOf(clusterId), topicDoc);

                        topicTokens = new LinkedList<String>();
                        topicTokensFreq = new HashMap<String, Integer>();

                    } else {
                        continue;

                    }
                }
            }
            clusterId++;
            Document topicDoc = new Document(topicTokens);
            topicDoc.setTermFrequency(topicTokensFreq);
            TFIDFCalculator.calculateTFIDFGivenCollection(topicDoc, dc, TFIDFCalculator.LOGTFIDF);
            topics.put(String.valueOf(clusterId), topicDoc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        for(String id: topics.keySet()) {
            Document doc = topics.get(id);
            System.out.println("topic id: " + id);
            doc.printTerms();
        }

        return topics;
    }
}



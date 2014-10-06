package evaluation;

import Clustering.Document;
import Clustering.DocumentCollection;
import componentDetection.DetectCodeComponent;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.HashSet;

/**
 * Created by mhjang on 2/7/14.
 * Written at 2:01 pm
 */
public class ACLClusteringFMeasure {

    LinkedList<LinkedList<String>> clusters;
    HashMap<Integer, HashSet<String>> goldClusters;
    HashSet<String> goldpool;

    // this is a horrible constructor design... refactor later
    public ACLClusteringFMeasure(LinkedList<LinkedList<Document>> clusters_, String goldDir) {
        clusters = new LinkedList<LinkedList<String>>();
        for(int i=0; i<clusters_.size(); i++) {
            LinkedList<String> docs = new LinkedList<String>();
            for(Document d : clusters_.get(i)) {
                docs.add(d.getName());
            }
            clusters.add(i, docs);
        }
        goldClusters = readGoldstandardACLDataset(goldDir);
    }


    public HashMap<Integer, HashSet<String>> readGoldstandardACLDataset (String goldDir) {
        HashMap<Integer, HashSet<String>> goldstandard = null;
        HashMap<String, Integer> topics = readTopicClustersACL("/Users/mhjang/Desktop/clearnlp/dataset/acl/clustertopics.txt");
        try {
            BufferedReader br = new BufferedReader(new FileReader(goldDir));
            String line;
            goldstandard = new HashMap<Integer, HashSet<String>>();

            while ((line = br.readLine()) != null) {
                if (line.length() > 0) {
                    line = line.trim().toLowerCase();
                    String filename = line.substring(line.length() - 8, line.length());
                    String title = line.substring(0, line.indexOf('-')).trim();
                    if(topics.containsKey(title)) {
                        Integer clusterId = topics.get(title);
                        if (!goldstandard.containsKey(clusterId)) {
                            goldstandard.put(clusterId, new HashSet<String>());
                        } else {
                            goldstandard.get(clusterId).add(filename);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return goldstandard;

    }


    // 2014/9/25
    // We don't really need hashmap keys, but had to make a fake key to fit in KMeansClustering constructor's parameters
    public static HashMap<String, Integer>  readTopicClustersACL(String goldDir) {
        HashMap<Integer, Document> topics = new HashMap<Integer, Document>();
//        HashMap<String, HashSet<String>> goldstandard = readGoldstandardACLDataset("/Users/mhjang/Desktop/clearnlp/dataset/acl/goldstandard.txt");
        HashMap<String, Integer> topicClusterMap = new HashMap<String, Integer>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(goldDir));
            String line;
            LinkedList<String> tokenSet = new LinkedList<String>();
            int clusterId = 0;
            while ((line = br.readLine()) != null) {
                if (line.trim().length() > 0) {
                    line = line.trim().toLowerCase();
                    topicClusterMap.put(line, clusterId);
                    String[] tokens = line.split("\\/| |\\,");
                    for (int i = 0; i < tokens.length; i++) {
                        if (!tokenSet.contains(tokens[i]))
                            tokenSet.add(tokens[i]);
                    }
                    topicClusterMap.put(line, clusterId);
                    System.out.println("topic: " + line + " (" + line.length() + ")");
                } else {
                    if (tokenSet.size() > 0) {
                        Document document = new Document(tokenSet);
                        //           document.printTerms();
                        tokenSet = new LinkedList<String>();
                        topics.put(clusterId, document);
                        clusterId++;
                    } else {
                        continue;

                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return topicClusterMap;
    }



    /**
     * compute accuracy only the items that are present in gold standard
     * filter out the clustering result by removing the ones not in gold standard, and call computeAccuracy
     *
     */
    public void compAccuracyOnlyItemsInGold() {
        System.out.println("Accuracy in Gold Standard");
        LinkedList<LinkedList<Document>> newClusters = new LinkedList<LinkedList<Document>>();
        if(goldpool == null) {
            goldpool = new HashSet<String>();
            for(Integer clusterId : goldClusters.keySet()) {
                for(String docId : goldClusters.get(clusterId)) {
                    goldpool.add(docId);
                }
            }
        }
        for(Integer clusterId : goldClusters.keySet()) {
            LinkedList<String> cluster = clusters.get(clusterId);
            LinkedList<String> newCluster = new LinkedList<String>();
            for (String d : cluster) {
                if (goldpool.contains(d))
                    newCluster.add(d);
            }
            clusters.set(clusterId, newCluster);
        }
        computeAccuracy();
    }

    public void computeAccuracy() {
        double avgPrecision = 0.0, avgRecall = 0.0;
        //
        for(Integer clusterId : goldClusters.keySet()) {
            HashSet<String> goldCluster = goldClusters.get(clusterId);
            System.out.println(clusterId + " th Cluster");
            for (String doc : goldCluster) {
                System.out.print(doc + ", ");
            }
            System.out.println();
        }
            for(Integer clusterId : goldClusters.keySet()) {
            HashSet<String> goldCluster = goldClusters.get(clusterId);
            LinkedList<String> cluster = clusters.get(clusterId);
            /***
             * precision = correctInCluster / |cluster|
             * recall = correctInCluster / |gold cluster|
             */
            int correctInCluster = 0, correctInGoldCluster = 0;
            for (String element : goldCluster) {
                //         System.out.println(element);
                if (cluster.contains(element)) correctInCluster++;
            }
            double precision = 0.0, recall = 0.0, fMeasure = 0.0;
            if (cluster.size() > 0) {
                precision = (double) (correctInCluster) / (double) (cluster.size());
            }
            else
                precision = 0.0;
            if (goldCluster.size() > 0)
                recall = (double) (correctInCluster) / (double) (goldCluster.size());
            else
                recall = -1;
            avgPrecision += precision;
            avgRecall += recall;

            fMeasure = (2 * precision * recall) / (precision + recall);
            System.out.println(clusterId + "\t" + precision + "\t" + recall + "\t" + fMeasure);
        }
        double length = (double)goldClusters.size();
        avgPrecision /= length;
        avgRecall /= length;
        System.out.println(avgPrecision+ "\t"+ avgRecall + "\t" + (2*avgPrecision*avgRecall)/(avgPrecision + avgRecall));


    }


}

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
public class ClusteringFMeasure {

    HashMap<String, LinkedList<String>> clusters;
    HashMap<String, Integer> clusterLabelMap;
    HashMap<Integer, HashSet<String>> goldClusters;
    HashSet<String> goldpool = new HashSet<String>();
    ArrayList<String> topiclist;
    DocumentCollection dc;

    public ClusteringFMeasure(HashMap<String, LinkedList<String>> clusters_, HashMap<String, Integer> clusterLabelMap_, ArrayList<String> topiclist_, String goldDir, DocumentCollection dc) {
        clusters = clusters_;
        clusterLabelMap = clusterLabelMap_;

        goldClusters = readGoldstandard(goldDir);
        this.topiclist = topiclist_;
        this.dc = dc;
    }

    public ClusteringFMeasure() {

    }


    public void errorAnalysis(HashMap<String, LinkedList<String>> oldCluster, HashMap<String, LinkedList<String>> newCluster) {
        for(String doc : oldCluster.keySet()) {
            int correctDropped = 0, correctIntroduced = 0, wrongDropped = 0, wrongIntroduced = 0;
            LinkedList<String> oldClusterList = oldCluster.get(doc);
            LinkedList<String> newClusterList = newCluster.get(doc);

            // remove duplicate items
            LinkedList<String> duplicateItems = new LinkedList<String>();
            for(String clusterItem : oldClusterList) {
                if(newClusterList.contains(clusterItem)) {
                    duplicateItems.add(clusterItem);
                }
            }
            for(String duplicateItem : duplicateItems) {
                oldClusterList.remove(duplicateItem);
                newClusterList.remove(duplicateItem);
            }
            HashSet<String> goldItems = goldClusters.get(clusterLabelMap.get(doc));
            System.out.println(doc);
            for(String item : oldClusterList) {
                System.out.print(item + " ");
                if(goldItems.contains(item)) {
                    correctDropped++;
                    System.out.print("(CD) ");
                }
                else {
                    wrongDropped++;
                    System.out.print("(WD) ");

                }
            }

            for(String item : newClusterList) {
                System.out.print(item + " ");
                if(goldItems.contains(item)) {
                    correctIntroduced++;
                    System.out.print("(CI) ");

                }
                else {
                    wrongIntroduced++;
                    System.out.print("(WI) ");
                }
            }
            System.out.println();
            System.out.println(correctDropped + "\t" + correctIntroduced + "\t" + wrongDropped + "\t" + wrongIntroduced);
        }
    }

    public HashMap<String, HashSet<String>> readGoldstandardACLDataset (String goldDir) {
        HashMap<String, HashSet<String>> goldstandard = null;
        try {
            BufferedReader br = new BufferedReader(new FileReader(goldDir));
            String line;
            goldstandard = new HashMap<String, HashSet<String>>();

            while ((line = br.readLine()) != null) {
                if (line.length() > 0) {
                    line = line.trim();
                    String filename = line.substring(line.length() - 8, line.length());
                    String title = line.substring(0, line.indexOf('-')).trim();
                    if (goldstandard.containsKey(title)) {
                        goldstandard.get(title).add(filename);
                    } else {
                        goldstandard.put(title, new HashSet<String>());
                        goldstandard.get(title).add(filename);
                    }
                    System.out.println(title + ":" + filename);
                }
            }
            System.out.println("set size: " + goldstandard.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return goldstandard;

    }

    public void readTopicClustersACL(String goldDir) {
        LinkedList<Document> topics = new LinkedList<Document>();
//        HashMap<String, HashSet<String>> goldstandard = readGoldstandardACLDataset("/Users/mhjang/Desktop/clearnlp/dataset/acl/goldstandard.txt");
        try {
            BufferedReader br = new BufferedReader(new FileReader(goldDir));
            String line;
            HashMap<String, Integer> topicClusterMap = new HashMap<String, Integer>();
            LinkedList<String> tokenSet = new LinkedList<String>();
            int clusterId = 0;
            while ((line = br.readLine()) != null) {
                if (line.trim().length() > 0) {
                    topicClusterMap.put(line, clusterId);
                    String[] tokens = line.split("\\/| |\\,");
                    for (int i = 0; i < tokens.length; i++) {
                        if (!tokenSet.contains(tokens[i]))
                            tokenSet.add(tokens[i].toLowerCase());
                    }
                    topicClusterMap.put(line, clusterId);
              //      System.out.println(line + ": " + clusterId);
               //     HashSet<String> docs = goldstandard.get(line);
               //     for(String doc : docs) {
               ////         System.out.print(doc + ", ");
               //     }
               //     System.out.println();

                } else {
                    if (tokenSet.size() > 0) {
                        Document document = new Document(tokenSet);
             //           document.printTerms();
                        tokenSet = new LinkedList<String>();
                        topics.add(document);
                        clusterId++;
                    } else {
                        continue;

                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return topics;
    }

    // read goldstandard for slide data
    public  HashMap<Integer, HashSet<String>> readGoldstandard(String goldDir) {
        HashMap<Integer, HashSet<String>> goldstandard = null;
        Integer clusterID = 0;
        try{
            BufferedReader br = new BufferedReader(new FileReader(goldDir));
            goldstandard = new HashMap<Integer, HashSet<String>>();
            String line;
            while((line = br.readLine()) != null) {
                String[] elements = line.split(",");
                HashSet<String> documents = new HashSet<String>();
                for(int i=0; i<elements.length; i++) {
                    if(elements[i].length() >0) {
                        documents.add(elements[i]);
                        goldpool.add(elements[i]);
          //              System.out.print(elements[i] + "\t");
                    }
                }
      //          System.out.println(clusterID);
                goldstandard.put(clusterID++, documents);
            }
        }catch(Exception e) {
           e.printStackTrace();
        }
        return goldstandard;

    }

    /**
     * to see how many lines of codes were affected in each cluster
     */
    public void analyzeCodeRemovedPerCluster() throws FileNotFoundException {
        for (String clusterName : topiclist) {
            HashSet<String> goldCluster = goldClusters.get(clusterLabelMap.get(clusterName));
            int count = 0, otherlines = 0;
            for (String element : goldCluster) {
                BufferedReader br = new BufferedReader(new FileReader(new File("/Users/mhjang/Documents/teaching_documents/extracted/stemmed/"+element)));
                try {
                    String line = br.readLine();
                    while (line != null) {
                        if (line.length() > 0) {
                            line = line.toLowerCase().trim();
                            if (DetectCodeComponent.isCodeLine(line))
                                count++;
                            else
                                otherlines++;
                        }
                        line = br.readLine();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            double ratio = (double)count/(double)(count + otherlines);
            System.out.println(clusterName + ": " + count + "/" + otherlines + ":" + ratio);
        }
    }

    /**
     * compute accuracy only the items that are present in gold standard
     * filter out the clustering result by removing the ones not in gold standard, and call computeAccuracy
     *
     */
    public HashMap<String, LinkedList<String>> compAccuracyOnlyItemsInGold() {
        System.out.println("Accuracy in Gold Standard");
        for(String clusterName : topiclist) {
            LinkedList<String> cluster = clusters.get(clusterName);
            LinkedList<String> newCluster = new LinkedList<String>();
            for (String d : cluster) {
                if (goldpool.contains(d))
                    newCluster.add(d);
            }
            clusters.put(clusterName, newCluster);
        }
        computeAccuracy();
        return clusters;
    }
    public void computeAccuracy() {
        double avgPrecision = 0.0, avgRecall = 0.0;
        for(String clusterName : topiclist) {
            HashSet<String> goldCluster = goldClusters.get(clusterLabelMap.get(clusterName));
            LinkedList<String> cluster = clusters.get(clusterName);
            /***
             * precision = correctInCluster / |cluster|
             * recall = correctInCluster / |gold cluster|
             */
            int correctInCluster = 0, correctInGoldCluster = 0;
            for (String element : goldCluster) {
       //         System.out.println(element);
                if (cluster.contains(element)) correctInCluster++;
            }
       //     for(String element : cluster) {
       //         dc.getDocument(element).printTerms();
       //     }
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
            System.out.println(clusterName + "\t" + precision + "\t" + recall + "\t" + fMeasure);
        }
        double length = (double)topiclist.size()-1;
        avgPrecision /= length;
        avgRecall /= length;
        System.out.println(avgPrecision+ "\t"+ avgRecall + "\t" + (2*avgPrecision*avgRecall)/(avgPrecision + avgRecall));

//        System.out.println("Avg Precision: " + avgPrecision+ "\t Avg Recall: " + avgRecall + "\t Avg F-measure:" + (2*avgPrecision*avgRecall)/(avgPrecision + avgRecall));
 //      return fMeasure;

      }


}

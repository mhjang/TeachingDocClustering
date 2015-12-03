package evaluation;

import Clustering.Document;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * Created by mhjang on 10/28/15.
 * evaluates the clustering result wihtout topic list index
 */
public class ClusteringFMeasure2 {
    LinkedList<LinkedList<Document>> clusters;
    LinkedList<LinkedList<String>> goldstandard;
    HashSet<String> goldpool;

    public ClusteringFMeasure2(LinkedList<LinkedList<Document>> clusters_, String goldDir) {
        clusters = clusters_;
        goldpool = new HashSet<String>();
        Integer clusterID = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader(goldDir));
            goldstandard = new LinkedList<LinkedList<String>>();
            String line;
            while ((line = br.readLine()) != null) {
                String[] elements = line.split(",");
                LinkedList<String> documents = new LinkedList<String>();
                for (int i = 0; i < elements.length; i++) {
                    if (elements[i].length() > 0) {
                        documents.add(elements[i]);
                        goldpool.add(elements[i]);

                    }
                }
                goldstandard.add(documents);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    /**
     * compute accuracy only the items that are present in gold standard
     * filter out the clustering result by removing the ones not in gold standard, and call computeAccuracy
     */
    public void compAccuracyOnlyItemsInGold() {
        //    System.out.println("Accuracy in Gold Standard");
        double precision = 0.0, recall = 0.0, avgPrecision = 0.0, avgRecall = 0.0, fMeasure=0.0;
        for (int i = 0; i < clusters.size(); i++) {
            int correctInCluster = 0;

            LinkedList<Document> cluster = clusters.get(i);
            LinkedList<String> gold = goldstandard.get(i);
            LinkedList<String> clusterInGold = removeItemsNotAnnotated(cluster);

            for (String element : gold) {
                //         System.out.println(element);
                if (clusterInGold.contains(element)) correctInCluster++;
            }

            if (clusterInGold.size() > 0) {
                precision = (double) (correctInCluster) / (double) (clusterInGold.size());
            }
            else
                precision = 0.0;
            if (gold.size() > 0)
                recall = (double) (correctInCluster) / (double) (gold.size());
            else
                recall = -1;
            avgPrecision += precision;
            avgRecall += recall;
            System.out.println(i + "\t" + (2*precision*recall)/(precision + recall));
        }
    //      fMeasure = (2 * precision * recall) / (precision + recall);
            //       System.out.println(clusterName + "\t" + precision + "\t" + recall + "\t" + fMeasure);
        avgPrecision /= (double) clusters.size();
        avgRecall /= (double) clusters.size();
        System.out.println(avgPrecision+ "\t"+ avgRecall + "\t" + (2*avgPrecision*avgRecall)/(avgPrecision + avgRecall));

    }

    public LinkedList<String> removeItemsNotAnnotated(LinkedList<Document> cluster) {
        LinkedList<String> newCluster = new LinkedList<String>();
        for(Document s : cluster) {
            if(goldpool.contains(s.getName()))
                newCluster.add(s.getName());
        }
        return newCluster;
    }
}

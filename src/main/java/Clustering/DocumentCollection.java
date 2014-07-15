package Clustering;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by mhjang on 2/18/14.
 */
public class DocumentCollection {

    private HashMap<String, Document> documentSet = null;
    private HashMap<String, Integer> globalTermCountMap = null;
    private HashMap<String, Integer> binaryTermFreqInDoc = null;

    public DocumentCollection(HashMap<String, Document> documentSet, HashMap<String, Integer> globalTermCountMap, HashMap<String, Integer> binaryTermFreqInDoc) {
        this.documentSet = documentSet;
        this.globalTermCountMap = globalTermCountMap;
        this.binaryTermFreqInDoc = binaryTermFreqInDoc;
    }

    public HashMap<String, Document> getDocumentSet()
    {
        return documentSet;
    }

    /**
     * 2014/3/23
     * Print the average number of unigrams over the collection
     */
    public void printUnigramStats() {
        int sum = 0;
        for(Document d : documentSet.values()) {
            sum += d.getUnigrams().size();
        }
        System.out.println("Unigram Stats: " + (double)(sum)/(double)(documentSet.size()));
    }

    /**
     * 2014/7/1
     * Construct a signature vector from a document collection with top K terms extracted from each document
     * @return LinkedList<String>
     */
    public LinkedList<String> constructSignatureVector(int k) {
        LinkedList<String> signatureTerms = new LinkedList<String>();
        System.out.println("signature terms");
        for(Document d : documentSet.values()) {
           LinkedList<Map.Entry<String, Double>> keywords = d.getTopTermsTFIDF(k);
           for(Map.Entry<String, Double> terms : keywords) {
               if(!signatureTerms.contains(terms.getKey())) {
                   signatureTerms.add(terms.getKey());
                   System.out.print(terms.getKey() + "\t");
               }
           }
        }
        System.out.println();
        return signatureTerms;
    }





    public int getCollectionSize() {
        return documentSet.size();
    }
    public HashMap<String, Double> getWordProbablity() {
        HashMap<String, Double> wordProb = new HashMap<String, Double>();
        int wordCountSum = 0;
        for(String t : globalTermCountMap.keySet()) {
            wordCountSum+=globalTermCountMap.get(t);
        }
        double probSum = 0.0;
        for(String t : globalTermCountMap.keySet()) {
            double wProb = (double) globalTermCountMap.get(t) / (double) wordCountSum;
            probSum += wProb;
            wordProb.put(t, wProb);
        }
  //      System.out.println("sum of global word prob: " + probSum);
        return wordProb;

    }

    public void printDocumentList() {
        for(String doc : documentSet.keySet()) {
            System.out.println(doc);
        }
    }
    public HashMap<String, Integer> getglobalTermCountMap()
    {
        return globalTermCountMap;
    }
    public HashMap<String, Integer> getBinaryTermFreqInDoc()
    {
        return binaryTermFreqInDoc;
    }

    public Document getDocument(String docName) {
        if(documentSet.containsKey(docName))
            return documentSet.get(docName);
        else return null;
    }

}

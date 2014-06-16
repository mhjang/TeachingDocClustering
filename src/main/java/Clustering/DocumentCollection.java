package Clustering;

import java.util.HashMap;

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

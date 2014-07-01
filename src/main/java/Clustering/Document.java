package Clustering;



import TeachingDocParser.Tokenizer;

import java.util.*;

/**
 * Created by mhjang on 1/31/14.
 */
public class Document {
    public LinkedList<String> unigrams;
    public LinkedList<String> bigrams;
    public LinkedList<String> trigrams;
    public static int UNIGRAM = 0;
    public static int BIGRAM = 1;
    public static int TRIGRAM = 2;
    private String docName;
    HashMap<String, Integer> termFrequency = null;
    public HashMap<String, Double> termTFIDFMap = new HashMap<String, Double>();
    LinkedList<ArrayList<String>> corpusByLine;
    LinkedList<String> corpus = new LinkedList<String>();
    private HashMap<String, Double> languageModelProb = new HashMap<String, Double>();

    public Document(String docName, LinkedList<String> unigrams, LinkedList<String> bigrams, LinkedList<String> trigrams) {
        this.unigrams = unigrams;
        this.bigrams = bigrams;
        this.trigrams = trigrams;
        this.docName = docName;
    }

    public Document(String docName, LinkedList<String> unigrams, LinkedList<String> bigrams, LinkedList<String> trigrams, LinkedList<ArrayList<String>> corpusByLine) {
        this.unigrams = unigrams;
        this.bigrams = bigrams;
        this.trigrams = trigrams;
        this.docName = docName;
        this.corpusByLine = corpusByLine;
    }


    public Document(LinkedList<String> unigrams) {
        this.unigrams = unigrams;
    }


    public HashMap<String, Double> getTFIDFMap() {
        return termTFIDFMap;
    }

    public void setTFIDFMap(HashMap<String, Double> newTFIDFMap) {
        this.termTFIDFMap = newTFIDFMap;
    }
    /**
     * I preserved the original sequence of words separated by lines,
     * but now I just need a flatten list of this.
     */
    private void flattenCorpusByLine() {
        for(ArrayList<String> corpusInLine : corpusByLine) {
            corpus.addAll(corpusInLine);
        }
    }

    public LinkedList<String> getCorpus() {
        flattenCorpusByLine();
        return corpus;
    }
    public LinkedList<ArrayList<String>> getCorpusByLine() {
        return this.corpusByLine;
    }
    /**
     * denominator for word probability
     * @return
     */
    public HashMap<String, Double> getWordProbability() {
        HashMap<String, Double> wordProb = new HashMap<String, Double>();
        int wordCountSum = 0;
        for(String t : termFrequency.keySet()) {
            wordCountSum+=termFrequency.get(t);
        }
        double probSum = 0.0;
        for(String t : termFrequency.keySet()) {
            double wProb =(double) termFrequency.get(t) / (double) wordCountSum;
            probSum += wProb;
            wordProb.put(t, probSum);
        }
     //   System.out.println("document word probablity sum = " + probSum);
        return wordProb;
    }
    public void addLMProb(String term, double probability) {
        if(languageModelProb.containsKey(term))
            languageModelProb.put(term, languageModelProb.get(term) + probability);
        else
            languageModelProb.put(term, probability);
    }

    public HashMap<String, Double> getLMProb() {
        return languageModelProb;
    }

    public Double getLMProb(String term) {
        if(!languageModelProb.containsKey(term))
            return null;
        else
            return languageModelProb.get(term);
    }
    /**
     * @return word probablity denominator
     *
     */
    public int getWordCountSum() {
        int wordCountSum = 0;
        for(String t : termFrequency.keySet()) {
            wordCountSum+=termFrequency.get(t);
        }
        return wordCountSum;
    }
    public void setUnigrams(LinkedList<String> grams) {
        this.unigrams = grams;
    }

    public void setBigrams(LinkedList<String> grams) {
        this.bigrams = grams;
    }

    public void setTrigrams(LinkedList<String> grams) {
        this.trigrams = grams;
    }

    public LinkedList<String> getUnigrams() {
        return this.unigrams;
    }

    public LinkedList<String> getBigrams() {
        return this.bigrams;
    }

    public LinkedList<String> getTrigrams() {
        return this.trigrams;
    }

    public String getName() { return this.docName; }

    public void setTermFrequency(HashMap<String, Integer> termFrequency_) {
        this.termFrequency = termFrequency_;

    }

    public void setTermTFIDF(HashMap<String, Double> termTFIDF) {
        this.termTFIDFMap = termTFIDF;

    }

    public LinkedList<Map.Entry<String, Double>> getTopTermsTFIDF(int k) {
        LinkedList<Map.Entry<String, Double>> list =
                new LinkedList<Map.Entry<String, Double>>(termTFIDFMap.entrySet());
        if(k > list.size()) return list;
        Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
            @Override
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                return (o2.getValue().compareTo(o1.getValue()));
            }
        });
        LinkedList<Map.Entry<String, Double>> sublist = new LinkedList<Map.Entry<String, Double>>();
        for(int i=0; i<k; i++) {
           sublist.add(list.get(i));
        }
        return sublist;

    }

    /**
     * Returns a linkedlist that contains a map of <Term, Term_Frequency> for the top K terms ranked by term frequency
     * @param k
     * @return
     */
    public LinkedList<Map.Entry<String, Integer>> getTopTFMap(int k) {
        LinkedList<Map.Entry<String, Integer>> list =
                new LinkedList<Map.Entry<String, Integer>>(termFrequency.entrySet());
          Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return (o2.getValue().compareTo(o1.getValue()));
            }
        });
        LinkedList<Map.Entry<String, Integer>> sublist = new LinkedList<Map.Entry<String, Integer>>();
        for(int i=0; i<k; i++) {
            sublist.add(list.get(i));
        }
        return sublist;
    }

    /**
     * Returns a linkedlist that contains top K terms ranked by term frequency
     * @param k
     * @return
     */
    public LinkedList<String> getTopTFTerms(int k) {
        LinkedList<Map.Entry<String, Integer>> list =
                new LinkedList<Map.Entry<String, Integer>>(termFrequency.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return (o2.getValue().compareTo(o1.getValue()));
            }
        });
        LinkedList<String> sublist = new LinkedList<String>();
        if(k>list.size()) k = list.size();
        for(int i=0; i<k; i++) {
            sublist.add(list.get(i).getKey());
        }
        return sublist;
    }

    public boolean hasBigrams() {
        if(bigrams == null) return false;
        return true;
    }

    public boolean hasTrigrams() {
        if(trigrams == null) return false;
        return true;
    }

    public double getTFIDF(String term) {
        if(termTFIDFMap.containsKey(term)) return termTFIDFMap.get(term);
        else return 0.0;
    }

    /**
     * added 2/17/2014 3:08 am
     * @param k
     * @param ngramType
     * @param termOccurenceDic: (Term, Frequency) from the collection
     * @param threshold: throw away the terms whose frequency is lower than the threshold from the collection
     * @return
     */
    public LinkedList<String> getFirstKGrams(int k, int ngramType, HashMap<String, Integer> termOccurenceDic, int threshold) {
        if(unigrams == null) return new LinkedList<String>();
        LinkedList<String> sublist = new LinkedList<String>();
        if(unigrams.size() < k)
            k = unigrams.size();
        for(int i=0; i<k; i++) {
            sublist.add(unigrams.get(i));
        }
        Tokenizer tokenizer = new Tokenizer();
        if(ngramType == Document.UNIGRAM) return sublist;
        else if(ngramType == Document.BIGRAM) {
            sublist.addAll(tokenizer.generateNramsWrapper(sublist, Document.BIGRAM));
        }
        else {
            LinkedList<String> subgrams = tokenizer.generateNramsWrapper(sublist, Document.BIGRAM);
            subgrams.addAll(tokenizer.generateNramsWrapper(sublist, Document.TRIGRAM));
            subgrams.addAll(sublist);
        }

        LinkedList<String> finalList = new LinkedList<String>();
        for(String term: sublist) {
            if(termOccurenceDic.containsKey(term)) {
                if(termOccurenceDic.get(term) > threshold) {
                    finalList.add(term);
                }
            }
        }
        return finalList;
     }

    public int getTermFrequency(String term) {
        if(termFrequency.containsKey(term))
            return termFrequency.get(term);
        else
            return 0;
    }
    public HashMap<String, Integer> getTermFrequencyMap() {
        return termFrequency;
    }
    public void mergeDocument(Document d) {
        mergeTerms(d.getUnigrams(), Document.UNIGRAM);
        if(d.hasBigrams())
            mergeTerms(d.getBigrams(), Document.BIGRAM);
        if(d.hasTrigrams())
            mergeTerms(d.getTrigrams(), Document.TRIGRAM);
    }

    /**
     * for the sake of quality evaluations
     */
    public void printTerms() {
       System.out.println("******************************" + getName() + "*****************************");
       LinkedList<String> allTerms = getAllGrams();
       for(String term : allTerms) {
           System.out.print(term + ", ");
       }
        System.out.println();
    }

    public static int removeInfrequentTerms(Document doc, int ngramType, HashMap<String, Integer> termOccrurenceDic, int threshold) {
        LinkedList<String> terms = null;
        int removedTerms = 0;
        System.out.println(doc.getName());
        if(ngramType == Tokenizer.UNIGRAM)
            terms = new LinkedList<String>(doc.getUnigrams());
        else if(ngramType == Tokenizer.BIGRAM)
            terms = new LinkedList<String>(doc.getBigrams());
        else if(ngramType == Tokenizer.TRIGRAM)
            terms = new LinkedList<String>(doc.getTrigrams());
        for(String term : terms) {
            if(termOccrurenceDic.containsKey(term)) {
                if(termOccrurenceDic.get(term) < threshold) {
                    doc.removeTerm(ngramType, term);
                    removedTerms++;
                }
            }
            else {
                doc.removeTerm(ngramType, term);
                removedTerms++;

            }
        }
//        doc.printTerms();
        return removedTerms;
    }

    private void mergeTerms(LinkedList<String> anotherGrams, int type) {
        for(String w : anotherGrams) {
            if(termFrequency.containsKey(w))
                termFrequency.put(w, (termFrequency.get(w)+1));
            else {
                termFrequency.put(w, 1);
                if(type == Document.UNIGRAM)
                    unigrams.add(w);
                else if(type == Document.BIGRAM) {
                    if(bigrams == null) bigrams = new LinkedList<String>();
                    bigrams.add(w);
                }
                else if(type == Document.TRIGRAM) {
                    if(trigrams == null) trigrams = new LinkedList<String>();
                    trigrams.add(w);
                }
                else {
                    System.out.println("ERROR: UNKNOWN N-GRAM TYPE");
                    return;
                }
            }
        }
    }



    /**
     * Written in 2/14/2014
     * @param ngramType
     * @param term
     *
     * This is to remove infrequent terms from QueryExpander
     */
    public void removeTerm(int ngramType, String term) {
        if(ngramType == Document.UNIGRAM)
            unigrams.remove(term);
        else if(ngramType == Document.BIGRAM)
            bigrams.remove(term);
        else if(ngramType == Document.TRIGRAM)
            trigrams.remove(term);
        termFrequency.remove(term);

    }




    /**
     * return a list of unigrams, bigrams, trigrams combined
     * The list has to be generated on the fly when the method is called because unigrams / bigrams / trigrams can be altered after the document is constructed
     * @return
     */
    public LinkedList<String> getAllGrams() {
        LinkedList<String> l = new LinkedList<String>();
        if(unigrams != null)
            l.addAll(getUnigrams());
        if(bigrams != null)
           l.addAll(getBigrams());
        if(trigrams != null)
            l.addAll(getTrigrams());
   //     System.out.println(this.docName + ": grams (" + l.size());
        return l;
    }


}

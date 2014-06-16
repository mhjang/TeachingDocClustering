package TermScoring.LanguageModeling;

import Clustering.Document;
import Clustering.DocumentCollection;
import indexing.NGramReader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.*;

/**
 * Created by mhjang on 3/2/14.
 */
public class LanguageModeling {
    NGramReader ng;
    public double unigramModelWeight = 0.7;
    public double bigramModelWeight = 0.2;
    public double trigramModelWeight = 0.1;
    DocumentCollection documentCollection;
    int termWindow = 7;
    int k = 30;
    public LanguageModeling(DocumentCollection dc, int topK, double alpha, double beta) throws IOException {
        ng = new NGramReader();
        this.documentCollection = dc;
        this.k = topK;
        this.unigramModelWeight = alpha;
        this.bigramModelWeight = beta;
        this.trigramModelWeight = 1-(alpha + beta);
    }

    public LanguageModeling(DocumentCollection dc) throws IOException {
        ng = new NGramReader();
        this.documentCollection = dc;

    }
    public static void main(String[] args) throws IOException {
        PrintStream console = System.out;
        File file = new File("log.txt");
        FileOutputStream fos = new FileOutputStream(file);
        PrintStream ps = new PrintStream(fos);
        System.setOut(ps);

     //   LanguageModeling lm = new LanguageModeling();
    //    lm.run();
    }



    private double getCollectionProbability(String term) {
        BigInteger collectionCount = new BigInteger("1024908267229");
        Integer count = ng.lookUpTerm(term);
        double collectionProb = (count.doubleValue()) / (collectionCount.doubleValue());
   //     System.out.println(count + " : " + collectionProb);
        return collectionProb;
    }



    public void selectHighTFTerms() {
        HashMap<String, Document> docSet = documentCollection.getDocumentSet();

        /***
         * Dirichlet smoothing
         */

        int mu = 2000;
        int shiftFactor = 100;
        // slice window
        // taking top k terms
        boolean debugMode = false;
        //      String docName = "lect21.html.txt";
        for (String docName : docSet.keySet()) {
            Document d = docSet.get(docName);
            d.setUnigrams(d.getTopTermsTF2(k));
            d.setBigrams(null);
            d.setTrigrams(null);

        }
    }
    /**
     * 2014/3/23
     * This method computes language modeling score, unigram, bigram and trigram model using google n-gram data as background probability.
     * It combines the three scores with paramter alpha and beta, which are arbitrarily set as 0.7 and 0.2 for now.
     * It takes the top-k terms (or any other way) and UPDATES documents' unigram data to be used for clustering later.
     * More details can be found in /documentation/language_modeling.pdf
     * @throws java.io.IOException
     */
    public void run() throws IOException {
        HashMap<String, Document> docSet = documentCollection.getDocumentSet();

        /***
         * Dirichlet smoothing
         */

        int mu = 2000;
        int shiftFactor = 100;
        // slice window
        // taking top k terms
        boolean debugMode = false;
     //      String docName = "lect21.html.txt";
        for (String docName : docSet.keySet()) {
            if(debugMode) System.out.println(docName);
            Document doc = docSet.get(docName.toLowerCase());

            HashMap<String, Integer> wordFreq = doc.getTermFrequencyMap();
            int D = doc.getWordCountSum();
            LinkedList<String> corpus = doc.getCorpus();
            int len = corpus.size();

            if(debugMode) System.out.println("==================Unigram=======================");
            /**
             * 5 window unigram language modeling
             */
        //    termWindow = 1;
            for (int i = 0; i < len; i += termWindow) {
                int endIdx = (i + termWindow >= len) ? (len) : (i + termWindow);
                List<String> sublist = new LinkedList();
                sublist = corpus.subList(i, endIdx);
                double logprob = 0.0;
                int sublen = sublist.size();
                for (int j = 0; j < sublen; j++) {
                    String token = sublist.get(j);
                    double tokenProb = (wordFreq.get(token) + mu * (getCollectionProbability(token))) / (D + mu);
                    logprob += Math.log(tokenProb);
                }
                for (int j = 0; j < sublen; j++) {
                    doc.addLMProb(sublist.get(j), (logprob + shiftFactor) * unigramModelWeight);
                }
                if(debugMode) System.out.println(sublist + "\t" + logprob);
            }
            if(debugMode) System.out.println("==================Bigram=======================");

            /***
             * Bigram language model
             * segment window size = k
             ***/
    //        termWindow = 2;
            for (int i = 0; i < len; i += termWindow) {
                int endIdx = (i + termWindow >= len) ? (len) : (i + termWindow);
                List<String> sublist = new LinkedList();
                sublist = corpus.subList(i, endIdx);
                double logprob = 0.0;
                int sublen = sublist.size();
                for (int j = 0; j < sublen - 1; j++) {
                    String token1 = sublist.get(j);
                    String token2 = sublist.get(j + 1);
                    String bigram = token1 + " " + token2;
                    /**
                     * log(token2 | token1; d) = c(bigram) + mu * P(token2 | token1; Google Ngram) / (c(token1;d)* mu
                     * P(token2 | token1; Google Ngram) = google_ngram_count(bigram) / google_ngram_count(token1)
                     */
                    double prob_wi_given_w_i_1 = (double) ng.lookUpTerm(bigram) / (double) (ng.lookUpTerm(token1)+1);
                    if (Double.isNaN(prob_wi_given_w_i_1))
                        prob_wi_given_w_i_1 = 0.0;

                    double tokenProb = (wordFreq.get(bigram) + mu * (prob_wi_given_w_i_1)) / (wordFreq.get(token1) + mu);
                    logprob += Math.log(tokenProb);
                }
                // for the first word, assign Unigram model probability
                double firstWordProb = (wordFreq.get(sublist.get(0)) + mu * (getCollectionProbability(sublist.get(0)))) / (D + mu);
                logprob += firstWordProb;

                for (int j = 0; j < sublen; j++) {
                    doc.addLMProb(sublist.get(j), (logprob + shiftFactor) * bigramModelWeight);
                }
                if(debugMode) System.out.println(sublist + "\t" + logprob);
            }

            if(debugMode) System.out.println("==================Trigram=======================");

            /***
             * Trigram language model
             * segment window size = k
             ***/
    //        termWindow = 3;
            for (int i = 0; i < len; i += termWindow) {
                int endIdx = (i + termWindow >= len) ? (len) : (i + termWindow);
                List<String> sublist = new LinkedList();
                sublist = corpus.subList(i, endIdx);
                double logprob = 0.0;
                int sublen = sublist.size();
                for (int j = 0; j < sublen - 2; j++) {
                    String token1 = sublist.get(j);
                    String token2 = sublist.get(j + 1);
                    String token3 = sublist.get(j + 2);
                    String bigram = token1 + " " + token2;
                    String trigram = token1 + " " + token2 + " " + token3;
                    /**
                     * log(token3 | token1, token2; d) = c(trigram; d) + mu * P(token3 | bigram; Google Ngram) / (c(bigram;d)* mu
                     * P(token3 | bigram; Google Ngram) = google_ngram_count(trigram) / google_ngram_count(bigram)
                     */

                    double prob_wi_given_w_i_1_w_i_2 = (double) ng.lookUpTerm(trigram) / (double) ((ng.lookUpTerm(bigram)+1));
                    if (Double.isNaN(prob_wi_given_w_i_1_w_i_2))
                        prob_wi_given_w_i_1_w_i_2 = 0.0;
                    //     System.out.println(token1 + ", " + token2);
                    double tokenProb = (wordFreq.get(trigram) + mu * (prob_wi_given_w_i_1_w_i_2)) / (wordFreq.get(bigram) + mu);
                    logprob += Math.log(tokenProb);
                }
                // processing the first and second words
                String firstWord = null;
                if(sublist.size()>=1) {
                    firstWord = sublist.get(0);
                    double firstWordProb = (wordFreq.get(firstWord) + mu * (getCollectionProbability(firstWord))) / (D + mu);
                    logprob += firstWordProb;
                }
                if(sublist.size() >=2) {
                    String secondWord = sublist.get(1);
                    String bigram = firstWord + " " + secondWord;
                    double prob_wi_given_w_i_1 = (double) ng.lookUpTerm(bigram) / (double) (ng.lookUpTerm(firstWord));
                    if (Double.isNaN(prob_wi_given_w_i_1))
                        prob_wi_given_w_i_1 = 0.0;
                    double secondWordProb = (wordFreq.get(bigram) +
                            mu * (prob_wi_given_w_i_1)) / (wordFreq.get(firstWord) + mu);
                    logprob += secondWordProb;
                }
                // for the first word; unigram probablity
                // for the second word, bigram probability

                if(debugMode) System.out.println(sublist + "\t" + logprob);
                for (int j = 0; j < sublen; j++) {
                    doc.addLMProb(sublist.get(j), (logprob + shiftFactor) * trigramModelWeight);
                }

            }

            HashMap<String, Double> termLMProbMap = doc.getLMProb();
            LinkedList<Map.Entry<String, Double>> entryList = new LinkedList<Map.Entry<String, Double>>(termLMProbMap.entrySet());
            Collections.sort(entryList, new Comparator() {

                @Override
                public int compare(Object o1, Object o2) {
                    Map.Entry<String, Double> e1 = (Map.Entry<String, Double>) o1;
                    Map.Entry<String, Double> e2 = (Map.Entry<String, Double>) o2;

                    return e2.getValue().compareTo(e1.getValue());
                }
            });


            /*****
             * option 1: taking top K terms for the binary representation
             *****/
            int idx = 0;
            LinkedList<String> newUnigrams = new LinkedList<String>();
            if (k > entryList.size())
                k = entryList.size();
            for (Map.Entry<String, Double> e : entryList) {
                if (idx++ > k) break;
                newUnigrams.add(e.getKey());
        //         System.out.println(e.getKey() + "\t " + e.getValue());
            }
            doc.setUnigrams(newUnigrams);
            // because we're testing unigrams arnked by language modeling, set bigrams and trigrams to null
            // so that getAllGrams() only returns unigrams later
            doc.setBigrams(null);
            doc.setTrigrams(null);
        }
    }

    /**
     * 2014/3/23
     * To conclude that language modeling extracts better terms (in terms of clustering accuracy)
     * this sets top K terms sorted by TFIDF score.
     */
    public void TFIDFBaselineRun() {
        HashMap<String, Document> docSet = documentCollection.getDocumentSet();
        for (String docName : docSet.keySet()) {
            LinkedList<String> newUnigrams = new LinkedList<String>();
            Document doc = docSet.get(docName);
            System.out.println(doc.getName());
            LinkedList<Map.Entry<String, Double>> entryList = doc.getTopTermsTFIDF(k);
            for (Map.Entry<String, Double> e : entryList) {
                System.out.println(e.getKey() + "\t" + e.getValue());
                newUnigrams.add(e.getKey());
            }
            doc.setUnigrams(newUnigrams);
            // because we're testing unigrams arnked by language modeling, set bigrams and trigrams to null
            // so that getAllGrams() only returns unigrams later
            doc.setBigrams(null);
            doc.setTrigrams(null);
        }
        }


    private static void printListToLine(ArrayList<String> tokensInLine) {
        for(int i=0; i<tokensInLine.size(); i++) {
            System.out.print(tokensInLine.get(i) + " ");
        }
        System.out.println();
    }

}

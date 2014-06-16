package TermScoring.TFIDF;

import Clustering.Document;
import Clustering.DocumentCollection;
import indexing.NGramReader;
import TeachingDocParser.Tokenizer;

import java.io.IOException;
import java.util.*;

/**
 * Created by mhjang on 1/21/14.
 */
public class TFIDFCalculator {
    public static int BINARYTFIDF = 0;
    public static int LOGTFIDF = 1;
    public static int AUGMENTEDTFIDF = 2;

    public HashSet<String> stopwords = null;
    public HashMap<String, Document> documentSet = null;
    public HashMap<String, Integer> globalTermCountMap = null;
    HashMap<String, Integer> binaryTermFreqInDoc = null;
    NGramReader ngReader;
    private boolean useGoogleNGram = true;
    DocumentCollection dc;
    int TFOption = LOGTFIDF;
    // test
    // contains a parsed text per each document in the given directory
    private HashMap<String, String> documentTextMap = new HashMap<String, String>();


    /**
     *
     * @param useGoogleNGram: whether we want to use google n-gram data as a collection probability; useful when your collection is small
     * @throws java.io.IOException
     */
    public TFIDFCalculator(boolean useGoogleNGram) throws IOException {
        this.useGoogleNGram = useGoogleNGram;
    }

    public void calulateTFIDF(int TFOption, String dir, int ngram, boolean wikifiltering) {
        this.TFOption = TFOption;
        try {
            Tokenizer tokenizer = new Tokenizer();
            documentSet = tokenizer.tokenize(dir, wikifiltering, ngram);
            dc = runTFIDF(TFOption);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /***
     *
     * @return a collection of document set with tokenized n-grams and term frequency
     */
    public DocumentCollection getDocumentCollection() throws IOException {
        if(dc == null) System.out.println("Document Collection is not generated! call calulateTFIDF() ");
        return dc;
     }


    /**
     * compute TFIDF score of the terms in a given doc (1st arg) given the document collection distribution (2nd arg)
     * @param doc
     * @param dc
     * @param TFIDFOption
     */
    public static void calculateTFIDFGivenCollection(Document doc, DocumentCollection dc, int TFIDFOption) {
        calculateTFIDFGivenDistribution(TFIDFOption, dc.getglobalTermCountMap(), doc, dc.getBinaryTermFreqInDoc(), false);
    }
    /**
     *
     * @param TFIDFOption  = {BINARYTFIDF, LOGTFIDF, AUGMENTEDTFIDF}
     */
    private DocumentCollection runTFIDF(int TFIDFOption) throws IOException {

        /*****************************************************************************************************
         * Counting
         * (1) term frequencies in a document (docTFMap),
         * (2) # of documents that the term appears (binaryTermFreqInDoc),
         * (3) # of term counts in the collection (globalTermCountMap)
         ****************************************************************************************************/

       // the number of documents that the term appears
       binaryTermFreqInDoc = new HashMap<String, Integer>();
       // stores term frequency over the collection
        globalTermCountMap = new HashMap<String, Integer>();
       // count term frequencies
       for(String docName : documentSet.keySet()) {
            Document doc = documentSet.get(docName);
            HashMap<String, Integer> docTFMap = doc.getTermFrequencyMap();
            for(String term : docTFMap.keySet()) {
                if(globalTermCountMap.containsKey(term)) {
                    globalTermCountMap.put(term, globalTermCountMap.get(term) + docTFMap.get(term));
                }
                else {
                    globalTermCountMap.put(term, docTFMap.get(term));
                }
                if(binaryTermFreqInDoc.containsKey(term)) {
                    binaryTermFreqInDoc.put(term, binaryTermFreqInDoc.get(term) + 1);
                }
                else {
                    binaryTermFreqInDoc.put(term, 1);
                }
            }
        }


        /**************************************************
         *               Calculating TF * IDF
         **************************************************/
        if(this.useGoogleNGram) {
             ngReader = new NGramReader();
        }
        for(String docName : documentSet.keySet()) {
            Document doc = documentSet.get(docName);
            HashMap<String, Integer> termFreqMap = doc.getTermFrequencyMap();
            HashMap<String, Double> termTFIDFMap = new HashMap<String, Double>();

            for(String term : termFreqMap.keySet()) {
                double tf = 0.0;
                if(TFIDFOption == TFIDFCalculator.BINARYTFIDF) {
                    if(termFreqMap.containsKey(term))
                        tf = 1.0;
                }
                else if (TFIDFOption == TFIDFCalculator.LOGTFIDF)
                    tf = Math.log(termFreqMap.get(term) + 1);
                else if (TFIDFOption == TFIDFCalculator.AUGMENTEDTFIDF) {
                    int maxFrequency = 0;
                    LinkedList<String> termKeySet = new LinkedList<String>(termFreqMap.keySet());
                    for(String t : termKeySet) {
                        if(termFreqMap.get(t) > maxFrequency)
                            maxFrequency = termFreqMap.get(t);
                    }
                    tf = 0.5 + 0.5 * ((double)(termFreqMap.get(term)) / (double)(maxFrequency));
                }
                double idf;
                if(!this.useGoogleNGram) {
                    if (binaryTermFreqInDoc.containsKey(term))
                        idf = Math.log((double) documentSet.size() / (double) binaryTermFreqInDoc.get(term));
                    else
                        idf = 0.0;
                }
                else {
                    idf = Math.log(ngReader.termCollectionProbability(term));
                }
                double tfidf = tf * idf;
                termTFIDFMap.put(term, tfidf);
            }
            doc.setTermTFIDF(termTFIDFMap);
        }
        DocumentCollection docCol = new DocumentCollection(documentSet, globalTermCountMap, binaryTermFreqInDoc);
        return docCol;
    }


    private void filterTermsInList(Document doc, int ngramType, int termDocumentFrequency, LinkedList<String> ngrams) {
        for(String term: ngrams) {
            if(binaryTermFreqInDoc.get(term) < termDocumentFrequency) {
                doc.removeTerm(ngramType, term);
 //               System.out.println("removing "+ term);
            }
        }
    }
    /**
     *
     * @param termDocumentFrequency: a threshold of # of documents that a term has to appear to be not dropped
     */
    private void filterInfrequentTerms(int termDocumentFrequency, int nGramType) {
        for(String docName : documentSet.keySet()) {
            Document doc = documentSet.get(docName);
            if(nGramType == Tokenizer.UNIGRAM) {
                filterTermsInList(doc, Tokenizer.UNIGRAM, termDocumentFrequency, new LinkedList<String>(doc.getUnigrams()));
            }
            else if(nGramType == Tokenizer.BIGRAM) {
                filterTermsInList(doc, Tokenizer.UNIGRAM, termDocumentFrequency, new LinkedList<String>(doc.getUnigrams()));
                filterTermsInList(doc, Tokenizer.BIGRAM, termDocumentFrequency, new LinkedList<String>(doc.getBigrams()));
            }
            else if(nGramType == Tokenizer.TRIGRAM) {
                filterTermsInList(doc, Tokenizer.UNIGRAM, termDocumentFrequency, new LinkedList<String>(doc.getUnigrams()));
                filterTermsInList(doc, Tokenizer.BIGRAM, termDocumentFrequency, new LinkedList<String>(doc.getBigrams()));
                filterTermsInList(doc, Tokenizer.TRIGRAM, termDocumentFrequency, new LinkedList<String>(doc.getTrigrams()));
            }

        }
    }

    /**
     * This is a more generic method that takes a term frequency and binary term frequency, and calculate TFIDF score for a given document
     * @param TFType
     * @param globalMap
     * @param doc
     * @param binaryTermFreqInDoc
     * @param printTerms
     * @return
     */

    private static LinkedList<TermTFIDF> calculateTFIDFGivenDistribution(int TFType, HashMap<String, Integer> globalMap, Document doc, HashMap<String, Integer> binaryTermFreqInDoc, boolean printTerms) {
        int totalDocs = globalMap.keySet().size();
        LinkedList<TermTFIDF> scoredTerms = new LinkedList<TermTFIDF>();
        LinkedList<String> terms = new LinkedList<String>();
        HashMap<String, Integer> termFreqMap = doc.getTermFrequencyMap();
        HashMap<String, Double> termTFIDFMap = new HashMap<String, Double>();

        for(String term : termFreqMap.keySet()) {
            double tf = 0.0;
            if(TFType == TFIDFCalculator.BINARYTFIDF) {
                 if(termFreqMap.containsKey(term))
                      tf = 1.0;
             }
            else if (TFType == TFIDFCalculator.LOGTFIDF)
                tf = Math.log(termFreqMap.get(term) + 1);
            else if (TFType == TFIDFCalculator.AUGMENTEDTFIDF) {
                int maxFrequency = 0;
                LinkedList<String> termKeySet = new LinkedList<String>(termFreqMap.keySet());
                for(String t : termKeySet) {
                    if(termFreqMap.get(t) > maxFrequency)
                        maxFrequency = termFreqMap.get(t);
                }
                tf = 0.5 + 0.5 * ((double)(termFreqMap.get(term)) / (double)(maxFrequency));
            }
            double idf;
            if(binaryTermFreqInDoc.containsKey(term))
                idf = Math.log(globalMap.keySet().size() / binaryTermFreqInDoc.get(term));
            else
                idf = 0.0;
            double tfidf = tf * idf;
            scoredTerms.add(new TermTFIDF(term, tfidf));
            termTFIDFMap.put(term, tfidf);
        }

        doc.setTermTFIDF(termTFIDFMap);

      // I commented it out because I don't need a ranked list for now to save the running time.
     /*   Collections.sort(scoredTerms, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                TermTFIDF t1 = (TermTFIDF) (o1);
                TermTFIDF t2 = (TermTFIDF) (o2);

                return t1.score.compareTo(t2.score);
            }
        });
        if(printTerms) {
            for(TermTFIDF st : scoredTerms) {
                System.out.println(st.term + " : " + st.score);
             }
        }
        return scoredTerms;
       */
        return scoredTerms;
    }



    /**
     * generate bigrams
     * @param wordlist
     * @return
     */
    private LinkedList<String> getBigrams(ArrayList<String> wordlist) {
  //    uncomment if you want to generate *UPTO* bigrams in addition to unigrams (this line includes unigrams)
  //     LinkedList<String> wordPool = new LinkedList<String>(wordlist);
        LinkedList<String> wordPool = new LinkedList<String>();
        if(wordlist.size() < 2) {
            System.out.println("There is only one word in the list");
            return wordPool;
        }

        String bigram = wordlist.get(0) + " " + wordlist.get(1);
        wordPool.add(bigram);
        int length = wordlist.size();
        for(int i=2; i<length; i++) {
            bigram = wordlist.get(i-1) + " " + wordlist.get(i);
            wordPool.add(bigram);
        }
        return wordPool;
    }

    /**
     * generate trigrams
     * @param wordlist
     * @return
     */
    private LinkedList<String> getTrigrams(ArrayList<String> wordlist) {
   //    uncomment star-marked (*) lines if you want to generate *UPTO* trigrams in addition to unigrams (this line includes unigrams)
   //*     LinkedList<String> wordPool = new LinkedList<String>(wordlist);
        LinkedList<String> wordPool = new LinkedList<String>();
        if(wordlist.size() < 3) {
            System.out.println("There is less than 3 words in the list; can't build trigrams");
            return getBigrams(wordlist);
        }
        String bigram = wordlist.get(0) + " " + wordlist.get(1);
    //*    wordPool.add(bigram);
        String prevBigram, trigram;
        for(int i=2; i<wordlist.size(); i++) {
            prevBigram = bigram;
            bigram = wordlist.get(i-1) + " " + wordlist.get(i);
            trigram = prevBigram + " " + wordlist.get(i);
    //*     wordPool.add(bigram);
            wordPool.add(trigram);
         }
        return wordPool;
    }


    static class TermTFIDF {
        String term;
        Double score;
        public TermTFIDF(String term_, Double score_) {
            term = term_;
            score = score_;
        }

    }


}

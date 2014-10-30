package Clustering;

import Classify.liblinear.FeatureExtractor;
import Classify.liblinear.SVMClassifier;
import Classify.noisegenerator.TableGenerator;
import Clustering.KMeans.KMeansClustering;
import Similarity.CosineSimilarity;
import TermScoring.LanguageModeling.LanguageModeling;
import evaluation.ClusteringFMeasure;
import org.lemurproject.galago.core.parse.stem.KrovetzStemmer;
import TeachingDocParser.StopWordRemover;
import TermScoring.TFIDF.TFIDFCalculator;
import TeachingDocParser.Tokenizer;

import java.io.*;
import java.util.*;

/**
 * Created by mhjang on 1/28/14.
 * Tuesday 10:30pm
 */
public class Clustering {
    private String DUMMY = "dummy";
    HashSet<String> documentFirstKTerms;
    DocumentCollection dc;

    HashMap<String, Document> clusterFeatureMap;

    public static void main(String[] args) throws IOException {
        // redirecting a system output to a file
        PrintStream console = System.out;
        File file = new File("noise_20.txt");
        FileOutputStream fos = new FileOutputStream(file);
        PrintStream ps = new PrintStream(fos);
  //      System.setOut(ps);

        /***
         * Setting the parameters
         */
        int firstTopK = 100;
        double clusteringThreshold = 0.02;
        int infrequentTermThreshold = 0;
        int kgramsTermThreshold = 1;

        double[] alpha = {0.1, 0.2, 0.3, 0.4};
        double beta = 0.5;


        // parameter: whether to use Google N-gram
        TFIDFCalculator tfidf = new TFIDFCalculator(true);
        SVMClassifier svm = new SVMClassifier();
        String baseDir = "/Users/mhjang/Desktop/clearnlp/acl/training/";

        svm.runFiveFoldCrossValidation(baseDir, false, true, FeatureExtractor.LINE_BASED);
        TableGenerator t = svm.getTableGenerator();
   //     TableGenerator t= new TableGenerator();
        System.out.println("Table size: " + t.getSize());
        TFIDFCalculator.noiseRatio = 0.2;
        tfidf.calulateTFIDFWithNoise(TFIDFCalculator.LOGTFIDF, "/Users/mhjang/Documents/teaching_documents/extracted/dataset/applydataset/goldstandard/", Tokenizer.UNIGRAM, false, t);
   //     tfidf.calulateTFIDF(TFIDFCalculator.LOGTFIDF, "/Users/mhjang/Documents/teaching_documents/extracted/dataset/applydataset/annotation/", Tokenizer.UNIGRAM, false);

        DocumentCollection dc = tfidf.getDocumentCollection();
 //       DocumentCollection dc = tfidf.getDocumentCollection("/Users/mhjang/Documents/teaching_documents/extracted/stemmed/parsed/noise_removed", Tokenizer.TRIGRAM, false);

        System.out.println("Documents features ready");
        HashMap<String, Document> documentMap = dc.getDocumentSet();
        HashMap<String, Integer> termOccurrenceDic = dc.getglobalTermCountMap();


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


        /**
         * cleaning the document terms by dropping a bunch of infrequent bigrams and trigrams
         * If NullPointException occurs, that's probably because above language modeling methods nullfied bigrams and trigrams
         */
        //  clustering.documentTermCleansing(documentMap);

        /**
         *  Setting documents' first K term words
         *  Taking the first K terms from each document, and keep the pool of terms
         *  Intended to remove all terms that have not occurred within the first K terms in any of the documents
         */
         /*
            System.out.println("Kgrams : " + kgramsTermThreshold);
            HashSet<String> firstKWords = new HashSet<String>();
            for(String docID : documentMap.keySet()) {
                Document doc = documentMap.get(docID);
                LinkedList<String> kGrams = doc.getFirstKGrams(firstTopK, Document.BIGRAM, termOccurrenceDic, kgramsTermThreshold);
                firstKWords.addAll(kGrams);
            }
            clustering.documentFirstKTerms = firstKWords;
        */

        //   String[] topics = {"list and array representation", "graph traverse", "sorting algorithm"};
        // added this, because when topic names are long.. it's hard to recognize the cluster and the gold standard cluster name when parsing
        // It's simpler if I use a separate label index by line
        Integer clusterLabelIndex = 0;
        HashMap<String, Integer> clusterLabelMap = new HashMap<String, Integer>();
        // reading a topic file
        BufferedReader br = new BufferedReader(new FileReader(new File("./goldstandard/topics_v2_stemmed")));
        ArrayList<String> topiclist = new ArrayList<String>();
        String line = null;
        while ((line = br.readLine()) != null) {
            topiclist.add(line);
            clusterLabelMap.put(line, clusterLabelIndex++);
        }
//        clusterLabelMap.put("dummy", clusterLabelIndex);
//        topiclist.add("dummy");
   //     Clustering clustering = new Clustering(dc);
  //      clustering.naiveAssignmentLazyUpdate(documentMap, topiclist, infrequentTermThreshold, clusteringThreshold);




         KMeansClustering kmeans = new KMeansClustering(topiclist, dc);
        HashMap<String, LinkedList<String>> clusters = kmeans.convertToTopicMap(kmeans.clusterRun(10, 0.05));
     //  HashMap<String, LinkedList<String>> clusters = kmeans.convertToTopicMap(kmeans.clusterRunWithSignatureVector(10, 0.05, dc.constructSignatureVector(25)));
        ClusteringFMeasure cfm = new ClusteringFMeasure(clusters, clusterLabelMap, topiclist, "./goldstandard/goldstandard_v2.csv", dc);
        System.out.println("Original");

     //   cfm.computeAccuracy();
        cfm.compAccuracyOnlyItemsInGold();
        System.out.println();
        System.out.println();


/*
        TFIDFCalculator tfidf2 = new TFIDFCalculator(true);
        TFIDFCalculator.noiseRatio = 0.3;
        tfidf2.calulateTFIDF(TFIDFCalculator.LOGTFIDF, "/Users/mhjang/Documents/teaching_documents/extracted/dataset/applydataset/output/", Tokenizer.UNIGRAM, false);
        DocumentCollection dc2 = tfidf2.getDocumentCollection();
        //       DocumentCollection dc = tfidf.getDocumentCollection("/Users/mhjang/Documents/teaching_documents/extracted/stemmed/parsed/noise_removed", Tokenizer.TRIGRAM, false);

        documentMap = dc2.getDocumentSet();
        termOccurrenceDic = dc2.getglobalTermCountMap();

        LanguageModeling lm2 = new LanguageModeling(dc2, 30, 0.7, 0.2);
//        lm.run();
        lm2.selectHighTFTerms();
        Clustering clustering2 = new Clustering(dc2);
        KMeansClustering kmeans2 = new KMeansClustering(topiclist, dc2);
        HashMap<String, LinkedList<String>> clusters2 = kmeans2.convertToTopicMap(kmeans2.clusterRun(10, 0.05));
  //      HashMap<String, LinkedList<String>> clusters2 = kmeans2.convertToTopicMap(kmeans2.clusterRunWithSignatureVector(10, 0.05, dc2.constructSignatureVector(25)));

        ClusteringFMeasure cfm2 = new ClusteringFMeasure(clusters2, clusterLabelMap, topiclist, "./goldstandard/goldstandard_v2.csv", dc2);
        System.out.println("Original" );
        cfm2.compAccuracyOnlyItemsInGold();
  //     cfm2.computeAccuracy();
   //     cfm2.errorAnalysis(clusters, clusters2);
   //    kmeans2.printCluster(clusters, topiclist);
   //     kmeans2.printCluster(clusters2, topiclist);

    //    cfm2.errorAnalysis(clusters, clusters2);
   //     kmeans2.printCluster(clusters, topiclist);
  //      kmeans2.printCluster(clusters2, topiclist);
        //      HashMap<String, LinkedList<String>> clusters = kmeans.convertToTopicMap(kmeans.clusterRunWithSignatureVector(10, 0.05, dc.constructSignatureVector(25)));
        //       cfm.computeAccuracy();


        //       kmeans.printCluster(clusters, topiclist);



  /*          HashMap<String, LinkedList<String>> clusters = clustering.naiveAssignmentLazyUpdate(documentMap, topiclist, infrequentTermThreshold, clusteringThreshold);
            //  HashMap<String, LinkedList<String>> clusters = clustering.naiveAssignmentLazyUpdateDuplicate(documentMap, topiclist, 0, 0.05);

            ClusteringFMeasure cfm = new ClusteringFMeasure(clusters, clusterLabelMap, topiclist, "./goldstandard/goldstandard_v2.csv", dc);

            cfm.computeAccuracy(); */
//            HashMap<String, LinkedList<String>> clustersInGold = cfm.compAccuracyOnlyItemsInGold();
//            clustering.printCluster(clustersInGold, topiclist);

       //     cfm.analyzeCodeRemovedPerCluster(topiclist);

       //   HashMap<String, LinkedList<String>> clusters= clustering.naiveAssignmentFirstRandomAssign(documentMap, topiclist);


/*
        TFIDFCalculator tfidf3 = new TFIDFCalculator(true);
        tfidf3.calulateTFIDF(TFIDFCalculator.LOGTFIDF, "/Users/mhjang/Documents/teaching_documents/extracted/dataset/applydataset/goldstandard_noiseremoved/", Tokenizer.UNIGRAM, false);
        DocumentCollection dc3 = tfidf3.getDocumentCollection();
        //       DocumentCollection dc = tfidf.getDocumentCollection("/Users/mhjang/Documents/teaching_documents/extracted/stemmed/parsed/noise_removed", Tokenizer.TRIGRAM, false);

        documentMap = dc3.getDocumentSet();
        termOccurrenceDic = dc3.getglobalTermCountMap();

        LanguageModeling lm3 = new LanguageModeling(dc3, 30, 0.7, 0.2);
//        lm.run();
        lm3.selectHighTFTerms();
        Clustering clustering3 = new Clustering(dc3);
        KMeansClustering kmeans3 = new KMeansClustering(topiclist, dc3);
        HashMap<String, LinkedList<String>> clusters3 = kmeans3.convertToTopicMap(kmeans3.clusterRun(10, 0.05));
        //      HashMap<String, LinkedList<String>> clusters2 = kmeans2.convertToTopicMap(kmeans2.clusterRunWithSignatureVector(10, 0.05, dc2.constructSignatureVector(25)));

        ClusteringFMeasure cfm3 = new ClusteringFMeasure(clusters3, clusterLabelMap, topiclist, "./goldstandard/goldstandard_v2.csv", dc3);
        System.out.println("After Noise Algorithm removal ");
        //      cfm2.compAccuracyOnlyItemsInGold();
        cfm3.computeAccuracy();
        */

        }
  //  }


    public Clustering(DocumentCollection dc) {
        this.dc = dc;
    }
    /**
     * Written 2/17/2014 8:34 PM
     *
     * After all "Document" objects are created, we have to filter out some noisy terms with term frequency dictionary obtained from the collection.
     * This method removes bigrams and trigrams that do not appear more than threshold k times from the collection for all documents.
     *
     */
    private void documentTermCleansing(HashMap<String, Document> documentMap) {
        for(String docID : documentMap.keySet()) {
            Document doc = documentMap.get(docID);
            int removedTerms = 0;
//            removedTerms += Document.removeInfrequentTerms(doc, Document.UNIGRAM, termOccurrenceDic, threshold);
            removedTerms += Document.removeInfrequentTerms(doc, Tokenizer.BIGRAM, dc.getglobalTermCountMap(), 10);
            removedTerms += Document.removeInfrequentTerms(doc, Tokenizer.TRIGRAM, dc.getglobalTermCountMap(), 5);
            System.out.println("removed terms: " + docID + ": " + removedTerms);
        }
    }



    private void stemming(String path) throws IOException {
        File fileEntry = new File(path);
        LineNumberReader reader = new LineNumberReader(new FileReader(fileEntry));
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(fileEntry.getName()+"_stemmed")));
        String line;
        KrovetzStemmer stemmer = new KrovetzStemmer();
        try {
            line = reader.readLine();
            while (line != null) {
                line = line.trim();
                String [] tokens = line.split(" ,");
                for( int i=0 ; i<tokens.length ; i++ ) {
                    String stem = stemmer.stem(tokens[i]);
                    System.out.println(tokens[i] + " " + stem);
                    bw.write(stem + " ");
                }
                bw.write("\n");
                bw.flush();
                line = reader.readLine();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        bw.close();
    }




    /**
     *
     * @param documentMap
     * @param topics
     * @throws java.io.IOException
     */
    public HashMap<String, LinkedList<String>> naiveAssignmentFirstRandomAssign(HashMap<String, Document> documentMap, ArrayList<String> topics) {

        try {
            AbstractMap.SimpleEntry <HashMap<String, LinkedList<String>>, HashMap<String, Document>> entry = (AbstractMap.SimpleEntry) convertTopicToDocument(topics);
            HashMap<String, LinkedList<String>> clusters = entry.getKey();
            HashMap<String, Document> clusterFeatureMap = entry.getValue();

//            clusterFeatureMap = expandTopicQueries(clusterFeatureMap, "./wikiexpansion_resource/stemmed/");

            /****
             * clustering
             */
            for(String docID : documentMap.keySet()) {
                Document document = documentMap.get(docID);
                double sim = 0.0, maxSim = 0.0;
                String bestTopic = null;
                for(String clusterTopic : clusterFeatureMap.keySet()) {
                    Document clusterDoc = clusterFeatureMap.get(clusterTopic);
                    sim = CosineSimilarity.CosineSimilarity(document, clusterDoc);
                    if(sim > maxSim) {
                        maxSim = sim;
                        bestTopic = clusterTopic;
                    }
                }
                if(bestTopic == null) {
                    // similarity to all existing cluster labels was 0
                    bestTopic = DUMMY;
                }
          //      System.out.println(bestTopic);
                LinkedList<String> clusterList = clusters.get(bestTopic);
                clusterList.add(docID);
                clusters.put(bestTopic, clusterList);

                // updating cluster features
                if(bestTopic != DUMMY) {
                    Document clusterDoc = clusterFeatureMap.get(bestTopic);
                    clusterDoc.mergeDocument(document);
                }
            }

            /***
             * print the clustering result
             */

      //      printCluster(clusters, topics);
            return clusters;

        }catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * Written in in 2/2 3:23 pm
     * assign a set of documents to the cluster whose similarity score to is the maximum at one phase, and update the cluster features.
     * Then come back to the "dummy" clusters, and deal with the unassigned documents until it converges or there is no document to be assigned.
     *
     * @param documentMap
     * @param topics
     * @param clusterThreshold
     *@param termFilterThreshold @throws java.io.IOException
     */
    public HashMap<String, LinkedList<String>> naiveAssignmentLazyUpdate(HashMap<String, Document> documentMap, ArrayList<String> topics, int termFilterThreshold, double clusterThreshold) throws IOException {
        AbstractMap.SimpleEntry <HashMap<String, LinkedList<String>>, HashMap<String, Document>> entry = (AbstractMap.SimpleEntry) convertTopicToDocument(topics);
        HashMap<String, LinkedList<String>> clusters = entry.getKey();
        clusterFeatureMap = entry.getValue();

        /***
         * Query Expansion
         */
   //     QueryExpander qe = new QueryExpander(dc);
    //    qe.expandTopicQueriesWithFrequentTerms(clusterFeatureMap, "/Users/mhjang/Documents/workspace/TeachingTest/wikiexpansion_resource/ver2/html", dc.getglobalTermCountMap(), termFilterThreshold);
   //    qe.expandTopicQueriesWithFirstKTerms(clusterFeatureMap, "./wikiexpansion_resource/ver2/html", documentFirstKTerms);

        /****
         * clustering
         */
        LinkedList<String> dummyCluster = clusters.get(DUMMY);
        dummyCluster.addAll(documentMap.keySet());
        boolean isConverged = false, finished = false;
        while(!isConverged && !finished) {
            // since we can't modify the list while enumerating, save the list of items we're moving, and delete them after the loop
            LinkedList<String> documentMoved = new LinkedList<String>();
            HashMap<String, LinkedList<String>> clusterUpdate = initializeTopicCluster(topics);
            isConverged = true;
            for(String docID : dummyCluster) {
                Document document = documentMap.get(docID);
                double sim = 0.0, maxSim = 0.0;
                String bestTopic = null;
                for(String clusterTopic : clusterFeatureMap.keySet()) {
                    if(clusterTopic == DUMMY) continue;
                    Document clusterDoc = clusterFeatureMap.get(clusterTopic);
                //    sim = CosineSimilarity.BinaryCosineSimilarity(document, clusterDoc);
                    sim = CosineSimilarity.TFIDFCosineSimilarity(document, clusterDoc);

                    if(sim > maxSim) {
                        maxSim = sim;
                        bestTopic = clusterTopic;
                    }
                }
           //     System.out.println("maxsim = "+ maxSim);
                if(bestTopic != null && maxSim > clusterThreshold) {
                    LinkedList<String> clusterList =  clusterUpdate.get(bestTopic);
                    clusterList.add(docID);
                     clusterUpdate.put(bestTopic, clusterList);
                    isConverged = false;
                }
            }
            int numOfDocMoved = 0;
            int numOfDummyDocs = dummyCluster.size();
            // cluster feature update
            for(String clusterTopic :  clusterUpdate.keySet()) {
                LinkedList<String> clusterList = clusters.get(clusterTopic);
                LinkedList<String> clusterListToAttach =  clusterUpdate.get(clusterTopic);
                // removing the documents from the dummy cluster that were just assigned to clusters
                dummyCluster.removeAll(clusterListToAttach);
                numOfDocMoved += clusterListToAttach.size();
                // updating the cluster (1) by adding the documents and (2) updating the features
                clusterList.addAll(clusterListToAttach);
                Document clusterDoc = clusterFeatureMap.get(clusterTopic);
                for(String docID : clusterListToAttach)
                    clusterDoc.mergeDocument(documentMap.get(docID));
                clusterFeatureMap.put(clusterTopic, clusterDoc);
            }
            clusters.put(DUMMY, dummyCluster);
            if(dummyCluster.isEmpty()) finished = true;
   //         System.out.println("# of dummy Docs: " + numOfDummyDocs +", # of documents moved: " + numOfDocMoved);
        }
        printCluster(clusters, topics);
        return clusters;
   }


    public HashMap<String, Document> getClusterFeatures() {
        return clusterFeatureMap;
    }

    /**
     * Written in in 2/2 3:23 pm
     * assign a set of documents to the cluster whose similarity score to is the maximum at one phase, and update the cluster features.
     * Then come back to the "dummy" clusters, and deal with the unassigned documents until it converges or there is no document to be assigned.
     *
     * @param documentMap
     * @param topics
     * @param clusterThreshold
     *@param termFilterThreshold @throws java.io.IOException
     */
    public HashMap<String, LinkedList<String>> naiveAssignmentLazyUpdateDuplicate(HashMap<String, Document> documentMap, ArrayList<String> topics, int termFilterThreshold, double clusterThreshold) throws IOException {
        AbstractMap.SimpleEntry <HashMap<String, LinkedList<String>>, HashMap<String, Document>> entry = (AbstractMap.SimpleEntry) convertTopicToDocument(topics);
        HashMap<String, LinkedList<String>> clusters = entry.getKey();
        HashMap<String, Document> clusterFeatureMap = entry.getValue();

        /***
         * Query Expansion
         */
  //      QueryExpander qe = new QueryExpander(dc);
  //      qe.expandTopicQueriesWithFrequentTerms(clusterFeatureMap, "./wikiexpansion_resource/ver2/html", dc.getglobalTermCountMap(), termFilterThreshold);

        /****
         * clustering
         */
        LinkedList<String> dummyCluster = clusters.get(DUMMY);
        dummyCluster.addAll(documentMap.keySet());
        boolean isConverged = false, finished = false;
        while(!isConverged && !finished) {
            // since we can't modify the list while enumerating, save the list of items we're moving, and delete them after the loop
            LinkedList<String> documentMoved = new LinkedList<String>();
            HashMap<String, LinkedList<String>> clusterUpdate = initializeTopicCluster(topics);
            isConverged = true;
            for(String docID : dummyCluster) {
                Document document = documentMap.get(docID);
                double sim = 0.0, maxSim = 0.0;
                String bestTopic = null;
                for(String clusterTopic : clusterFeatureMap.keySet()) {
                    if(clusterTopic == DUMMY) continue;
                    Document clusterDoc = clusterFeatureMap.get(clusterTopic);
                    sim = CosineSimilarity.BinaryCosineSimilarity(document, clusterDoc);
                    if(sim > clusterThreshold) {
                        LinkedList<String> clusterList =  clusterUpdate.get(clusterTopic);
                        clusterList.add(docID);
                        clusterUpdate.put(clusterTopic, clusterList);
                        isConverged = false;
                    }
                  }
            }
            int numOfDocMoved = 0;
            int numOfDummyDocs = dummyCluster.size();
            // cluster feature update
            for(String clusterTopic :  clusterUpdate.keySet()) {
                LinkedList<String> clusterList = clusters.get(clusterTopic);
                LinkedList<String> clusterListToAttach =  clusterUpdate.get(clusterTopic);
                // removing the documents from the dummy cluster that were just assigned to clusters
                dummyCluster.removeAll(clusterListToAttach);
                numOfDocMoved += clusterListToAttach.size();
                // updating the cluster (1) by adding the documents and (2) updating the features
                clusterList.addAll(clusterListToAttach);
                Document clusterDoc = clusterFeatureMap.get(clusterTopic);
                for(String docID : clusterListToAttach)
                    clusterDoc.mergeDocument(documentMap.get(docID));
                clusterFeatureMap.put(clusterTopic, clusterDoc);
            }
            clusters.put(DUMMY, dummyCluster);
            if(dummyCluster.isEmpty()) finished = true;
      //      System.out.println("# of dummy Docs: " + numOfDummyDocs +", # of documents moved: " + numOfDocMoved);
        }
        printCluster(clusters, topics);
        return clusters;
    }



    public void naiveAssignmentSortByPurity(HashMap<String, Document> documentMap, ArrayList<String> topics) throws IOException {

        AbstractMap.SimpleEntry <HashMap<String, LinkedList<String>>, HashMap<String, Document>> entry = (AbstractMap.SimpleEntry) convertTopicToDocument(topics);
        HashMap<String, LinkedList<String>> clusters = entry.getKey();
        HashMap<String, Document> clusterFeatureMap = entry.getValue();

        /****
         * clustering
         */
        // for each topic find the best document at each phase

        /***
         * print the clustering result
         */
        printCluster(clusters, topics);

    }


    public void printCluster(HashMap<String, LinkedList<String>> clusters, ArrayList<String> topics) {
        for(String topic : topics) {
            LinkedList<String> cluster = clusters.get(topic);
            System.out.println(topic + ":" + cluster.toString());
            System.out.println();
        }
//        System.out.println(clusters.get(DUMMY).toString());
    }


    /**
     * 2014/2/2 3:05 pm
     * Made a separate method for this. Let's try not to duplicate any code component..
     * @param topics
     * @return
     * @throws java.io.IOException
     */
    public Map.Entry<HashMap<String, LinkedList<String>>, HashMap<String, Document>> convertTopicToDocument(ArrayList<String> topics) throws IOException {
        // initializing the cluster dictionary
        HashMap<String, LinkedList<String>> clusters = initializeTopicCluster(topics);
        /***
         *  constructing tokens for topic queries
         *  data structure and list --> "data" "structure" "and" "list"
         */
        HashMap<String, Document> clusterFeatureMap = new HashMap<String, Document>();
        StopWordRemover swr = new StopWordRemover();
        for(String topic: topics) {
            String[] tokens = topic.split("[:, ]");
            tokens = swr.removeStopWords(tokens);
            // using hashset to remove duplicate words
            // constructing the initial set of feature words and their frequency of the cluster
            LinkedList<String> topicTokens = new LinkedList<String>();
            HashMap<String, Integer> topicTokensFreq = new HashMap<String, Integer>();
            for(String token : tokens) {
                if(token.trim().length() > 0) {
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
            Document topicDoc = new Document(topicTokens);
            topicDoc.setTermFrequency(topicTokensFreq);
            TFIDFCalculator.calculateTFIDFGivenCollection(topicDoc, dc, TFIDFCalculator.LOGTFIDF);
   //         for(String t : topicDoc.termTFIDFMap.keySet()) {
   //             System.out.print(t + ": " + topicDoc.getTFIDF(t));
    //        }
   //         System.out.println();
            clusterFeatureMap.put(topic, topicDoc);
        }
        // I know this is an abuse of "Entry" for using a tuple.. but it works!
        return new AbstractMap.SimpleEntry<HashMap<String, LinkedList<String>>, HashMap<String, Document>>(clusters, clusterFeatureMap);
    }

    private HashMap<String, LinkedList<String>> initializeTopicCluster(ArrayList<String> topics) {
        HashMap<String, LinkedList<String>> clusters = new HashMap<String, LinkedList<String>>();
        for(String t : topics) {
            clusters.put(t, new LinkedList<String>());
        }
        clusters.put(DUMMY, new LinkedList<String>());
        return clusters;
    }
}

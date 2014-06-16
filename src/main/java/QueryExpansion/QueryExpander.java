package QueryExpansion;

import Clustering.Document;
import Clustering.DocumentCollection;
import TermScoring.TFIDF.TFIDFCalculator;
import TeachingDocParser.Stemmer;
import TeachingDocParser.Tokenizer;
import TeachingDocParser.WikiParser;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * Created by mhjang on 2/16/14.
 * Written by 9:13 pm
 */
public class QueryExpander {
    DocumentCollection dc;
    public QueryExpander(DocumentCollection dc) {
        this.dc = dc;
    }

    /**
     *
     * @param topicDocumentMap: a set of documents whose words are initial topic words
     * @param resourceDir: the directory that contains Wikipedia files
     *                   I manually downloaded a few matching Wikipedia articles.
     * Written 2/2/14 8:21pm
     */
    public HashMap<String, Document> expandTopicQueries(HashMap<String, Document> topicDocumentMap, String resourceDir) {
        File directory = new File(resourceDir);
        File[] listOfFiles = directory.listFiles();
        WikiParser wikiparser = new WikiParser();
        Tokenizer tokenizer = new Tokenizer();
        Stemmer stemmer = new Stemmer();
        for(File file: listOfFiles) {
            String parsedText = wikiparser.parse(file.getPath());
            parsedText = stemmer.stemString(parsedText);
            //   System.out.println("parsed " + file.getName());
            // For simplicity, I saved all wikipedi articles with the same topic labels
            // It ends with ".txt". I just need a name of the file, which is also the topic label
            String topicName = file.getName().substring(0, file.getName().length()-5);

            Document wikiDoc = tokenizer.tokenize(file.getName(), parsedText, false, Tokenizer.TRIGRAM);
            String matchingTopic = getMatchingTopicLabel(topicName.toLowerCase(), topicDocumentMap);
            Document topicDoc = topicDocumentMap.get(matchingTopic);
            //     System.out.println("Topic Name: " + topicName);
            topicDoc.mergeDocument(wikiDoc);
            topicDocumentMap.put(matchingTopic, topicDoc);
        }
        return topicDocumentMap;
    }


    /**
     * Added 2/17/2014 2:35 AM, at very remorseful night
     * expand the topic queries with terms that had appeared at least once in the first top K words in any of those document from the collection
     * Note that we only generate UNIGRAMS here from WikiDocs because the first-K-word list was generated for the unigrams. (at least for now!)
     * @param topicDocumentMap
     * @param resourceDir
     * @return
     */
    public HashMap<String, Document> expandTopicQueriesWithFirstKTerms(HashMap<String, Document> topicDocumentMap, String resourceDir, HashSet<String> firstKTerms) {
        File directory = new File(resourceDir);
        File[] listOfFiles = directory.listFiles();
        WikiParser wikiparser = new WikiParser();
        Tokenizer tokenizer = new Tokenizer();
        Stemmer stemmer = new Stemmer();
        int numOfExpandedTerms = 0;
        for(File file: listOfFiles) {
            String parsedText = wikiparser.parse(file.getPath());
            parsedText = stemmer.stemString(parsedText);
            String topicName = file.getName().substring(0, file.getName().length()-5);
            Document wikiDoc = tokenizer.tokenize(file.getName(), parsedText, false, Tokenizer.TRIGRAM);
            LinkedList<String> wikiUnigrams = new LinkedList<String>(wikiDoc.getUnigrams());
            int wikiWordDrop = 0;
            for(String term : wikiUnigrams) {
                if(!firstKTerms.contains(term))
                    wikiDoc.removeTerm(Document.UNIGRAM, term);
            }

            LinkedList<String> wikiBigrams = new LinkedList<String>(wikiDoc.getBigrams());
            for(String term : wikiBigrams) {
                if(!firstKTerms.contains(term))
                    wikiDoc.removeTerm(Document.BIGRAM, term);
            }

            LinkedList<String> wikiTrigrams = new LinkedList<String>(wikiDoc.getTrigrams());
            for(String term : wikiTrigrams) {
                if(!firstKTerms.contains(term))
                    wikiDoc.removeTerm(Document.TRIGRAM, term);
            }

            wikiDoc.printTerms();
            numOfExpandedTerms += wikiDoc.getAllGrams().size();

            String matchingTopic = getMatchingTopicLabel(topicName.toLowerCase(), topicDocumentMap);
            Document topicDoc = topicDocumentMap.get(matchingTopic);
            //     System.out.println("Topic Name: " + topicName);
            if(wikiDoc == null) {
                System.out.println("why is this null");
            }

            if(topicDoc == null) {
                System.out.println("why is this null");
            }
            topicDoc.mergeDocument(wikiDoc);
            TFIDFCalculator.calculateTFIDFGivenCollection(topicDoc, dc, TFIDFCalculator.LOGTFIDF);
            topicDocumentMap.put(matchingTopic, topicDoc);
        }
        System.out.println("# of average expanded terms: " + (double)(numOfExpandedTerms)/(double)(listOfFiles.length));
        return topicDocumentMap;


    }




    /**
     * expand the topic queries with frequent terms whose occurence in the collection is bigger than a given threshold
     * @param topicDocumentMap
     * @param resourceDir
     * @return
     */
    public HashMap<String, Document> expandTopicQueriesWithFrequentTerms(HashMap<String, Document> topicDocumentMap, String resourceDir, HashMap<String, Integer> termOccrurenceDic, int threshold) {
        File directory = new File(resourceDir);
        File[] listOfFiles = directory.listFiles();
        WikiParser wikiparser = new WikiParser();
        Tokenizer tokenizer = new Tokenizer();
        Stemmer stemmer = new Stemmer();
        int numOfExpandedTerms = 0;
        for(File file: listOfFiles) {
            System.out.println(file.getPath());
            String parsedText = wikiparser.parse(file.getPath());
            parsedText = stemmer.stemString(parsedText);
            String topicName = file.getName().substring(0, file.getName().length()-5);
            Document wikiDoc = tokenizer.tokenize(file.getName(), parsedText, false, Tokenizer.UNIGRAM);
            /****
             * filtering out the infrequent terms
             *
             * I could've modified the tokenizer in a way that throws away the terms that are not in the freequent term pool,
             * but I just thought it would be better if "Tokenizer" does tokenizing, and this sort of post-processing for query expansion
             * is done in the "Query Expander" for the sake of component modularization. (2/16/2014)
             *
             * Made this method static in Document class because this has to be used in Clustering class for documents to be clustered.
             */
            Document.removeInfrequentTerms(wikiDoc, Document.UNIGRAM, termOccrurenceDic, threshold);
       //     Document.removeInfrequentTerms(wikiDoc, Document.BIGRAM, termOccrurenceDic, threshold);
        //    Document.removeInfrequentTerms(wikiDoc, Document.TRIGRAM, termOccrurenceDic, threshold);

            numOfExpandedTerms += wikiDoc.getAllGrams().size();

            String matchingTopic = getMatchingTopicLabel(topicName.toLowerCase(), topicDocumentMap);
            Document topicDoc = topicDocumentMap.get(matchingTopic);
            //     System.out.println("Topic Name: " + topicName);
            topicDoc.mergeDocument(wikiDoc);
            topicDocumentMap.put(matchingTopic, topicDoc);
        }
        System.out.println("# of average expanded terms: " + (double)(numOfExpandedTerms)/(double)(listOfFiles.length));
        return topicDocumentMap;


    }


    public HashMap<String, Document> getWikiDocuments(String resourceDir) {
        File directory = new File(resourceDir);
        File[] listOfFiles = directory.listFiles();
        WikiParser wikiparser = new WikiParser();
        Tokenizer tokenizer = new Tokenizer();
        Stemmer stemmer = new Stemmer();
        HashMap<String, Document> wikiDocMap = new HashMap<String, Document>();
        int numOfExpandedTerms = 0;
        for(File file: listOfFiles) {
            String parsedText = wikiparser.parse(file.getPath());
            parsedText = stemmer.stemString(parsedText);
            String topicName = file.getName().substring(0, file.getName().length()-5);
            Document wikiDoc = tokenizer.tokenize(file.getName(), parsedText, false, Tokenizer.BIGRAM);
            wikiDocMap.put(topicName, wikiDoc);
        }
        return wikiDocMap;
    }

    /**
     * The name of the Wikipedia article is like "analysis of algorithm" whereas the topic key is "analysis of algorithm, and bubble sort..."
     * So instead of retrieving the matching document by hashing, we have to see if the key "contains" the given query
     * The usage of this method is very limited; it works under the assumption that the wikipedia article's filename is always the part of the topic key string.
     * I hate generating such ad-hoc methods like this, but at this point making it work is more important.
     *
     * Written in 2/2/14 8:55 pm
     * @param articleName
     * @param topicDocumentMap
     * @return
     */
    private String getMatchingTopicLabel(String articleName, HashMap<String, Document> topicDocumentMap) {
        for(String label : topicDocumentMap.keySet()) {
            if(label.contains(articleName))
                return label;
        }
        return null;
    }
}



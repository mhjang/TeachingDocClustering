package TeachingDocParser;

import Classify.noisegenerator.TableGenerator;
import Clustering.Document;
import db.DBConnector;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.LinkedHashSet;

/**
 * Created by mhjang on 2/2/14.
 * Written at 8:25 pm
 */
public class Tokenizer {
    public static int UNIGRAM = 0;
    public static int BIGRAM = 1;
    public static int TRIGRAM = 2;
    boolean useGoogleTrigram = false;
    DBConnector db;

    TableGenerator tableGenerator;
    public Tokenizer() {

    }

    /**
     * set the table generator from reading annotation data
     */
    public void setTableGenerator(TableGenerator tg) {
        this.tableGenerator = tg;
    }

    /**
     * takes String and convert it to a document with unigram/bigram/trigrams and term frequency information
     * @param line
     * @param ngram
     * @return
     */
    public Document tokenize(String docName, String line, boolean wikiFiltering, int ngram) {
        // splitting line by line s.t. we can apply language modeling to the line unit
        String[] lines = line.split("\\n");
        StopWordRemover stopRemover = new StopWordRemover();
        LinkedList<ArrayList<String>> corpusSplitByLine = new LinkedList<ArrayList<String>>();
        ArrayList<String> wordlist = new ArrayList<String>();
        for(int i=0; i<lines.length; i++) {
            String l = lines[i];
            String[] rawwords = l.split("[^a-zA-Z0-9]+");
        // removing stopwords
            String[] words = stopRemover.removeStopWords(rawwords);
            ArrayList<String> wordsInLine = new ArrayList<String>(Arrays.asList(words));
            if(wordsInLine.size()>0) {
                corpusSplitByLine.add(wordsInLine);
                  wordlist.addAll(wordsInLine);
            }
        }

        LinkedList<String> unigrams = null;
        LinkedList<String> bigrams = null;
        LinkedList<String> trigrams = null;

        if(wordlist.size() >= Tokenizer.TRIGRAM) {
            if(ngram == Tokenizer.UNIGRAM) {
                unigrams = generateUnigrams(wordlist);
                if(wikiFiltering) unigrams = filterWikiAnchor(unigrams);
            }
            else if(ngram == Tokenizer.BIGRAM) {
                unigrams = generateUnigrams(wordlist);
                bigrams = generateBigrams(wordlist);
                if(wikiFiltering) {
                    unigrams = filterWikiAnchor(unigrams);
                    bigrams = filterWikiAnchor(bigrams);
                }

            }
            else {
                unigrams = generateUnigrams(wordlist);
                bigrams = generateBigrams(wordlist);
                trigrams = generateTrigrams(wordlist);
                if(wikiFiltering) {
                    unigrams = filterWikiAnchor(unigrams);
                    bigrams = filterWikiAnchor(bigrams);
                    trigrams = filterWikiAnchor(trigrams);
                }
                if(trigrams.size() == 0)
                    System.out.println(docName);

            }
        }

        Document doc = new Document(docName, unigrams, bigrams, trigrams, corpusSplitByLine);
        LinkedList<String> wordPool = doc.getAllGrams();
        HashMap<String, Integer> docTFMap = new HashMap<String, Integer>();
        for(String word : wordPool) {
            if(!docTFMap.containsKey(word))
                docTFMap.put(word, 1);
            else
                docTFMap.put(word,  (docTFMap.get(word) + 1));
        }
        doc.setTermFrequency(docTFMap);

        // currently terms are duplicated in the list of unigram, bigram, trigrams
        // because I had to save the duplicates to count the frequency
        // now that we've saved this information to TFMap, update the N-gram list so that it only contains unique grams
        unigrams = removeDuplicates(unigrams);
        bigrams = removeDuplicates(bigrams);
        trigrams = removeDuplicates(trigrams);

        doc.setUnigrams(unigrams);
        doc.setBigrams(bigrams);
        doc.setTrigrams(trigrams);


        return doc;
     }



    /**
     * Cases like (71. 5) (83.3) ==> tokenize 71.5, 83.3.
     * We can't use Tokenizer("()") because there can be a space inside the parenthesis
     * @param s
     */
    public static ArrayList<String> parenthesisTokenizer(String s) {
        char[] array = s.toCharArray();
        ArrayList<String> tokens = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while(i < array.length) {
            if (array[i] == '(') {
                boolean parenthesisClosed = false;
                i++;
                while (!parenthesisClosed && i<array.length) {
                    if (array[i] == ')') {
                        parenthesisClosed = true;
                        tokens.add(sb.toString());
                        sb.delete(0, sb.length());
                    } else {
                        sb.append(array[i]);
                    }
                    i++;
                }
            } else {
                boolean tokenEnd = false;
                while (!tokenEnd && i<array.length) {
                    // if this is the last character
                    if(array[i] != ' ') {
                        sb.append(array[i]);
                    }
                    if (array[i] == ' ' || i==array.length -1) {
                        tokenEnd = true;
                        if(sb.toString().length() > 0) {
                            tokens.add(sb.toString());
                            sb.delete(0, sb.length());
                        }
                    }
                    i++;
                }
            }
        }
        return tokens;
    }


    public HashMap<String, Document> tokenize(String dir, boolean wikiFiltering, int nGramType) {
        HashMap<String, String> documentTextMap = readFiles(dir);
        HashMap<String, Document> documentSet = new HashMap<String, Document>();
        for(String docName : documentTextMap.keySet()) {
            Document d = tokenize(docName, documentTextMap.get(docName), wikiFiltering, nGramType);
            documentSet.put(docName, d);
        }
        return documentSet;
    }

    public HashMap<String, Document> tokenizeWithNoise(String dir, boolean wikiFiltering, int nGramType, double ratio) {
        HashMap<String, String> documentTextMap = readFilesInjectingNoise(dir, ratio);
        HashMap<String, Document> documentSet = new HashMap<String, Document>();
        for(String docName : documentTextMap.keySet()) {
            Document d = tokenize(docName, documentTextMap.get(docName), wikiFiltering, nGramType);
            documentSet.put(docName, d);
        }
        return documentSet;
    }

    /**
     * reads files in the given directory and return a parsed text per each document that's ready to be tokenized
     * @param dir
     * @return
     */
    private HashMap<String, String> readFiles(String dir) {
        HashMap<String, String> documentTextMap = new HashMap<String, String>();
        Stemmer stemmer = new  Stemmer();
        File folder = new File(dir);
        for (final File fileEntry : folder.listFiles()) {
            try {
                if(fileEntry.getName().contains(".ds_store")) continue;
                if(fileEntry.isDirectory()) continue;
                BufferedReader br = new BufferedReader(new FileReader(fileEntry));
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();
                int charsCount = 0;
                int wordsCount = 0;
                while (line != null) {
                    sb.append(line);
                    sb.append('\n');
                    line = br.readLine();
                }
                String text = sb.toString();

                documentTextMap.put(fileEntry.getName().toLowerCase(), stemmer.stemString(text));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        return documentTextMap;
    }

    /**
     * reads files in the given directory and return a parsed text per each document that's ready to be tokenized
     * @param dir
     * @return
     */
    private HashMap<String, String> readFilesInjectingNoise(String dir, double ratio) {
        HashMap<String, String> documentTextMap = new HashMap<String, String>();
        Stemmer stemmer = new  Stemmer();
        File folder = new File(dir);
        int insertedTable = 0;

        for (final File fileEntry : folder.listFiles()) {
            try {
                if(fileEntry.getName().toLowerCase().contains(".ds_store")) continue;
                if(fileEntry.isDirectory()) continue;
                BufferedReader br = new BufferedReader(new FileReader(fileEntry));
                BufferedWriter bw = new BufferedWriter(new FileWriter(dir + "/noise30/"+fileEntry.getName()));

                StringBuilder sb = new StringBuilder();
                String line = br.readLine();
                int charsCount = 0;
                int wordsCount = 0;
                int fileLine = 0;
                while (line != null) {
                    sb.append(line);
                    sb.append('\n');
                    line = br.readLine();
                    fileLine++;
               }
                int newNoiseSize = (int) (fileLine * ratio);
                int newNoiseCurrentSize = 0;
                while(newNoiseCurrentSize <= newNoiseSize) {
                    LinkedList<String> newlines = tableGenerator.generateTable();

                    //   LinkedList<String> newlines = tableGenerator.generateRandomTable();
                    for(String noiseLine : newlines) {
                        sb.append(noiseLine + "\n");
                    }
                    insertedTable++;
                    newNoiseCurrentSize += newlines.size();
                }


                String text = sb.toString();
                bw.write(text);
                bw.close();

                documentTextMap.put(fileEntry.getName().toLowerCase(), stemmer.stemString(text));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("Average # of inserted tables: " + (double)insertedTable / (double)folder.listFiles().length);

        return documentTextMap;
    }

    /**
     * generate unigrams (actually just converts it to LinkedList since tokens are already unigrams)
     * @param wordlist
     * @return
     */
    private LinkedList<String> generateUnigrams(ArrayList<String> wordlist) {
        return new LinkedList<String>(wordlist);

    }

    /**
     * generate bigrams
     * @param wordlist
     * @return
     */
    private LinkedList<String> generateBigrams(ArrayList<String> wordlist) {
        //    uncomment if you want to generate *UPTO* bigrams in addition to unigrams (this line includes unigrams)
        //     LinkedList<String> wordPool = new LinkedList<String>(wordlist);
        LinkedList<String> termList = new LinkedList<String>();

        if(wordlist.size() < 2) {
            System.out.println("There is only one word in the list");
            return termList;
        }

        String bigram = wordlist.get(0) + " " + wordlist.get(1);
        termList.add(bigram);
        int length = wordlist.size();
        for(int i=2; i<length; i++) {
            bigram = wordlist.get(i-1) + " " + wordlist.get(i);
            termList.add(bigram);
        }

        return termList;
    }

    private LinkedList<String> removeDuplicates(LinkedList<String> termsWithDuplicates) {
        if(termsWithDuplicates == null) return null;
        LinkedHashSet<String> uniqueTerms = new LinkedHashSet<String>();
        LinkedList<String> uniqueTermList = new LinkedList<String>();
        for(String term : termsWithDuplicates) {
            uniqueTerms.add(term);
        }
        uniqueTermList.addAll(uniqueTerms);
        return uniqueTermList;
    }


    /**
     * generate trigrams
     * @param wordlist
     * @return
     */
    private LinkedList<String> generateTrigrams(ArrayList<String> wordlist) {
        //    uncomment star-marked (*) lines if you want to generate *UPTO* trigrams in addition to unigrams (this line includes unigrams)
        //*     LinkedList<String> wordPool = new LinkedList<String>(wordlist);
        LinkedList<String> termList = new LinkedList<String>();

        if(wordlist.size() < 3) {
            System.out.println("There is less than 3 words in the list; can't build trigrams");
            return termList;
        }
        String bigram = wordlist.get(0) + " " + wordlist.get(1);
        //*    wordPool.add(bigram);
        int count = 0;
        String prevBigram, trigram;
        for(int i=2; i<wordlist.size(); i++) {
            prevBigram = bigram;
            bigram = wordlist.get(i-1) + " " + wordlist.get(i);
            trigram = prevBigram + " " + wordlist.get(i);
            termList.add(trigram);
        }
            //*     wordPool.add(bigram);
        return termList;
    }

    /**
     * @param wordlist
     * @return
     */
    private LinkedList<String> filterWikiAnchor(LinkedList<String> wordlist) {
        if(db == null)
            db = new DBConnector("wikipedia");
        LinkedList<String> filteredList = new LinkedList<String>();
        for(String word: wordlist) {
            ResultSet rs = db.getQueryResult("SELECT * from stemmedtitles where title ='"+word+"'");
            try {
                if(rs.next()) {
                    filteredList.add(word);
              //      System.out.println(word);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    //    System.out.println(wordlist.size() - filteredList.size() + " words are dropped with WikiAnchorFiltering.");
        return filteredList;
    }

    /**
     * Added at 2/17/2014 3:05 am.
     * This method was written because this has to be used from Query Expander
     * @param unigram
     * @param ngramType
     * @return
     */
    public LinkedList<String> generateNramsWrapper(LinkedList<String> unigram, int ngramType) {
        ArrayList<String> unigramList = new ArrayList<String>(unigram);
        if(ngramType == Tokenizer.BIGRAM)
            return generateBigrams(unigramList);
        else if(ngramType == Tokenizer.TRIGRAM)
            return generateTrigrams(unigramList);
        else return null;
    }
}

package Classify.Baseline;

import TeachingDocParser.Stemmer;
import TeachingDocParser.StopWordRemover;
import TeachingDocParser.Tokenizer;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by mhjang on 10/9/15.
 */
public class LanguageModel {
    Multiset<String> wordCount;
    StopWordRemover stopWordRemover;
    Stemmer stemmer;
    int denominator = 0;
    public static double lambda = 0.1;
    public static LanguageModel collectionModel;
    public LanguageModel() {
        wordCount = HashMultiset.create();
        stopWordRemover = new StopWordRemover();
        stemmer = new Stemmer();
    }

    public void addCorpus(String line) {

        String[] rawwords = line.split("[^a-zA-Z0-9]+");
        // removing stopwords
        String[] words = stopWordRemover.removeStopWords(rawwords);

        for(String w : words) {
            String stemmedString = stemmer.stemString(w);
            wordCount.add(stemmedString);
            denominator++;
        }
    }


    public double getProbability(String word) {
        return LanguageModel.lambda * ((double)wordCount.count(word) / (double)denominator)
                 + (1 - LanguageModel.lambda) * ((double)collectionModel.wordCount.count(word) / (double)(collectionModel.denominator));

    }
}

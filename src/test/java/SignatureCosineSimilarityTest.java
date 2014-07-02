import Clustering.Document;
import Clustering.DocumentCollection;
import Similarity.CosineSimilarity;
import TeachingDocParser.Tokenizer;
import TermScoring.TFIDF.TFIDFCalculator;
import junit.framework.TestCase;

import java.io.IOException;
import java.util.LinkedList;

/**
 * Created by mhjang on 7/1/14.
 */
public class SignatureCosineSimilarityTest extends TestCase {
    public static void main(String[] args) throws IOException {
        TFIDFCalculator tfidf = new TFIDFCalculator(true);
        tfidf.calulateTFIDF(TFIDFCalculator.LOGTFIDF, "/Users/mhjang/Documents/workspace/TeachingDocClustering/src/main/resources/testcollection/", Tokenizer.UNIGRAM, false);
        DocumentCollection dc = tfidf.getDocumentCollection();
        LinkedList<String> signatures = dc.constructSignatureVector(2);
        /***
         * signature features
         */
        for(String s : signatures) {
            System.out.println(s);
        }

        Document d1 = dc.getDocument("testdoc1");
        Document d2 = dc.getDocument("testdoc2");

        System.out.println(CosineSimilarity.TFIDFCosineSimilarityForSignature(d1, d2, signatures));


    }
}

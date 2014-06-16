package TermScoring;

import Clustering.DocumentCollection;
import TermScoring.TFIDF.TFIDFCalculator;
import TeachingDocParser.Tokenizer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

/**
 * Created by mhjang on 4/11/14.
 */
public class ConferenceAnalysis {
    public static void main(String[] args) throws IOException {
        PrintStream console = System.out;
        File file = new File("termranking_bigramsonly_ratio.txt");
        FileOutputStream fos = new FileOutputStream(file);
        PrintStream ps = new PrintStream(fos);
        System.setOut(ps);
        TFIDFCalculator ICTIRCal = new TFIDFCalculator(false);
        System.out.println("ICTIR Term Ranking");

        ICTIRCal.calulateTFIDF(TFIDFCalculator.LOGTFIDF,"/Users/mhjang/Downloads/fwdictirvssigir/raw/ICTIR", Tokenizer.BIGRAM, false);
        DocumentCollection ictirDoc = ICTIRCal.getDocumentCollection();

        HashMap<String, Integer> termCountInICTIR = ictirDoc.getglobalTermCountMap();
        List<Map.Entry<String, Integer>> list =
                new LinkedList<Map.Entry<String, Integer>>( termCountInICTIR.entrySet() );
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });


        for(Map.Entry<String, Integer> entry : list.subList(0,80)) {
            System.out.println(entry.getKey() + ":" + entry.getValue());
        }

        HashSet<String> terms = new HashSet<String>();
        // adding all terms from ICTIR
        terms.addAll(termCountInICTIR.keySet());

        System.out.println("SIGIR Term Ranking");

        TFIDFCalculator SIGIRCal = new TFIDFCalculator(false);
        SIGIRCal.calulateTFIDF(TFIDFCalculator.LOGTFIDF,"/Users/mhjang/Downloads/fwdictirvssigir/raw/SIGIR", Tokenizer.BIGRAM, false);
        DocumentCollection sigirDoc = SIGIRCal.getDocumentCollection();

        HashMap<String, Integer> termCountInSIGIR = sigirDoc.getglobalTermCountMap();
        List<Map.Entry<String, Integer>> list2 =
                new LinkedList<Map.Entry<String, Integer>>( termCountInSIGIR.entrySet() );
        Collections.sort(list2, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });
        // adding all terms from SIGIR
        terms.addAll(termCountInSIGIR.keySet());

   //     System.out.println("count of analysis (SIGIR) " + termCountInSIGIR.get("analysis"));
   //     System.out.println("count of mathematical (SIGIR) " + termCountInSIGIR.get("mathematical"));


             for(Map.Entry<String, Integer> entry : list2.subList(0, 80)) {
            System.out.println(entry.getKey() + ":" + entry.getValue());
        }

        HashMap<String, Double> diffMap = new HashMap<String, Double>();
        HashMap<String, Double> diffMap2 = new HashMap<String, Double>();

        double ratio1 = 0, ratio2 = 0;

        /**
         * The collection size
         */
        double ictirSize = 0.0;
        for(String key: termCountInICTIR.keySet()) {
            ictirSize += termCountInICTIR.get(key);
        }

        double sigirSize = 0.0;
        for(String key: termCountInSIGIR.keySet()) {
            sigirSize += termCountInSIGIR.get(key);
        }

        System.out.println("ICTIR size=" + ictirSize);
        System.out.println("SIGIR size=" + sigirSize);

        for(String term : terms) {
            if(termCountInICTIR.containsKey(term))
                ratio1 = (double)termCountInICTIR.get(term) / ictirSize;
            else {
                ratio1 = 0.0;
                termCountInICTIR.put(term, 0);
            }
            if(termCountInSIGIR.containsKey(term))
                ratio2 = (double)termCountInSIGIR.get(term) / sigirSize;
            else {
                ratio2 = 0.0;
                termCountInSIGIR.put(term, 0);
            }
            if(ratio1 > 0.0) diffMap.put(term, (ratio1 - ratio2));
            if(ratio2 > 0.0) diffMap2.put(term, (ratio2 - ratio1));
        }

        List<Map.Entry<String, Double>> list3 =
                new LinkedList<Map.Entry<String, Double>>( diffMap.entrySet() );
        Collections.sort(list3, new Comparator<Map.Entry<String, Double>>() {
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });



        System.out.println("Difference Ranking (ICTIR Term - SigIR Term count)");
         for(Map.Entry<String, Double> entry : list3.subList(0, 80)) {
            System.out.println(entry.getKey() + ":" + entry.getValue() * 10000);
         }


        List<Map.Entry<String, Double>> list4 =
                new LinkedList<Map.Entry<String, Double>>( diffMap2.entrySet() );
        Collections.sort(list4, new Comparator<Map.Entry<String, Double>>() {
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });

        System.out.println("Difference Ranking (SIGIR Term - SigIR Term count)");
        for(Map.Entry<String, Double> entry : list4.subList(0, 80)) {
            System.out.println(entry.getKey() + ":" + entry.getValue() * 10000);
        }

    }
}

package Classify;

import simple.io.myungha.DirectoryReader;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by mhjang on 7/14/14.
 */
public class FiveFoldDataSplit {
    /**
     *
     * @param dir: directory that contains all annotation files
     * @param isBasedOnFile true: divide by FILES, false: divide by LINES
     */

    public static Map.Entry<LinkedList<LinkedList<String>>, LinkedList<LinkedList<String>>> getFiveFoldSet(String dir, boolean isBasedOnFile) {
        if(isBasedOnFile) {
                DirectoryReader dr = new DirectoryReader(dir);
                ArrayList<String> filenames = dr.getFileNameList();
                int size = filenames.size();
                int unit = size / 5;
                // partitioning
                ArrayList<ArrayList<String>> partitions = new ArrayList<ArrayList<String>>();
                for(int i=1; i<=5; i++) {
                    if(i<5)
                        partitions.add(new ArrayList<String>(filenames.subList((i-1)*unit, i*unit)));
                    else
                        partitions.add(new ArrayList<String>(filenames.subList((i-1)*unit, filenames.size()-1)));
                }
                // assembling folds
                LinkedList<LinkedList<String>> trainingSet = new LinkedList<LinkedList<String>>();
                LinkedList<LinkedList<String>> testSet = new LinkedList<LinkedList<String>>();
                LinkedList<String> training = new LinkedList<String>();
                LinkedList<String> test = new LinkedList<String>();

                for(int i = 0; i<5; i++) {
                    for(int j=0; j<5; j++) {
                        if(j != i)
                            training.addAll(partitions.get(i));
                        else
                            test.addAll(partitions.get(i));
                        trainingSet.add(training);
                        testSet.add(test);
                    }
                }
            return new AbstractMap.SimpleEntry<LinkedList<LinkedList<String>>, LinkedList<LinkedList<String>>>(trainingSet, testSet);

        }
        else {
            // token-based five fold; to be implemented

        }
        return null;
    }
}

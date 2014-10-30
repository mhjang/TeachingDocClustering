package Classify.noisegenerator;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import java.util.LinkedList;
import java.util.Random;

/**
 * Created by mhjang on 10/20/14.
 */
public class TableGenerator {
    LinkedList<LinkedList<String>> tables;
 //   Multiset<String> vocabulary = new HashMultiset;
    static Random random = new Random();

    public static void main(String[] args) {
    //    TableGenerator.generateRandomTable(10);
    }
    public TableGenerator() {
        tables = new LinkedList<LinkedList<String>>();
    }

    /**
     * 2014/10/20 differentiate only the first and rest lines

     */
    public void addTable(LinkedList<String> aTable) {
        tables.add(aTable);
    }

    public int getSize() {
        return tables.size();
    }
    public LinkedList<String> generateTable() {
        int n = tables.size();
        int rand = random.nextInt(n);
        LinkedList<String> t =  tables.get(rand);
    /*    for(String line : t) {
            System.out.println(line);
        }
        */
        return t;
    }


    private String generateRandomString() {
        StringBuilder randomString = new StringBuilder();
        int length = random.nextInt(6) + 1;
        for (int i = 0; i < length; i++) {
            int j = 97 + random.nextInt(122 - 97);
            char c = (char) j;
            randomString.append(c);
        }
        return randomString.toString();
    }
    public LinkedList<String> generateRandomTable() {
        // first line

        int n = random.nextInt(7) + 4;
        int column = random.nextInt(15) + 3;
        LinkedList<String> t = new LinkedList<String>();
        StringBuilder lineBuilder = new StringBuilder();
        for(int k=0; k<column; k++) {
            lineBuilder.append(generateRandomString() + " ");
   //         System.out.print(randomString.toString() + " ");
        }
        t.add(lineBuilder.toString());

        //    System.out.println();
        for(int i=0; i<n; i++) {
            lineBuilder = new StringBuilder();
            lineBuilder.append(generateRandomString() + " ");
            for(int j=0; j<column-1; j++) {
                lineBuilder.append(random.nextInt(10000) + " ");
            }
            t.add(lineBuilder.toString());
//            System.out.println();
        }

        return t;
    }



}

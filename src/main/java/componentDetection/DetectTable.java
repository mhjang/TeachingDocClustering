package componentDetection;

import Classify.liblinear.datastructure.DatasetDir;
import TeachingDocParser.Tokenizer;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.eclipse.jetty.util.MultiMap;

import java.io.*;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by mhjang on 6/9/14.
 *
 * Table Detection Rules
 *
 * 1. # of tokens in a table block must be the same.
 * 2. It can contain a sequence of numbers with the left-most column with some name.
 * 3. The pattern of the two consecutive lines might be the same.
 */
public class DetectTable {
    Pattern numberContainPattern = Pattern.compile("^[a-zA-Z]*([0-9]+).*");
    Pattern numberPattern = Pattern.compile("/\\d*\\.?\\d*$/\n");
    ArrayList<String> lines = new ArrayList<String>();
    Multiset<String> tablePatternUnigram;
    Multiset<String> tablePatternBigram;

    public DetectTable() {
        try {
            String baseDir = DatasetDir.CLASSIFY_combined;
            tablePatternUnigram = HashMultiset.create();
            BufferedReader br = new BufferedReader(new FileReader(new File(baseDir + "stats/table.1gram")));
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split("\\t");
                tablePatternUnigram.add(tokens[0], Integer.parseInt(tokens[1]));
            }

            tablePatternBigram = HashMultiset.create();
            br = new BufferedReader(new FileReader(new File(baseDir + "stats/table.2gram")));
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split("\\t");
                tablePatternBigram.add(tokens[0], Integer.parseInt(tokens[1]));
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // rule #3
    public  boolean containsSamePattern(String prevLine, String curLine) {
        // Do the two lines both contain the same number of number tokens?
        Matcher m1 = numberPattern.matcher(prevLine);
        int prevLineCount = 0, curLineCount = 0;
        while(m1.find())
            prevLineCount++;
        Matcher m2 = numberPattern.matcher(curLine);
        while(m2.find())
            curLineCount++;
        if(prevLineCount == curLineCount) return true;
        return false;


    }

    // rule #2
/*    public static boolean containsNumbers(String curLine) {

    }
*/
    // rule #1
    public  boolean containsSameTokenNum(String prevLine, String curLine) {
        if(prevLine.split(" ").length == curLine.split(" ").length) return true;
        else return false;
    }


    public  boolean isTable(String line) {
        line = line.replaceAll(",|;", "");

/*
       String[] tokens = line.split(" ");
        for(int i=0; i<tokens.length; i++) {
            boolean codeMatch = false;
            if(numberPattern.matcher(tokens[i]).find()) codeMatch = true;
        }
*/
        return true;
    }


    public static void main(String[] args) {
        // edit distance
     /*   String l1 = "1 2 3 4 5 6                                         Alb  Ken Eme Ber Oak Pie";
        String l2 = "1 - - - T - -                       Albany           -     T      -      T      -    -";
        String l3 = "2 T - - - - -                Kensington          T    -        -     T      -   -";
        String l4 = "3 - T - - - T                Emeryville           -   -   -   T   T   -";
        String l5 = "4 - - - - T -                  Berkeley            T   T   T   -   T   -";
        String l6 = "5 - T - - - T                   Oakland            -   -   T   T   -   T";
        String l7 = "6 - - T - - -                  Piedmont           -   -   -   -   T   -";

        System.out.println(encodeString(l1));
        System.out.println(encodeString(l2));
        System.out.println(encodeString(l3));
        System.out.println(encodeString(l4));
        System.out.println(encodeString(l5));
        System.out.println(encodeString(l6));
        System.out.println(encodeString(l7));

    */
        //restoreParenthesis("( 3 ) + ( 4 ) ");
    }


    public  double tableLMProbability(String encodedLine) {
        char[] encodedArray = encodedLine.toCharArray();
        int unigramTotal = 0, bigramTotal = 0;
        for(String c : tablePatternUnigram.elementSet()) {
            unigramTotal += tablePatternUnigram.count(c);
        }
        for(String cc : tablePatternBigram.elementSet()) {
            bigramTotal += tablePatternBigram.count(cc);
        }
        double probability = 1.0;
        for(int i=0; i<encodedArray.length; i++) {
            if (i == 0) {
                probability *= (double) tablePatternUnigram.count(Character.toString(encodedArray[i])) / (double) unigramTotal;
            } else {
                probability *= (double) tablePatternBigram.count(Character.toString(encodedArray[i - 1]) + Character.toString(encodedArray[i])) /
                        (double) tablePatternUnigram.count(Character.toString(encodedArray[i]));
            }
        }
        return probability;
    }
    // encode the line by type
    public  String encodeString(String line) {
        ArrayList<String> tokens = Tokenizer.parenthesisTokenizer(line);
        StringBuilder sb = new StringBuilder();
        for(String word: tokens) {
            word = removeSpecialCharacters(word);
            if(word.length() > 0) {
                if (NumberUtils.isNumber(word))
                    sb.append("N");
                else
                    sb.append("S");
            }
        }
        return sb.toString();

    }

    public  int getNumberTokenCount(String encodedLine) {
        int count = 0;
        for(int i=0; i<encodedLine.length(); i++) {
            if(encodedLine.charAt(i) == 'N') count++;
        }
        return count;
    }
    // collect encoded table lines
    // one time statistics building process
    public  void addAnnotationLine(String line) {
        lines.add(line);
    }

    // one time statistics building: I saved the result at /stats/table.ngram
    public  void buildStatistics() {
        Multiset<Character> tablePatternUnigram = HashMultiset.create();
        Multiset<String> tablePatternBigram = HashMultiset.create();

        for(String line: lines) {
            char[] pattern = line.toCharArray();
            for(int i=0; i<pattern.length; i++) {
                tablePatternUnigram.add(pattern[i]);
                if(i>0) {
                    tablePatternBigram.add(Character.toString(pattern[i-1]) + Character.toString(pattern[i]));
                }
            }
        }
        System.out.println("table pattern unigram");
        for(Character c : tablePatternUnigram.elementSet()) {
            System.out.println(c + ":" + tablePatternUnigram.count(c));
        }

        System.out.println("table pattern bigram");
        for(String cc : tablePatternBigram.elementSet()) {
            System.out.println(cc + ":" + tablePatternBigram.count(cc));
        }
    }

    public  double getRatioRightMatching(String line, String prevLine) {
        int n = line.length();
        int m = prevLine.length();
        int size = min(n, m);

        int i=0, count =0;
        while(i<size) {
            if(line.charAt(n-1-i) == prevLine.charAt(m-1-i)) count++;
            else break;
            i++;
        }
        double ratio = (double) count/ (double) n;
        if(Double.isNaN(ratio)) return 0.0;
        else return ratio;
    }

    public  double getRatioLeftMatching(String line, String prevLine) {
        int n = line.length();
        int m = prevLine.length();
        int size = min(n, m);

        int i=0, count =0;
        while(i<size) {
            if(line.charAt(i) == prevLine.charAt(i)) count++;
            else break;
            i++;
        }
        double ratio = (double) count/ (double) n;
        if(Double.isNaN(ratio)) return 0.0;
        else return ratio;
    }


    public  String removeSpecialCharacters(String s) {
        char[] sarray = s.toCharArray();
        char[] specialCharacters = {'(',')','!','@','#','$','%','^','&','*','{','}','\'',':','\"','+','-', '.', ','};
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i< sarray.length; i++) {
            boolean flag = false;
            for(int j=0; j<specialCharacters.length; j++) {
                if (sarray[i] == specialCharacters[j]) {
                    flag = true;
                    break;
                }
            }
            if(!flag) sb.append(sarray[i]);
        }
        //     System.out.println(sb.toString());
        return sb.toString();
    }
    public  int getEditDistance(String s1, String s2)
    {

        int m = s1.length();
        int n = s2.length();
        int[][] v = new int[m+1][n+1];
        for (int i = 0; i <= m; i++) {
            v[i][0] = i;
        }
        for (int j = 0; j <= n; j++) {
            v[0][j] = j;
        }

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i-1) == s2.charAt(j-1)) v[i][j] = v[i-1][j-1];
                    //         else v[i][j] = 5 + min(min(v[i][j-1],v[i-1][j]),v[i-1][j-1]);
                else {
                    v[i][j] = min(min(v[i-1][j]+1, v[i][j-1]+1), v[i-1][j-1] + 1);
                }
            }
        }

        return v[m][n];
    }

    static int min(int a, int b) {
        if(a< b) return a;
        else return b;
    }

    // 2014/10/5
    // Because clearNLP's tokenizer butchered up all parentehsis, I had this method to put them back.
    // Later I decided to use original text files.
    public  String restoreParenthesis(String line) {
        line = line.replaceAll("\\(\\s", "(");
        line = line.replaceAll("\\s\\)", ")");

        line = line.replaceAll("\\{\\s", "{");
        line = line.replaceAll("\\s\\}", "}");

        line = line.replaceAll("\\[\\s", "[");
        line = line.replaceAll("\\s\\]", "]");

        return line;

    }


}

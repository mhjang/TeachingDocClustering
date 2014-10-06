package Classify.liblinear;

import componentDetection.DetectTable;
import evaluation.ClusteringFMeasure;

import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Created by mhjang on 9/5/14.
 */
public class Test {

    public static void main(String[] args) {
  //      fm.readGoldstandardACLDataset("/Users/mhjang/Desktop/clearnlp/dataset/acl/goldstandard.txt");
    //    fm.readTopicClustersACL("/Users/mhjang/Desktop/clearnlp/dataset/acl/clustertopics.txt");
   /*     String s = "n=5 80.74 80.88 81.03 81.05 83.17";
        String s1 = "(line 5) (71. 7) (73. 4) (73.3) (74.6) a b";

        System.out.println(DetectTable.getRatioRightMatching("NNN", "SNNN"));
  //      parenthesisTokenizer(s1);
/*        StringTokenizer st = new StringTokenizer(s1, "()");
        while(st.hasMoreTokens()) {
            System.out.println(st.nextToken());
        }
        /**
        String newS1 = DetectTable.encodeString(s);
        String newS2 = DetectTable.encodeString(s1);
        System.out.println(newS1);
        System.out.println(newS2);
        System.out.println(DetectTable.getEditDistance(newS1, newS2));
        **/
    }

    public static void parenthesisTokenizer(String s) {
        char[] array = s.toCharArray();
        ArrayList<String> tokens = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while(i < array.length) {
            if (array[i] == '(') {
                boolean parenthesisClosed = false;
                i++;
                while (!parenthesisClosed) {
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
        for(String t : tokens) {
            System.out.println(t);
        }
    }

}

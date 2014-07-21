package componentDetection;

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
    static Pattern numberContainPattern = Pattern.compile("^[a-zA-Z]*([0-9]+).*");
    static Pattern numberPattern = Pattern.compile("/\\d*\\.?\\d*$/\n");



    // rule #3
    public static boolean containsSamePattern(String prevLine, String curLine) {
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
    public static boolean containsSameTokenNum(String prevLine, String curLine) {
        if(prevLine.split(" ").length == curLine.split(" ").length) return true;
        else return false;
    }


    public static boolean isTable(String line) {
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


}

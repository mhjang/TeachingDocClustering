package componentDetection;

import java.util.regex.Pattern;

/**
 * Created by mhjang on 6/9/14.
 */
public class DetectTable {
    static Pattern numberPattern = Pattern.compile("^[a-zA-Z]*([0-9]+).*");

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

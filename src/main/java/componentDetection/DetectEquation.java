package componentDetection;

import java.util.regex.Pattern;

/**
 * Created by mhjang on 6/8/14.
 */
public class DetectEquation {
    static Pattern numberPattern = Pattern.compile("^[a-zA-Z]*([0-9]+).*");
    static Character.UnicodeBlock[] codeset = {Character.UnicodeBlock.MATHEMATICAL_ALPHANUMERIC_SYMBOLS,
    Character.UnicodeBlock.MATHEMATICAL_OPERATORS, Character.UnicodeBlock.BASIC_LATIN};
    public static boolean isEquation(String line) {
        String[] tokens = line.split(" ");
        for(int i=0; i<tokens.length; i++) {
            boolean codeMatch = false;
            if(numberPattern.matcher(tokens[i]).find()) codeMatch = true;
            else if(tokens[i].length()==1) {
                Character.UnicodeBlock code = Character.UnicodeBlock.of(tokens[i].charAt(0));
                for(int k=0; k<codeset.length; k++) {
                    if(code == codeset[k]) {
                        codeMatch = true;
                        break;
                    }
                }
            }
            if(codeMatch == false) return false;
        }
        return true;
    }

}

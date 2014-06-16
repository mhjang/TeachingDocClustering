package Classify;

/**
 * Created by mhjang on 6/7/14.
 */
public class StringTokenizerIndx {

    public static void main(String[] args) {
        /**
         * TEST
         */
        String sentence = "It is a beautiful night we are looking for something dumb to do";
        String token = "night";
        int tokenIdx = sentence.indexOf(token);
        String[] tokens = sentence.split(" ");
        System.out.println(token + "(" +findTokenNthfromIndex(sentence, tokenIdx)+") --> " + tokens[findTokenNthfromIndex(sentence, tokenIdx)]);

    }

    /**
     * given the Nth token, return the character location in the line
     * @param line
     * @param tokenIdx
     * @return
     */
    public static int findIndexOfNthToken(String line, int tokenIdx) {
        String[] tokens = line.split(" ");
        int curIdx = 0;
        for(int i=0; i<tokenIdx; i++) {
            curIdx += tokens[i].length();
            curIdx++; // for a blank space
        }
        return curIdx;
    }
    /**
     * given the index, return the N that the token is located at
     * @return N
     */
    public static int findTokenNthfromIndex(String line, int index) {
        String[] tokens = line.split(" ");
        if(index == line.length()) return tokens.length -1;
        int curIdx = 0;
        for(int i=0; i<tokens.length; i++) {
            if(curIdx == index) return i;
            curIdx += tokens[i].length() + 1;
        }
        return -1;
    }


}

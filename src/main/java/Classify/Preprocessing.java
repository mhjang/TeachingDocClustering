package Classify;

import com.drew.metadata.Directory;
import simple.io.myungha.DirectoryReader;
import simple.io.myungha.SimpleFileReader;
import simple.io.myungha.SimpleFileWriter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by mhjang on 8/26/14.
 */
public class Preprocessing {
    static Pattern numberPattern = Pattern.compile("^[a-zA-Z]*([0-9]+).*");

    public static void main(String[] args) throws IOException {
        String dir = "/Users/mhjang/Desktop/clearnlp/allslides/annotation/";
        DirectoryReader dr = new DirectoryReader(dir);
        for (String filename : dr.getFileNameList()) {
            removeOneCharacterAndNumber(dir, filename);
            //      testRemoveEmptyTags();
        }

    }

    public static void removeOneCharacterAndNumber(String dir, String filename) throws IOException {
        SimpleFileReader fr = new SimpleFileReader(dir + filename);
        SimpleFileWriter fw = new SimpleFileWriter(dir + "processed/"+filename);
        int count = 0;
        while(fr.hasMoreLines()) {
            String line = fr.readLine();
            String[] tokens = line.split(" ");
            StringBuilder sb = new StringBuilder();
            for(int i=0; i<tokens.length; i++) {
                tokens[i] = tokens[i].replaceAll("\\?+", "");
                if(tokens[i].length() > 1 && !numberPattern.matcher(tokens[i]).find())
                    sb.append(tokens[i] + " ");
                else
                    count++;
            }
            String tagRemovedTags = removeEmptyTags(sb.toString());
            if(sb.toString().length() > 0)
                fw.writeLine(tagRemovedTags);
        }
        fw.close();
        fr.close();
        System.out.println(count + " characters are removed");
    }


    public static String removeEmptyTags(String line) {
        line = line.replaceAll("<MISCELLANEOUS>\\s+</MISCELLANEOUS>", "");
        line = line.replaceAll("<CODE>\\s+</CODE>", "");
        line = line.replaceAll("<TABLE>\\s+</TABLE>", "");
        line = line.replaceAll("<EQUATION>\\s+</EQUATION>","");

        return line;
    }

    /**
     * Test method for removeEmptyTags
     */
    public static void testRemoveEmptyTags() {
        String line = "blah <TABLE> </TABLE> blah";
        String line2 = "blah <TABLE>      </TABLE> blah";
        String line3 = "blah <CODE> </CODE> blah";
        String line4 = "blah <CODE>      </CODE> blah";
        String line5 = "blah <EQUATION> </EQUATION> blah";
        String line6 = "blah <EQUATION>      </EQUATION> blah";
        String line7 = "blah <MISCELLANEOUS> </MISCELLANEOUS> blah";
        String line8 = "blah <MISCELLANEOUS>      </MISCELLANEOUS> blah";

        System.out.println(removeEmptyTags(line));
        System.out.println(removeEmptyTags(line2));
        System.out.println(removeEmptyTags(line3));
        System.out.println(removeEmptyTags(line4));
        System.out.println(removeEmptyTags(line5));
        System.out.println(removeEmptyTags(line6));
        System.out.println(removeEmptyTags(line7));
        System.out.println(removeEmptyTags(line8));


    }

}

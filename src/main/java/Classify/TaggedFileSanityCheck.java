package Classify;

import simple.io.myungha.DirectoryReader;

/**
 * Created by mhjang on 6/3/14.
 * It is important to make sure that the tagged file does not include malformed tags.
 * Just check every initiated tag has a matching end tag before another tag is initiated
 */
public class TaggedFileSanityCheck {
    static String tableTag = "<TABLE>";
    static String codeTag = "<CODE>";
    static String equTag = "<EQUATION>";
    static String miscTag = "<MISCELLANEOUS>";

    static String tableCloseTag = "</TABLE>";
    static String codeCloseTag = "</CODE>";
    static String equCloseTag = "</EQUATION>";
    static String miscCloseTag = "</MISCELLANEOUS>";


    static String[] tags = {tableTag, codeTag, equTag, miscTag};
    static String[] closeTags = {tableCloseTag, codeCloseTag, equCloseTag, miscCloseTag};
    private enum Tag {
        tableTag, codeTag, equTag, miscTag
    }
    private enum EndTag {
        tableCloseTag, codeCloseTag, equCloseTag, miscCloseTag
    }

    public static void main(String[] args) {
        DirectoryReader dirReader = new DirectoryReader("./tagged/");
        for(String file: dirReader.getFileNameList()) {

        }
    }
}

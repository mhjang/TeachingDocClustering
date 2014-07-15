package Classify;

/**
 * Created by mhjang on 6/3/14.
 */
public class TagConstant {
/*
    public static int BEGINTABLE = 1;
    public static int INTTABLE = 2;
    public static int ENDTABLE = 3;

    public static int BEGINCODE = 4;
    public static int INTCODE = 5;
    public static int ENDCODE = 6;

    public static int BEGINEQU = 7;
    public static int INTEQU = 8;
    public static int ENDEQU = 9;

    public static int BEGINMISC = 10;
    public static int INTMISC = 11;
    public static int ENDMISC = 12;
    public static int TEXT = 0;
*/

    public static int BEGINTABLE = 1;
    public static int INTTABLE = 1;
    public static int ENDTABLE = 1;

    public static int BEGINCODE = 2;
    public static int INTCODE = 2;
    public static int ENDCODE = 2;

    public static int BEGINEQU = 3;
    public static int INTEQU = 3;
    public static int ENDEQU = 3;

    public static int BEGINMISC = 4;
    public static int INTMISC = 4;
    public static int ENDMISC = 4;
    public static int TEXT = 0;

    public static String tableTag = "<TABLE>";
    public static String codeTag = "<CODE>";
    public static String equTag = "<EQUATION>";
    public static String miscTag = "<MISCELLANEOUS>";

    public static String tableCloseTag = "</TABLE>";
    public static String codeCloseTag = "</CODE>";
    public static String equCloseTag = "</EQUATION>";
    public static String miscCloseTag = "</MISCELLANEOUS>";

    public static String tableIntTag = "<TABLE-I>";
    public static String codeIntTag = "<CODE-I>";
    public static String equIntTag = "<EQUATION-I>";
    public static String miscIntTag = "<MISCELLANEOUS-I>";
    public static String textTag = "<TEXT>";

    /**
     * for printing out test output
     * @param tagIdx
     * @return
     */
    static public String getTagLabel(int tagIdx) {
        if(tagIdx == BEGINTABLE) return tableTag;
        else if(tagIdx == INTTABLE) return tableIntTag;
        else if(tagIdx == ENDTABLE) return tableCloseTag;
        else if(tagIdx == BEGINCODE) return codeTag;
        else if(tagIdx == INTCODE) return codeIntTag;
        else if(tagIdx == ENDCODE) return codeCloseTag;
        else if(tagIdx == BEGINEQU) return equTag;
        else if(tagIdx == INTEQU) return equIntTag;
        else if(tagIdx == ENDEQU) return equCloseTag;
        else if(tagIdx == BEGINMISC) return miscTag;
        else if(tagIdx == INTMISC) return miscIntTag;
        else if(tagIdx == ENDMISC) return miscCloseTag;
        else return textTag;

    }

    /**
     * for printing out test output
     * @param tagIdx
     * @return
     */
    public static String getTagLabelByComponent(int tagIdx) {
        if(tagIdx == BEGINTABLE || tagIdx == INTTABLE || tagIdx == ENDTABLE) return tableTag;
        else if(tagIdx == BEGINCODE || tagIdx == INTCODE || tagIdx == ENDCODE) return codeTag;
        else if(tagIdx == BEGINEQU || tagIdx == INTEQU || tagIdx == ENDEQU ) return equTag;
        else if(tagIdx == BEGINMISC || tagIdx == INTMISC || tagIdx == ENDMISC) return miscTag;
        else return textTag;

    }

    public static String findMatchingEndTag(String beginTag) {
        if(beginTag == TagConstant.tableTag) return TagConstant.tableCloseTag;
        else if(beginTag == TagConstant.codeTag) return TagConstant.codeCloseTag;
        else if(beginTag == TagConstant.equTag) return TagConstant.equCloseTag;
        else return TagConstant.miscCloseTag;
    }
}

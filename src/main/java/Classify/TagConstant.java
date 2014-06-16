package Classify;

/**
 * Created by mhjang on 6/3/14.
 */
public class TagConstant {

    static int BEGINTABLE = 1;
    static int INTTABLE = 2;
    static int ENDTABLE = 3;

    static int BEGINCODE = 4;
    static int INTCODE = 5;
    static int ENDCODE = 6;

    static int BEGINEQU = 7;
    static int INTEQU = 8;
    static int ENDEQU = 9;

    static int BEGINMISC = 10;
    static int INTMISC = 11;
    static int ENDMISC = 12;
    static int TEXT = 0;

    static String tableTag = "<TABLE>";
    static String codeTag = "<CODE>";
    static String equTag = "<EQUATION>";
    static String miscTag = "<MISCELLANEOUS>";

    static String tableCloseTag = "</TABLE>";
    static String codeCloseTag = "</CODE>";
    static String equCloseTag = "</EQUATION>";
    static String miscCloseTag = "</MISCELLANEOUS>";

    static String tableIntTag = "<TABLE-I>";
    static String codeIntTag = "<CODE-I>";
    static String equIntTag = "<EQUATION-I>";
    static String miscIntTag = "<MISCELLANEOUS-I>";
    static String textTag = "<TEXT>";

    /**
     * for printing out test output
     * @param tagIdx
     * @return
     */
    static String getTagLabel(int tagIdx) {
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
    static String getTagLabelByComponent(int tagIdx) {
        if(tagIdx == BEGINTABLE || tagIdx == INTTABLE || tagIdx == ENDTABLE) return tableTag;
        else if(tagIdx == BEGINCODE || tagIdx == INTCODE || tagIdx == ENDCODE) return codeTag;
        else if(tagIdx == BEGINEQU || tagIdx == INTEQU || tagIdx == ENDEQU ) return equTag;
        else if(tagIdx == BEGINMISC || tagIdx == INTMISC || tagIdx == ENDMISC) return miscTag;
        else return textTag;

    }

}

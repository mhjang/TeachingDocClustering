package Classify.liblinear.datastructure;

import Classify.TagConstant;

/**
 * Created by mhjang on 10/2/14.
 */
public class NonTextualComponent {
    public int begin, intermediate, end;

    static NonTextualComponent table = new NonTextualComponent(TagConstant.BEGINTABLE, TagConstant.INTTABLE, TagConstant.ENDTABLE);
    static NonTextualComponent code = new NonTextualComponent(TagConstant.BEGINCODE, TagConstant.INTCODE, TagConstant.ENDCODE);
    static NonTextualComponent equation = new NonTextualComponent(TagConstant.BEGINEQU, TagConstant.INTEQU, TagConstant.ENDEQU);
    static NonTextualComponent misc = new NonTextualComponent(TagConstant.BEGINMISC, TagConstant.INTMISC, TagConstant.ENDMISC);

    public NonTextualComponent(int begin, int intermidiate, int end) {
        this.begin = begin;
        this.intermediate = intermidiate;
        this.end = end;
    }

    /**
     * Given the tag type extractef from the annotation, it returns NonTextualComponent object for generating features
     *
     * @param tagType
     * @return
     */
    public static NonTextualComponent getComponent(String tagType) {
        NonTextualComponent nonTextualComponent;
        if (tagType == null) {
            nonTextualComponent = null;
        } else if (tagType.equals(TagConstant.tableTag) || tagType.equals(TagConstant.tableIntTag) || tagType.equals(TagConstant.tableCloseTag)) {
            nonTextualComponent = table;
        } else if (tagType.equals(TagConstant.codeTag) || tagType.equals(TagConstant.codeIntTag) || tagType.equals(TagConstant.codeCloseTag)) {
            nonTextualComponent = code;
        } else if (tagType.equals(TagConstant.equTag) || tagType.equals(TagConstant.equIntTag) || tagType.equals(TagConstant.equCloseTag)) {
            nonTextualComponent = equation;
        } else {
            nonTextualComponent = misc;
        }
        return nonTextualComponent;
    }
}



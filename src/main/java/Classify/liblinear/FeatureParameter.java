package Classify.liblinear;

import Classify.TagConstant;
import com.clearnlp.dependency.DEPTree;

/**
 * Created by mhjang on 7/18/14.
 * Using Java Builder pattern to handle many paramters
 */
public class FeatureParameter {
    // required parameters
    private final boolean isThisLineCode;
    private final boolean isThisLineEquation;
    private final boolean isThisLineTable;
    private final boolean applyModel;
    // optional parameters
    private final int componentFragBegin;
    private final int componentFragEnd;
    private final DEPTree tree;
    private final String tagType;
    private final int beginTokenIndex;
    private final int endTokenIndex;

    public static class Builder {
        // required parameters
        private final boolean isThisLineCode;
        private final boolean isThisLineEquation;
        private final boolean isThisLineTable;
        private final boolean applyModel;
        // optional parameters
        private int componentFragBegin = 0;
        private int componentFragEnd = 0;
        private DEPTree tree;
        private String tagType = null;
        private int beginTokenIndex = 0;
        private int endTokenIndex = 0;

        public Builder(DEPTree tree_, boolean isThisLineCode_, boolean isThisLineEquation_, boolean isThisLineTable_, boolean applyModel_) {
            this.tree = tree_;
            this.isThisLineCode = isThisLineCode_;
            this.isThisLineEquation = isThisLineEquation_;
            this.isThisLineTable = isThisLineTable_;
            this.applyModel = applyModel_;
        }

        public Builder componentFrag(int beginLoc, int endLoc) {
            this.componentFragBegin = beginLoc;
            this.componentFragEnd = endLoc;
            return this;
        }

        public Builder tagType(String type) throws Exception {
            if (TagConstant.isValidTagType(type)) {
                this.tagType = type;
            } else throw new Exception("wrong tag type!");
            return this;
        }

        public Builder tokenLocation(int beginLoc, int endLoc) {
            this.beginTokenIndex = beginLoc;
            this.endTokenIndex = endLoc;
            return this;
        }
    }
        private FeatureParameter(Builder builder) {
            this.applyModel = builder.applyModel;
            this.beginTokenIndex = builder.beginTokenIndex;
            this.endTokenIndex = builder.endTokenIndex;
            this.componentFragBegin = builder.componentFragBegin;
            this.componentFragEnd = builder.componentFragEnd;
            this.isThisLineCode = builder.isThisLineCode;
            this.isThisLineEquation = builder.isThisLineEquation;
            this.isThisLineTable = builder.isThisLineTable;
            this.tree = builder.tree;
            this.tagType = builder.tagType;
        }
    }







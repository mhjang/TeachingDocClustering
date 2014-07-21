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
    private FragmentIndex componentFrag;
    private FragmentIndex tokenFrag;
    private final DEPTree tree;
    private final String tagType;

    public static class Builder {
        // required parameters
        private final boolean isThisLineCode;
        private final boolean isThisLineEquation;
        private final boolean isThisLineTable;
        private final boolean applyModel;
        // optional parameters
        private FragmentIndex componentFrag;
        private FragmentIndex tokenFrag;
        private DEPTree tree;
        private String tagType = null;

        public Builder(DEPTree tree_, boolean isThisLineCode_, boolean isThisLineEquation_, boolean isThisLineTable_, boolean applyModel_) {
            this.tree = tree_;
            this.isThisLineCode = isThisLineCode_;
            this.isThisLineEquation = isThisLineEquation_;
            this.isThisLineTable = isThisLineTable_;
            this.applyModel = applyModel_;
        }

        public Builder componentFrag(FragmentIndex componentFrag_) {
            this.componentFrag = componentFrag_;
            return this;
        }

        public Builder tagType(String type) throws Exception {
            if (TagConstant.isValidTagType(type)) {
                this.tagType = type;
            } else throw new Exception("wrong tag type!");
            return this;
        }

        public Builder tokenLocation(FragmentIndex tokenFrag_) {
            this.tokenFrag = tokenFrag_;
            return this;
        }

        public FeatureParameter build() {
            return new FeatureParameter(this);
        }
    }

    private FeatureParameter(Builder builder) {
        this.applyModel = builder.applyModel;
        this.componentFrag = builder.componentFrag;
        this.tokenFrag = builder.tokenFrag;
        this.isThisLineCode = builder.isThisLineCode;
        this.isThisLineEquation = builder.isThisLineEquation;
        this.isThisLineTable = builder.isThisLineTable;
        this.tree = builder.tree;
        this.tagType = builder.tagType;
    }

    public boolean isApplyModel() {
        return applyModel;
    }

    public String getTagType() {return tagType;   }

    public FragmentIndex getComponentFrag() { return componentFrag; }

    public FragmentIndex getTokenFrag()  { return  tokenFrag;    }

    public boolean isThisLineEquation() {     return isThisLineEquation;  }

    public boolean isThisLineCode() {     return isThisLineCode;  }

    public boolean isThisLineTable() {     return isThisLineTable;  }

}








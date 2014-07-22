package Classify.liblinear;

import Classify.TagConstant;
import com.clearnlp.dependency.DEPTree;

import javax.swing.text.html.HTML;

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
    private int currentIndex = 0;

    private final boolean excludeCode;
    private final boolean excludeTable;
    private final boolean excludeEqu;
    private final boolean excludeMisc;



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

        private boolean excludeTable = false;
        private boolean excludeCode = false;
        private boolean excludeEqu = false;
        private boolean excludeMisc = false;

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

        public Builder excludeTable() {
            this.excludeTable = true;
            return this;
        }

        public Builder excludeEquation() {
            this.excludeEqu = true;
            return this;
        }

        public Builder excludeCode() {
            this.excludeCode = true;
            return this;
        }

        public Builder excludeMisc() {
            this.excludeMisc = true;
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
        this.excludeCode = builder.excludeCode;
        this.excludeMisc = builder.excludeMisc;
        this.excludeEqu = builder.excludeEqu;
        this.excludeTable = builder.excludeTable;

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

    public DEPTree getParsingTree() {   return tree;    }

    // Because feature extraction happens tree by tree, the parameter set has to remember the index of the tree from whose features are extracted
    public void setCurrentIndex(int i) throws Exception {
        if(i >= tree.size()) throw new Exception("out of tree index exception");
        currentIndex = i;

    }
    public int getCurrentIndex() {
        return currentIndex;
    }

    public boolean isNoise(int prediction) {
        if(prediction == TagConstant.TEXT) return false;
        if(!excludeCode)
            if(prediction >= TagConstant.BEGINCODE && prediction <= TagConstant.ENDCODE) return true;
        if(!excludeEqu)
            if(prediction >= TagConstant.BEGINEQU && prediction <= TagConstant.ENDEQU) return true;
        if(!excludeTable)
            if(prediction >= TagConstant.BEGINTABLE && prediction <= TagConstant.ENDTABLE) return true;
        if(!excludeMisc)
            if(prediction >= TagConstant.BEGINMISC && prediction <= TagConstant.ENDMISC) return true;
        return true;
    }

}








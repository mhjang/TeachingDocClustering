package Classify.liblinear.datastructure;

import Classify.TagConstant;
import com.clearnlp.dependency.DEPTree;
import de.bwaldvogel.liblinear.Model;

/**
 * Created by mhjang on 7/18/14.
 * Using Java Builder pattern to handle many paramters
 */
public class FeatureParameter {
    // required parameters
    public static boolean predictPreviousNode = false;
    private double location;
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

    public static Model firstModel;
    public static Model secondModel;

    private String line;
    private String prev_1_line;
    private String prev_2_line;

    private double[] vector;
    public static boolean useSequentialFeature = true;
    public static boolean featureIndexReset = false;


    public static void setModelSet(Model m1, Model m2) {
        firstModel = m1;
        secondModel = m2;
    }

    public static void setFirstModel(Model m1) {
        firstModel = m1;
    }

    public static void setSecondModel(Model m2) {
        secondModel = m2;
    }

    public static class Builder {
        // required parameters
        // optional parameters
        private FragmentIndex componentFrag;
        private FragmentIndex tokenFrag;
        private DEPTree tree;
        private String tagType = null;

        private boolean excludeTable = false;
        private boolean excludeCode = false;
        private boolean excludeEqu = false;
        private boolean excludeMisc = false;
        private double[] vector = null;

        private String line;
        private String prev_1_line;
        private String prev_2_line;
        private Model model = null;
        private double location;

        // locaiton: relative location of the sentence in the document
        public Builder(DEPTree tree_, double location) {
            this.tree = tree_;
            this.location = location;
        }

        public Builder componentFrag(FragmentIndex componentFrag_) {
            this.componentFrag = componentFrag_;
            return this;
        }

        public Builder tagType(String type) throws Exception {
//            if (TagConstant.isValidTagType(type)) {
            this.tagType = type;
            //          } else throw new Exception("wrong tag type!");
            return this;
        }

        public Builder setVector(double[] vector) {
            this.vector = vector;
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

        public Builder model(Model model_) {
            this.model = model_;
            return this;
        }

        public Builder setLines(String prev_2_sentence, String prev_1_sentence, String sentence) {
            this.line = sentence;
            this.prev_1_line = prev_1_sentence;
            this.prev_2_line = prev_2_sentence;
            return this;
        }

        public FeatureParameter build() {
            return new FeatureParameter(this);
        }
    }

    private FeatureParameter(Builder builder) {
        this.componentFrag = builder.componentFrag;
        this.tokenFrag = builder.tokenFrag;
        this.location = builder.location;
        this.tree = builder.tree;
        this.tagType = builder.tagType;
        this.excludeCode = builder.excludeCode;
        this.excludeMisc = builder.excludeMisc;
        this.excludeEqu = builder.excludeEqu;
        this.excludeTable = builder.excludeTable;
        this.line = builder.line;
        this.prev_1_line = builder.prev_1_line;
        this.prev_2_line = builder.prev_2_line;
        this.vector = builder.vector;
    }

    public boolean isApplyModel() {
        return predictPreviousNode;
    }

    public String getTagType() {return tagType;   }

    public FragmentIndex getComponentFrag() { return componentFrag; }

    public FragmentIndex getTokenFrag()  { return  tokenFrag;    }

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
        return false;
    }

    public double[] getVector() {
        return vector;
    }
    public String getCurrentLine() { return line; }

    public String getPrev_1_line() { return prev_1_line; }

    public String getPrev_2_line() { return prev_2_line; }

}




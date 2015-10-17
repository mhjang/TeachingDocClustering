package Classify.liblinear;

import Classify.TagConstant;
import Classify.liblinear.datastructure.FeatureParameter;
import Classify.noisegenerator.TableGenerator;
import com.clearnlp.dependency.DEPNode;
import com.clearnlp.dependency.DEPTree;
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.SolverType;
import org.omg.Dynamic.Parameter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;


/**
 * Created by mhjang on 10/2/14.
 */
public abstract class BasicFeatureExtractor {

    static String initiatedTag = null;
    public static String[] startTags = {TagConstant.codeTag, TagConstant.tableTag, TagConstant.equTag, TagConstant.miscTag};
    public static String[] closetags = {TagConstant.codeCloseTag, TagConstant.tableCloseTag, TagConstant.equCloseTag, TagConstant.miscCloseTag};

    static HashMap<String, Integer> featureMap = new HashMap<String, Integer>();
    static HashMap<Integer, String> featureinverseMap = new HashMap<Integer, String>();

    // reserved keywords for designated feature index
    static String IS_SEMICOLON = "IS_SEMICOLON";
    static String IS_PARENTHESIS = "IS_PARENTHESIS";
    static String IS_CODELINE = "IS_CODELINE";
    static String IS_BRACKET = "IS_BRACKET";
    static String IS_COMMENT = "IS_COMMENT";
    static String IS_OPERATOR = "IS_OPERATOR";
    static String IS_VARIABLE = "IS_VARIABLE";
    static String IS_EQUATION = "IS_EQUATION";
    static String IS_TABLE = "IS_TABLE";
    static String PREVIOUS_LABEL = "PREVIOUS_LABEL";
    static String CHAR_LEN = "CHAR_LEN";
    static String KEYWORD_CONTAIN = "KEYWORD_CONTAIN";

    static protected boolean textualFeatureOn = true;
    static protected boolean ngramFeatureOn = true;

    static protected boolean equationBaselineOn = true;
    static protected boolean codeBaselineFeatureOn = true;
    static protected boolean tableBaselineOn = true;

    static protected boolean posFeatureOn = true;
    static protected boolean dependencyFeatureOn = true;

    static protected boolean structuralDependencyOn = false;
    static protected boolean structuralFeatureOn = true;
    int featureIdx = 0;
    int previousNodePrediction;

    boolean isLearningMode = true;


    SolverType solver = SolverType.L2R_L2LOSS_SVC; // -s 0
    double C = 1.0;    // cost of constraints violation
    double eps = 0.01; // stopping criteria



    // for debugging purpose
    ArrayList<String> originalTextLines = new ArrayList<String>();

    LinkedList<Feature[]> allFeatures;

    ArrayList<Double> answers = new ArrayList<Double>();
    public int featureNodeNum = 1;


    Model model;




    static class FeatureComparator<Feature> implements Comparator {

        @Override
        public int compare(Object o1, Object o2) {
            de.bwaldvogel.liblinear.Feature f1 = (de.bwaldvogel.liblinear.Feature) o1;
            de.bwaldvogel.liblinear.Feature f2 = (de.bwaldvogel.liblinear.Feature) o2;
            return (f1.getIndex() - f2.getIndex());
        }
    };

    FeatureComparator fc = new FeatureComparator();
    int[] componentCount;


    abstract public LinkedList<Feature[]> run(String baseDir, ArrayList<String> data, boolean learningMode);

    abstract protected int addFeature(FeatureParameter param);

    abstract public TableGenerator getTableGenerator();

    public HashMap<String, Integer> getFeatureDictionary() {
        return featureMap;
    }


    public void printEmbeddingNullRatio() {}
    public HashMap<Integer, String> getFeatureInverseDictionary() {
        return featureinverseMap;
    }

    int getFeatureIndex(String word) {
        word = word.toLowerCase();
        if (featureMap.containsKey(word))
            return featureMap.get(word);
        else {
            featureMap.put(word, featureNodeNum);
            featureinverseMap.put(featureNodeNum, word);
            featureNodeNum++;
        }
        return featureMap.get(word);
    }

    public HashMap<String, Integer> getFeatureIndexDic() {
        return featureMap;
    }

    static String getFeatureWord(int featureId) {
        return featureinverseMap.get(featureId);
    }
    protected void setModel(Model model_) {
        this.model = model_;
    }



    protected Model loadModel() {
        return model;
    }

    private void printTree(int n, DEPTree tree) {
        int size = tree.size();
        DEPNode node;
        System.out.print(n + ": ");
        for (int i = 1; i < size; i++) {
            node = tree.get(i);
            System.out.print(node.form + " ");
        }
        System.out.println();
    }

    }

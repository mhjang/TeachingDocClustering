package Classify.liblinear;

import Classify.StringTokenizerIndx;
import Classify.TagConstant;
import com.clearnlp.dependency.DEPArc;
import com.clearnlp.dependency.DEPNode;
import com.clearnlp.dependency.DEPTree;
import com.clearnlp.reader.DEPReader;
import com.clearnlp.util.UTInput;
import componentDetection.DetectCodeComponent;
import componentDetection.DetectEquation;
import componentDetection.DetectTable;
import de.bwaldvogel.liblinear.*;
import simple.io.myungha.DirectoryReader;
import simple.io.myungha.SimpleFileReader;
import simple.io.myungha.SimpleFileWriter;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by mhjang on 4/27/14.
 */


public class FeatureExtractor {


    static String initiatedTag = null;
    static String[] tags = {TagConstant.codeTag, TagConstant.tableTag, TagConstant.equTag, TagConstant.miscTag};
  //  static String[] tags = {TagConstant.codeTag};
    static String[] closetags = {TagConstant.codeCloseTag, TagConstant.tableCloseTag, TagConstant.equCloseTag, TagConstant.miscCloseTag};

    HashSet<DEPNode> traversedNodes;
    static HashMap<String, Integer> featureMap = new HashMap<String, Integer>();
    static HashMap<Integer, String> featureinverseMap = new HashMap<Integer, String>();


    static Pattern numberPattern = Pattern.compile("^[a-zA-Z]*([0-9]+).*");
    static Pattern puctPattern = Pattern.compile("\\p{Punct}");


    static FeatureNode feature1True = new FeatureNode(1, 1);
    static FeatureNode feature1False = new FeatureNode(1, 0);

    static FeatureNode feature2True = new FeatureNode(2, 1);
    static FeatureNode feature2False = new FeatureNode(2, 0);

    /**
     * reserved keywords for designated feature index
     */
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

    static private boolean textualFeatureOn = true;
    static private boolean ngramFeatureOn = true;

    static private boolean equationBaselineOn = true;
    static private boolean codeBaselineFeatureOn = true;
    static private boolean tableBaselineOn = true;

    static private boolean posFeatureOn = true;
    static private boolean dependencyFeatureOn = true;

    static private boolean structuralDependencyOn = false;
    static private boolean structuralFeatureOn = true;

    static class Component {
        public int begin, intermediate, end;

        public Component(int begin, int intermidiate, int end) {
            this.begin = begin;
            this.intermediate = intermidiate;
            this.end = end;
        }
    }

    LinkedList<Feature[]> allFeatures;

    Component table = new Component(TagConstant.BEGINTABLE, TagConstant.INTTABLE, TagConstant.ENDTABLE);
    Component code = new Component(TagConstant.BEGINCODE, TagConstant.INTCODE, TagConstant.ENDCODE);
    Component equation = new Component(TagConstant.BEGINEQU, TagConstant.INTEQU, TagConstant.ENDEQU);
    Component misc = new Component(TagConstant.BEGINMISC, TagConstant.INTMISC, TagConstant.ENDMISC);


    double[] answers = new double[1000000];
    double previousNodePrediction;
    int featureIdx = 0;

    static int featureNodeNum = 3;

    File modelFile = new File("slide_model_0608");
    Model model;
    int removedNode = 0, remainingNode = 0;

    static class FeatureComparator<Feature> implements Comparator {

        @Override
        public int compare(Object o1, Object o2) {
            de.bwaldvogel.liblinear.Feature f1 = (de.bwaldvogel.liblinear.Feature) o1;
            de.bwaldvogel.liblinear.Feature f2 = (de.bwaldvogel.liblinear.Feature) o2;
            return (f1.getIndex() - f2.getIndex());
        }
    }

    ;

    FeatureComparator fc = new FeatureComparator();
    int[] componentCount;

    public static void main(String[] args) {
        SVMClassifier svm = new SVMClassifier();
        final boolean useAnnotation = false;
        final boolean useModel = true;
        final boolean writeModel = true;
   //     String baseDir = "/Users/mhjang/Desktop/clearnlp/allslides/";
        String baseDir = "/Users/mhjang/Desktop/clearnlp/all/";
   //     String baseDir = "/Users/mhjang/Documents/teaching_documents/extracted/stemmed/parsed/gold/feature_tokens/";
        // routine 1: do five fold cross validation with annotation to evaluate the accuracy
 //       svm.runFiveFoldCrossValidation(baseDir, useAnnotation, writeModel);

        // routine 2: use all data for learning a model and use the model for five fold cross validation by predicting "previous_label" field
   //     String firstModel = "slide_model_0723";
   //     String secondModel = "final_slide_model";
        String firstModel = "acl_initial_model";
        String secondModel = "acl_final_model";
        svm.learnFirstModel(baseDir, firstModel);
        svm.learnSecondModel(baseDir, firstModel, secondModel);
   //     svm.runFiveFoldCrossValidation(baseDir, useModel, false);

        // routine 3: apply the learned model to generate the noise-free version of documents
        svm.applyModelToDocuments(firstModel, secondModel);


    }

    public HashMap<String, Integer> getFeatureDictionary() {
        return featureMap;
    }


    public HashMap<Integer, String> getFeatureInverseDictionary() {
        return featureinverseMap;
    }

    int getFeatureIndex(String word) {
        if (featureMap.containsKey(word))
            return featureMap.get(word);
        else {
            featureMap.put(word, featureNodeNum);
            featureinverseMap.put(featureNodeNum, word);
            featureNodeNum++;
        }
        return featureMap.get(word);
    }


    private void generateCodeRemovedDocuments() throws IOException {
        String baseDir = "/Users/mhjang/Documents/teaching_documents/extracted/stemmed/parsed/gold/feature_tokens/";
        String newDir = "/Users/mhjang/Documents/teaching_documents/extracted/stemmed/parsed/gold/code_removed/";

        DirectoryReader dr = new DirectoryReader(baseDir);
        for (String filename : dr.getFileNameList()) {
            System.out.println(filename);
            SimpleFileReader sr = new SimpleFileReader(baseDir + filename);
            SimpleFileWriter sw = new SimpleFileWriter(newDir + filename);

            while (sr.hasMoreLines()) {
                String line = sr.readLine();
                if (!DetectCodeComponent.isCodeLine(line))
                    sw.writeLine(line);
            }
            sw.close();
        }
    }

    /**
     * Extract features for applying the model
     *
     * @param
     * @return
     * @throws java.io.IOException
     */
    public void prepareFeatureForDocuments(String dir) throws IOException {
        //     read all annotated files from the directory
        //     String directory = "/Users/mhjang/Desktop/clearnlp/trainingdata/annotation/";
        // String parsedDir = "/Users/mhjang/Documents/teaching_documents/extracted/stemmed/parsed/";
        String noiseRemovedline;
        ArrayList<Double> ratios = new ArrayList<Double>();
        componentCount = new int[TagConstant.ENDMISC+1];
        System.out.println(dir);
        String featureTokenDir = dir + "feature_tokens/";
        String parsingDir = dir + "parsed/";

        DirectoryReader dr = new DirectoryReader(parsingDir);
        this.model = model;


    //    String featureTokenDir = dir + "annotation/";
    //    String featureTokenDir = dir + "feature_tokens/";
    //    String outputDir = dir + "noise_removed/";
        String outputDir = dir + "removed_tokens/";
        System.out.println("printing at " + outputDir);

        try {
            for (String filename : dr.getFileNameList()) {
                removedNode = 0;
                remainingNode = 0;
                DEPReader reader = new DEPReader(0, 1, 2, 3, 4, 5, 6);

                if (filename.contains(".DS_Store")) continue;
                // open a corresponding parsed file
                if (!filename.endsWith(".cnlp")) continue;
                reader.open(UTInput.createBufferedFileReader(parsingDir+ filename));
                System.out.println(parsingDir+ filename);
                DEPTree tree;
                ArrayList<DEPTree> treelist = new ArrayList<DEPTree>();
                while ((tree = reader.next()) != null) {
                    treelist.add(tree);

                }
                // then open an annotation file
                int treeIdx = 0;
                String line;
                String filename_ = filename.replace(".cnlp", "");
         //       System.out.println("printing at " + outputDir);
                SimpleFileReader freader = new SimpleFileReader(featureTokenDir + filename_);
                SimpleFileWriter fwriter = new SimpleFileWriter(outputDir + filename_);

           //     System.out.println(filename);

                boolean isTagBeginLine = false, tagClosed = false;
                String endTag = null;
                while (freader.hasMoreLines()) {
                    line = freader.readLine();
                    boolean treeIdxSkip = false;
                    line = line.trim();
                    if (!line.isEmpty()) {
                        LinkedList<String> tokens = new LinkedList<String>();
                        int startIdx = 0;
                        FeatureParameter param = new FeatureParameter.Builder(treelist.get(treeIdx), DetectCodeComponent.codeLineEvidence(line), DetectCodeComponent.keywordContainSize(line),DetectEquation.isEquation(line), DetectTable.isTable(line), true).build();
                        noiseRemovedline = generateApplyFeatures(param);
                        treeIdx++;
                        fwriter.writeLine(noiseRemovedline);
                    }

                }
                fwriter.close();
                double ratio = (double) removedNode / (double) (removedNode + remainingNode);
 //               System.out.println("Noise Ratio: " + ratio);
                ratios.add(ratio);
                // print the number of components predicted
                int sum = 0;
                System.out.print(filename + "\t");
                for (int i = 0; i < 5; i++) {
                    sum += componentCount[i];
                    System.out.print(componentCount[i] + "\t");
                    // then initialize
                    componentCount[i] = 0;
                }
                System.out.println(sum);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("# of removed nodes: " + removedNode);
        double ratioSum = 0.0;
        for (Double d : ratios) {
            ratioSum += d;
        }
   //     System.out.println("Average removed noise ratio: " + ratioSum / (double) ratios.size());
    }

    private Model loadModel() {
        return model;
    }

    protected void setModel(Model model_) {
        this.model = model_;
    }

    /**
     * Extract features for building the model
     *
     * @param data
     * @param applyModel: whether you use a trained model to fill out "previous label" feature
     * @return
     * @throws java.io.IOException
     */
    public LinkedList<Feature[]> generateClassifyFeaturesTokenBased(String baseDir, ArrayList<String> data, boolean applyModel) throws IOException {
        //     read all annotated files from the directory
        //     String directory = "/Users/mhjang/Desktop/clearnlp/trainingdata/annotation/";
        // baseDir = "/Users/mhjang/Desktop/clearnlp/allslides/";
        allFeatures = new LinkedList<Feature[]>();
        String parsedDir = baseDir + "parsed/";
        String annotationDir = baseDir + "annotation/";
        if (applyModel) {
            model = loadModel();
        }
        try {
            for (String filename : data) {
                DEPReader reader = new DEPReader(0, 1, 2, 3, 4, 5, 6);

                if (filename.contains(".DS_Store")) continue;
                // open a corresponding parsed file
                reader.open(UTInput.createBufferedFileReader(parsedDir + filename + ".cnlp"));
                DEPTree tree;
                ArrayList<DEPTree> treelist = new ArrayList<DEPTree>();
                while ((tree = reader.next()) != null) {
                    treelist.add(tree);

                }
                // then open an  file
                int treeIdx = 0;
                String line;
                SimpleFileReader freader = new SimpleFileReader(annotationDir + filename);
                //     System.out.println("loading " + filename);

                boolean isTagBeginLine = false, tagClosed = false;
                String endTag = null;

                //initialize component count

                componentCount = new int[5];
                for (int i = 0; i < 5; i++) {
                    componentCount[i] = 0;
                }
                int numOfTokensInDoc = 0;


                while (freader.hasMoreLines()) {
                    line = freader.readLine();
                    /**
                     * beginToken: begin of the component over the lines. It is set only if the begin tag is present in the current line; Otherwise set to -1
                     endToken: end of the component over the lines. It is set only if the end tag is present in the current line; Otherwise set to -1
                     componentBegin: begin of the component in this line.
                     componentEnd: begin of the component in this line.
                     */
                    int beginToken = -1, endToken = -1;
                    int componentBegin = -1, componentEnd = -1;
                    /**
                     * treeIdxSkip: a flag that determines whether or not to skip current line
                     * clearNLP skipped an empty line. To find the matching tree, an empty line in the annotation should also be skipped.
                     *
                     */
                    boolean treeIdxSkip = false;
                    line = line.trim();

                    if (!line.isEmpty()) {
                        int startIdx = 0;
                        // If currently no tag was opened
                        if (initiatedTag == null) {
                            for (String tag : tags) {
                                if (line.contains(tag)) {
                                    initiatedTag = tag;
                                    isTagBeginLine = true;
                                    break;
                                }
                            }
                        }
                        if (initiatedTag != null) {
                            /**
                             * If initiated tag is JUST SET, that means we have a begin tag in this line.
                             (1) To set this location to beginToken, first find the character offset of this begin tag to find the token location
                             (2) Find the matching end tag
                             */
                            if (isTagBeginLine) {
                                endTag = TagConstant.findMatchingEndTag(initiatedTag);
                                startIdx = (line.indexOf(initiatedTag) + initiatedTag.length() + 1 < line.length()) ? line.indexOf(initiatedTag) + initiatedTag.length() + 1 : 0;
                                // because of the tag itself, minus one
                                beginToken = componentBegin = StringTokenizerIndx.findTokenNthfromIndex(line, startIdx) - 1; // location - (startTag)
                                // If a line only contains a tag, in the original file that clearNLP parsed on, that line was empty.
                                if (line.replace(initiatedTag, "").trim().length() == 0) treeIdxSkip = true;
                            } else {
                                // the component is being continued from previous lines
                                componentBegin = 0;
                            }
                            int endIdx;
                            if (line.contains(endTag)) {
                                if (line.replace(endTag, "").trim().length() == 0) treeIdxSkip = true;
                                endIdx = line.indexOf(endTag);
                                tagClosed = true;
                            } else {
                                endIdx = line.length();
                            }
                            // If there is a begin tag in the line, subtract one from the found index
                            if (isTagBeginLine) {
                                componentEnd = StringTokenizerIndx.findTokenNthfromIndex(line, endIdx) - 1;
                                isTagBeginLine = false;
                            } else
                                componentEnd = StringTokenizerIndx.findTokenNthfromIndex(line, endIdx);

                            if (tagClosed) endToken = componentEnd;
                        }
                        if (treeIdxSkip) continue;
                        int keywordContain = DetectCodeComponent.keywordContainSize(line);
                        FeatureParameter param;
                     /*   if(initiatedTag != null && initiatedTag == TagConstant.codeTag)
                                param = new FeatureParameter.Builder(treelist.get(treeIdx), DetectCodeComponent.isCodeLine(line), keywordContain, DetectEquation.isEquation(line), DetectTable.isTable(line), applyModel).componentFrag(new FragmentIndex(componentBegin, componentEnd)).tagType(initiatedTag).tokenLocation(new FragmentIndex(beginToken, endToken)).build();
                        else
                                param = new FeatureParameter.Builder(treelist.get(treeIdx), DetectCodeComponent.isCodeLine(line), keywordContain, DetectEquation.isEquation(line), DetectTable.isTable(line), applyModel).componentFrag(new FragmentIndex(componentBegin, componentEnd)).tagType(null).tokenLocation(new FragmentIndex(beginToken, endToken)).build();
                        */
                        param = new FeatureParameter.Builder(treelist.get(treeIdx), DetectCodeComponent.codeLineEvidence(line), keywordContain, DetectEquation.isEquation(line), DetectTable.isTable(line), applyModel).componentFrag(new FragmentIndex(componentBegin, componentEnd)).tagType(initiatedTag).tokenLocation(new FragmentIndex(beginToken, endToken)).build();

                        buildFeatureVectorForTree(param);
                        numOfTokensInDoc += treelist.get(treeIdx).size() - 1;
                        if (tagClosed) {
                            initiatedTag = null;
                            tagClosed = false;
                        }

                        treeIdx++;
                    }

                }
                //      for(int i=0; i<5; i++) {
                //          System.out.print(((double)componentCount[i] / (double)numOfTokensInDoc) + "\t");
                //      }
                //     System.out.println(numOfTokensInDoc);
                //     System.out.println();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return allFeatures;
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


    /**
     *
     */
    private LinkedList<Feature> extractFeatures(FeatureParameter param) {
        LinkedList<Feature> features = new LinkedList<Feature>();
        DEPTree tree = param.getParsingTree();
        int i = param.getCurrentIndex(), size = tree.size(), featureIndx;
        DEPNode node = tree.get(i);
        List<DEPArc> dependents = node.getDependents();
        // check feature index duplicate
        HashSet<Integer> featureIndex = new HashSet<Integer>();
        /***********************************************
         *                 Textual Features            *
         ***********************************************/
        if (textualFeatureOn) {
            // Feature 1: Does this token contain numbers?

            Matcher m1 = numberPattern.matcher(node.form);
            if (m1.find())
                features.add(feature1True);
            else
                features.add(feature1False);

            // Feature 2: Is this token a punctuation?


            Matcher m2 = puctPattern.matcher(node.form);
            // whether token is a punctuation
            if (m2.find())
                features.add(feature2True);
            else
                features.add(feature2False);

            // Feature 3: the length of the token

            int charlen = node.form.length();
            features.add(new FeatureNode(getFeatureIndex(CHAR_LEN), charlen));

        }

        if (ngramFeatureOn) {

            // Feature 4: 1-gram of the token

            featureIndx = getFeatureIndex(node.form);
            if (!featureIndex.contains(featureIndx)) {
                features.add(new FeatureNode(getFeatureIndex(node.form), 1));
                featureIndex.add(featureIndx);
            }

            // bi-gram - previous token

            featureIndx = getFeatureIndex("P_" + tree.get(i - 1).form);
            if (!featureIndex.contains(featureIndx)) {
                features.add(new FeatureNode(featureIndx, 1));
                featureIndex.add(featureIndx);
            }

            // bi-gram - next token

            if (i != size - 1)
                featureIndx = getFeatureIndex("N_" + tree.get(i + 1).form);

            else
                featureIndx = getFeatureIndex("N_END");

            if (!featureIndex.contains(featureIndx)) {
                features.add(new FeatureNode(featureIndx, 1));
                featureIndex.add(featureIndx);
            }

            /**
             * bi-gram - bigram token (prev + current)
             */
            featureIndx = getFeatureIndex(tree.get(i - 1).form + " " + node.form);
            if (!featureIndex.contains(featureIndx)) {
                features.add(new FeatureNode(featureIndx, 1));
                featureIndex.add(featureIndx);
            }


            /**
             * bi-gram - bigram token (prev + current)
             */
            if (i != size - 1)
                featureIndx = getFeatureIndex(node.form + " " + tree.get(i + 1).form);
            else
                featureIndx = getFeatureIndex(node.form + " " + "N_END");
            if (!featureIndex.contains(featureIndx)) {
                features.add(new FeatureNode(featureIndx, 1));
                featureIndex.add(featureIndx);
            }
        }

        /****************************
         * Code component baseline
         ****************************/
        if (codeBaselineFeatureOn) {
            /**
             * Is this token a bracket?
             */

            features.add(new FeatureNode(getFeatureIndex(IS_BRACKET), DetectCodeComponent.isBracket(node.form) ? 1 : 0));

            /**
             * Is this token a Camal case variable name?
             */

            features.add(new FeatureNode(getFeatureIndex(IS_VARIABLE), DetectCodeComponent.isVariable(node.form) ? 1 : 0));

            /**
             * Is this token a comment "//"?
             */

            features.add(new FeatureNode(getFeatureIndex(IS_COMMENT), DetectCodeComponent.isComment(node.form) ? 1 : 0));

            /**
             * Is this token a programming operator?
             */

            features.add(new FeatureNode(getFeatureIndex(IS_OPERATOR), DetectCodeComponent.isOperator(node.form) ? 1 : 0));

            /**
             * Is this token a parenthesis?
             */

            features.add(new FeatureNode(getFeatureIndex(IS_PARENTHESIS), DetectCodeComponent.isParenthesis(node.form) ? 1 : 0));

            /**
             * Is this token a parenthesis?
             */

            features.add(new FeatureNode(getFeatureIndex(IS_SEMICOLON), DetectCodeComponent.isSemicolon(node.form) ? 1 : 0));

            /**
             * Is this line of the token a code line?
             */
            features.add(new FeatureNode(getFeatureIndex(IS_CODELINE), param.isThisLineCode()));

            /**
             * How many programming keywords does this line contain?
             */
            features.add(new FeatureNode(getFeatureIndex(KEYWORD_CONTAIN), param.getKeywordContain()));
        }

        /****************************
         * Equation component baseline
         ****************************/
        if (equationBaselineOn)
            features.add(new FeatureNode(getFeatureIndex(IS_EQUATION), param.isThisLineEquation() ? 1 : 0));


        /****************************
         * Table component baseline
         ****************************/
        if (tableBaselineOn)
            features.add(new FeatureNode(getFeatureIndex(IS_TABLE), param.isThisLineTable() ? 1 : 0));


        /**************************************************
         *      Parsing features (1) POS TAG Features     *
         * ************************************************/

        // POS tag of the token

        if (posFeatureOn) {
            featureIndx = getFeatureIndex(node.pos);
            if (!featureIndex.contains(featureIndx)) {
                features.add(new FeatureNode(featureIndx, 1));
                featureIndex.add(featureIndx);
            }

            // POS tags of the token's dependents

            for (DEPArc arc : dependents) {
                featureIndx = getFeatureIndex("D_" + arc.getNode().pos);
                if (!featureIndex.contains(featureIndx)) {
                    features.add(new FeatureNode(featureIndx, 1));
                    featureIndex.add(featureIndx);
                }
            }

            // POS tags of the token's heads

            if (node.hasHead()) {
                features.add(new FeatureNode(getFeatureIndex("H_" + node.getHead().pos), 1));
            }

            // Prev word (i-1) + POS tag of the current word


            featureIndx = getFeatureIndex(tree.get(i - 1).form + " " + node.pos);
            if (!featureIndex.contains(featureIndx)) {
                features.add(new FeatureNode(featureIndx, 1));
                featureIndex.add(featureIndx);
            }

            // POS tag of the prev word + the current word


            featureIndx = getFeatureIndex(tree.get(i - 1).pos + " " + node.form);
            if (!featureIndex.contains(featureIndx)) {
                features.add(new FeatureNode(featureIndx, 1));
                featureIndex.add(featureIndx);
            }

            // the current word + POS tag of the next word


            if (i != size - 1) {
                featureIndx = getFeatureIndex(node.form + " " + tree.get(i + 1).pos);
                if (!featureIndex.contains(featureIndx)) {
                    features.add(new FeatureNode(featureIndx, 1));
                    featureIndex.add(featureIndx);
                }
            }

            // the current pos + the next word


            if (i != size - 1) {
                featureIndx = getFeatureIndex(node.pos + " " + tree.get(i + 1).form);
                if (!featureIndex.contains(featureIndx)) {
                    features.add(new FeatureNode(featureIndx, 1));
                    featureIndex.add(featureIndx);
                }
            }
        }

        /**************************************************
         *      Parsing features (2) RELATION Features     *
         * ************************************************/
        if (dependencyFeatureOn) {
            for (DEPArc arc : dependents) {
                featureIndx = getFeatureIndex(arc.getNode().getLabel());
                if (!featureIndex.contains(featureIndx)) {
                    features.add(new FeatureNode(featureIndx, 1));
                    featureIndex.add(featureIndx);
                }
            }


        }
        /**************************************************
         *      Structural Features                       *
         **************************************************/
        if (structuralFeatureOn) {
            if (featureIdx > 0)
                if (param.isApplyModel())
                    features.add(new FeatureNode(getFeatureIndex(PREVIOUS_LABEL), previousNodePrediction));
                else features.add(new FeatureNode(getFeatureIndex(PREVIOUS_LABEL), answers[featureIdx - 1]));
            else
                features.add(new FeatureNode(getFeatureIndex(PREVIOUS_LABEL), 0));

        }
        return features;
    }

    /**
     * Given the tag type extractef from the annotation, it returns Component object for generating features
     *
     * @param tagType
     * @return
     */
    private Component getComponent(String tagType) {
        Component component;
        if (tagType == null) {
            component = null;
        } else if (tagType == TagConstant.tableTag) {
            component = table;
        } else if (tagType == TagConstant.codeTag) {
            component = code;
        } else if (tagType == TagConstant.equTag) {
            component = equation;
        } else {
            component = misc;
        }
        return component;
    }

    // this includes annotation tag, features, and using annotation as "previous label" features for cross validation
    private void buildFeatureVectorForTree(FeatureParameter param) throws Exception {
        Component component = getComponent(param.getTagType());
        DEPTree tree = param.getParsingTree();
        int i, size = tree.size();
        tree.resetDependents();
        for (i = 1; i < size; i++) {
            param.setCurrentIndex(i);
            LinkedList<Feature> features = extractFeatures(param);

            // determine whether the given token is the begin / middle / end of the component
            if (component != null) {
                if (param.getTokenFrag().getStart() == i - 1) answers[featureIdx] = component.begin;
                else if (param.getTokenFrag().getStart() + 1 == i - 1 && param.getComponentFrag().getEnd() > i - 1)
                    answers[featureIdx] = component.intermediate;
                else if (param.getTokenFrag().getEnd() - 1 == i - 1) answers[featureIdx] = component.end;
                else if (param.getComponentFrag().getStart() <= i - 1 && param.getComponentFrag().getEnd() > i - 1)
                    answers[featureIdx] = component.intermediate;
                else
                    answers[featureIdx] = TagConstant.TEXT;

            } else {
                answers[featureIdx] = TagConstant.TEXT;
            }

            Collections.sort(features, fc);
            Feature[] featureArray;
            featureArray = features.toArray(new Feature[features.size()]);

            // save current prediction to use as a feature for the next label
            if (param.isApplyModel()) {
                previousNodePrediction = Linear.predict(FeatureParameter.firstModel, featureArray);
            }
            featureIdx++;
            allFeatures.add(featureArray);
        }
    }

    private String generateApplyFeatures(FeatureParameter param) throws Exception {
        DEPTree tree = param.getParsingTree();
        int i, size = tree.size();
        tree.resetDependents();
        StringBuilder noiseRemovedLine = new StringBuilder();
        for (i = 1; i < size; i++) {
            param.setCurrentIndex(i);
            LinkedList<Feature> features = extractFeatures(param);
            Collections.sort(features, fc);

            Feature[] featureArray;
            featureArray = features.toArray(new Feature[features.size()]);
            int prediction = (int) Linear.predict(FeatureParameter.secondModel, featureArray);
            if (param.isNoise(prediction))
                noiseRemovedLine.append(tree.get(i).form + " ");
        //    else {
        //        System.out.println(TagConstant.getTagLabel(prediction) + ": "  + tree.get(i).form);
        //cd    }
            componentCount[prediction]++;

        }
        return noiseRemovedLine.toString();
    }


}




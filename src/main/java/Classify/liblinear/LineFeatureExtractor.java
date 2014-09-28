package Classify.liblinear;

import Classify.StringTokenizerIndx;
import Classify.TagConstant;
import com.clearnlp.dependency.DEPArc;
import com.clearnlp.dependency.DEPNode;
import com.clearnlp.dependency.DEPTree;
import com.clearnlp.reader.DEPReader;
import com.clearnlp.util.UTInput;
import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import componentDetection.DetectCodeComponent;
import componentDetection.DetectEquation;
import componentDetection.DetectTable;
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import simple.io.myungha.DirectoryReader;
import simple.io.myungha.SimpleFileReader;
import simple.io.myungha.SimpleFileWriter;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by mhjang on 8/25/14.
 */
public class LineFeatureExtractor {


    static String initiatedTag = null;
    static String[] tags = {TagConstant.codeTag, TagConstant.tableTag, TagConstant.equTag, TagConstant.miscTag};
   //static String[] tags = {TagConstant.codeTag, TagConstant.tableTag, TagConstant.equTag};
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

    static private boolean equationBaselineOn = false;
    static private boolean codeBaselineFeatureOn = true;
    static private boolean tableBaselineOn = false;

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
    String[] originalLines = new String[1000000];
    double previousNodePrediction;
    int featureIdx = 0;

    static int featureNodeNum = 3;

    File modelFile = new File("slide_model_0608");
    Model model;
    int removedNode = 0, remainingNode = 0;
    int editDistanceSum = 0;
    int editDistanceCount = 0;
    int editDistanceSumForTable = 0;
    int editDistanceCountForTable = 0;

    int linelinegthForTable = 0;
    int linelinegth = 0;
    double patternProbWeightedED = 0.0;
    double patternProbWeightedEDForTable = 0.0;

    double rightMatchingRatioForTable = 0.0;
    double rightMatchingRatioForOther = 0.0;

    double leftMatchingRatioForOther = 0.0;
    double leftMatchingRatioForTable = 0.0;

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
        // String baseDir = "/Users/mhjang/Desktop/clearnlp/allslides/";
        // String baseDir = "/Users/mhjang/Desktop/clearnlp/all/";
        String baseDir = "/Users/mhjang/Desktop/clearnlp/dataset/acl/";
        //     String baseDir = "/Users/mhjang/Documents/teaching_documents/extracted/stemmed/parsed/gold/feature_tokens/";
        // routine 1: do five fold cross validation with annotation to evaluate the accuracy
        svm.runFiveFoldCrossValidation(baseDir, useAnnotation, writeModel);

        // routine 2: use all data for learning a model and use the model for five fold cross validation by predicting "previous_label" field
        String firstModel = "slide_model_0723";
        String secondModel = "final_slide_model";
        //     svm.learnFirstModel(baseDir, firstModel);
        //     svm.learnSecondModel(baseDir, firstModel, secondModel);
        //     svm.runFiveFoldCrossValidation(baseDir, useModel, false);

        // routine 3: apply the learned model to generate the noise-free version of documents
        //     svm.applyModelToDocuments(firstModel, secondModel);


    }

    public HashMap<String, Integer> getFeatureDictionary() {
        return featureMap;
    }


    public HashMap<Integer, String> getFeatureInverseDictionary() {
        return featureinverseMap;
    }

    static int getFeatureIndex(String word) {
        if (featureMap.containsKey(word))
            return featureMap.get(word);
        else {
            featureMap.put(word, featureNodeNum);
            featureinverseMap.put(featureNodeNum, word);
            featureNodeNum++;
        }
        return featureMap.get(word);
    }

    static String getFeatureWord(int featureId) {
        return featureinverseMap.get(featureId);
    }
    protected void setModel(Model model_) {
        this.model = model_;
    }



    private Model loadModel() {
        return model;
    }

    /**
     * Extract features for building the model
     *
     * @param data
     * @param applyModel: whether you use a trained model to fill out "previous label" feature
     * @return
     * @throws java.io.IOException
     */
    public LinkedList<Feature[]> generateClassifyFeaturesLineBased(String baseDir, ArrayList<String> data, boolean applyModel) throws IOException {
        //     read all annotated files from the directory
        //     String directory = "/Users/mhjang/Desktop/clearnlp/trainingdata/annotation/";
        // baseDir = "/Users/mhjang/Desktop/clearnlp/allslides/";
        allFeatures = new LinkedList<Feature[]>();
        String parsedDir = baseDir + "parsed/";
        String annotationDir = baseDir + "annotation/";
        String originalDir = baseDir + "text/";
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
                System.out.println(annotationDir + filename);
                // then open an annotation file
                int treeIdx = 0;
                String line = null, nextLine = null;
                SimpleFileReader freader = new SimpleFileReader(annotationDir + filename);
                //      System.out.println(filename);

                SimpleFileReader treader = new SimpleFileReader(originalDir + filename);

                boolean isTagBeginLine = false, tagClosed = false;
                String endTag = null;

                //initialize component count

                componentCount = new int[5];
                for (int i = 0; i < 5; i++) {
                    componentCount[i] = 0;
                }
                int numOfTokensInDoc = 0;
                // current line - 1 previous line
                String prevLine_1 = "";
                // current line - 2 previous line
                String prevLine_2 = "";
                String textLine;


                ArrayList<String> annotatedLines = new ArrayList<String>();
                String l = "";
                String[] tags = TagConstant.getTags();
                String tagRemoved = "";
                while (freader.hasMoreLines()) {
                    l = freader.readLine();
                    tagRemoved = l;
                    for (String t : tags) {
                        tagRemoved = tagRemoved.replace(t, "");
                    }
                    if (tagRemoved.isEmpty() || tagRemoved.trim().length() == 0) continue;
                    else annotatedLines.add(l);
                }

                ArrayList<String> originalLines = new ArrayList<String>();
                while (treader.hasMoreLines()) {
                    l = treader.readLine();
                    if (l.isEmpty() || l.trim().length() == 0 || l.replace(" ", "").length() == 0) continue;
                    else originalLines.add(l);
                }

//                System.out.println(annotatedLines.size() + " lines vs " + originalLines.size());

                for (int i = 0; i < annotatedLines.size(); i++) {
                    boolean treeIdxSkip = false;
                    line = annotatedLines.get(i);

                    textLine = originalLines.get(i);
                    if(i == 0) {
                        prevLine_1 = "";
                        prevLine_2 = "";
                    }
                    else if(i == 1) {
                        prevLine_1 = originalLines.get(i-1);
                        prevLine_2 = "";
                    }
                    else {
                        prevLine_1 = originalLines.get(i-1);
                        prevLine_2 = originalLines.get(i-2);
                    }


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

                        if (line.replace("</EQUATION>", "").trim().isEmpty()) treeIdxSkip = true;
                        else if (line.replace("</TABLE>", "").trim().isEmpty()) treeIdxSkip = true;
                        else if (line.replace("</CODE>", "").trim().isEmpty()) treeIdxSkip = true;
                        else if (line.replace("</MISCELLANEOUS>", "").trim().isEmpty()) treeIdxSkip = true;
                        else if (line.replace("<MISCELLANEOUS>", "").trim().isEmpty()) treeIdxSkip = true;
                        else if (line.replace("<CODE>", "").trim().isEmpty()) treeIdxSkip = true;
                        else if (line.replace("<TABLE>", "").trim().isEmpty()) treeIdxSkip = true;
                        else if (line.replace("<EQUATION>", "").trim().isEmpty()) treeIdxSkip = true;
                        System.out.println(line);
                        if (initiatedTag != null) {
                            // If initiated tag is JUST SET, that means we have a begin tag in this line.
                            // (1) To set this location to beginToken, first find the character offset of this begin tag to find the token location
                            // (2) Find the matching end tag
                            System.out.println("initiated tag: " + initiatedTag);
                            if (isTagBeginLine) {
                                endTag = TagConstant.findMatchingEndTag(initiatedTag);
                                if (line.contains(endTag)) {
                                    tagClosed = true;
                                }
                                isTagBeginLine = false;
                            }
                            if (line.contains(endTag)) {
                                tagClosed = true;

                            }
                        }


                        if (treeIdxSkip) continue;
                        int keywordContain = DetectCodeComponent.keywordContainSize(line);
                        FeatureParameter param;

                        param = new FeatureParameter.Builder(treelist.get(treeIdx), DetectCodeComponent.codeLineEvidence(line), keywordContain, DetectEquation.isEquation(line), DetectTable.isTable(line), applyModel).tagType(initiatedTag).setLines(prevLine_2, prevLine_1, textLine).build();

                        buildFeatureVectorForTree(param);

                        if (tagClosed) {
                            initiatedTag = null;
                            tagClosed = false;
                        }

                        treeIdx++;
                        prevLine_2 = prevLine_1;
                        prevLine_1 = textLine;
//                        System.out.println(line + " vs " + textLine);

                    }

                }
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        System.out.println("average of edit distance count : " + (double)editDistanceSum / (double)editDistanceCount);
        System.out.println("average of edit distance count for table : " + (double)editDistanceSumForTable / (double)editDistanceCountForTable);
        System.out.println("average of weighted edit distance count : " + (double)patternProbWeightedED / (double)editDistanceCount);
        System.out.println("average of weighted edit distance count for table : " + (double)patternProbWeightedEDForTable / (double)editDistanceCountForTable);
        System.out.println("average of line length : " + (double)linelinegthForTable / (double)editDistanceCount);
        System.out.println("average of line length for table : " + (double)linelinegth / (double)editDistanceCountForTable);
        System.out.println("average of right matching  : " + (double)rightMatchingRatioForOther + ","  + (double)editDistanceCount);
        System.out.println("average of right matching for table : " + (double)rightMatchingRatioForTable  / (double)editDistanceCountForTable);
        System.out.println("average of left matching  : " + (double)leftMatchingRatioForOther / (double)editDistanceCount);
        System.out.println("average of left matching for table : " + (double)leftMatchingRatioForTable / (double)editDistanceCountForTable);

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
     * line-based extraction
     */
    private LinkedList<Feature> extractFeatures(FeatureParameter param) {
        LinkedList<Feature> features = new LinkedList<Feature>();
        DEPTree tree = param.getParsingTree();
        int size = tree.size(), featureIndx;
        Multiset<Integer> dependentRelationBag = HashMultiset.create();
        Multiset<Integer> grams = HashMultiset.create();
        Multiset<Integer> codeDetect = HashMultiset.create();
        Multiset<Integer> posTagBag = HashMultiset.create();
        // line-based feature

        /****************************
         * Table component baseline
         ****************************/
        String line = param.getCurrentLine().replace("<TABLE>","").replace("</TABLE>","");
        String prev_1_line = param.getPrev_1_line().replace("<TABLE>","").replace("</TABLE>","");
        String prev_2_line = param.getPrev_2_line().replace("<TABLE>","").replace("</TABLE>","");

        //    if (tableBaselineOn)
        //        features.add(new FeatureNode(getFeatureIndex(IS_TABLE), DetectTable.getEditDistance(DetectTable.encodeString(line), DetectTable.encodeString(prev_1_line))));

        String rline = DetectTable.encodeString(DetectTable.restoreParenthesis(line));
        String rprev_1_line = DetectTable.encodeString(DetectTable.restoreParenthesis(prev_1_line));
        String rprev_2_line = DetectTable.encodeString(DetectTable.restoreParenthesis(prev_2_line));
      //  int edcount = DetectTable.getEditDistance(rline, rprev_1_line);
           int edcount = DetectTable.getEditDistance(rline, rprev_1_line);
        int edcount2 = DetectTable.getEditDistance(rline, rprev_2_line);
        double probability = DetectTable.tableLMProbability(rline);
        double rightMatchingRatio = DetectTable.getRatioRightMatching(rline, rprev_1_line);
      //  double leftMatchingRatio = DetectTable.getRatioLeftMatching(rline, rprev_1_line);

      //  System.out.println(rline + ": " + DetectTable.getNumberTokenCount(rline));

        //      int edcount2 = DetectTable.getEditDistance(rline, rprev_2_line);

        HashSet<Integer> featureNodeIndex = new HashSet<Integer>();

   /*     if(param.getTagType() == TagConstant.tableTag) {
        //    System.out.println(DetectTable.encodeString(rline) + " vs " + DetectTable.encodeString(rprev_1_line));
            System.out.println(line + "vs " + prev_1_line);
             System.out.println(rline + " vs " + rprev_1_line + ": " + edcount + ":" + probability + ": " + rightMatchingRatio);
            editDistanceSumForTable += edcount;
            editDistanceCountForTable++;
            patternProbWeightedEDForTable += edcount * probability;
            linelinegthForTable += rline.length();
            rightMatchingRatioForTable += rightMatchingRatio;
            leftMatchingRatioForTable += leftMatchingRatio;


            //       DetectTable.addAnnotationLine(DetectTable.encodeString(rline));
        }

        else {
            editDistanceSum += edcount;
            editDistanceCount++;
            patternProbWeightedED += edcount * probability;
            linelinegth += rline.length();
            rightMatchingRatioForOther += rightMatchingRatio;
            leftMatchingRatioForOther += leftMatchingRatio;

        }
*/

        features.add(new FeatureNode(getFeatureIndex("EDIT_DISTANCE"), edcount));
        features.add(new FeatureNode(getFeatureIndex("EDIT_DISTANCE2"), edcount2));
        features.add(new FeatureNode(getFeatureIndex("COMPLEXITY"), edcount + probability));
   //    features.add(new FeatureNode(getFeatureIndex("WEIGHTED_ED"), edcount * probability));
   //     features.add(new FeatureNode(getFeatureIndex))
        features.add(new FeatureNode(getFeatureIndex(rline), 1));
 //       features.add(new FeatureNode(getFeatureIndex("NUMBERTOKEN"), DetectTable.getNumberTokenCount(rline)));
  //      features.add(new FeatureNode(getFeatureIndex("NUMBERTOKEN2"), Math.abs(DetectTable.getNumberTokenCount(rline) - DetectTable.getNumberTokenCount(rprev_1_line))));
        features.add(new FeatureNode(getFeatureIndex("LENGTH_DIFF"), Math.abs(rline.length() - rprev_1_line.length())));
  //      features.add(new FeatureNode(getFeatureIndex("RIGHTMATCHING"), DetectTable.getRatioRightMatching(line, prev_1_line)));
        features.add(new FeatureNode(getFeatureIndex("RIGHT_MATCHING"), rightMatchingRatio));
  //      features.add(new FeatureNode(getFeatureIndex("LEFT_MATCHING"), leftMatchingRatio));

        featureNodeIndex.add(getFeatureIndex(rline));

      /*  if(param.getTagType() == TagConstant.codeTag) {
            System.out.println("code:" + DetectTable.restoreParenthesis(line));
            Multiset<String> d = DetectCodeComponent.getCodeEvidence(line);
            for (String dd : d.elementSet()) {
                System.out.print(dd + ": " + d.count(dd));
            }
        }
        */
      //  if(param.getTagType() == TagConstant.miscTag) {
      //      System.out.println("MISC:" + DetectTable.restoreParenthesis(line));

      //  }



        for(int i=1; i<size; i++) {
            DEPNode node = tree.get(i);
            List<DEPArc> dependents = node.getDependents();
            // check feature index duplicate
            HashSet<Integer> featureIndex = new HashSet<Integer>();
            for (DEPArc darc : dependents) {
                int depId = getFeatureIndex(darc.getLabel());
                dependentRelationBag.add(getFeatureIndex(node.pos + " " + darc.getNode().pos));
                dependentRelationBag.add(depId);
            }


            // unigram feature
            grams.add(getFeatureIndex(node.form));
            // bigram features
            if(i<size-1) {
               grams.add(getFeatureIndex(node.form +" " +  tree.get(i+1).form));
            }
            posTagBag.add(getFeatureIndex(node.pos));

            /****************************
             * Code component baseline
             ****************************/

            if (codeBaselineFeatureOn) {
                /**
                 * Is this token a bracket?
                 */

                //   features.add(new FeatureNode(getFeatureIndex(IS_BRACKET), DetectCodeComponent.isBracket(node.form) ? 1 : 0));
                codeDetect.add(getFeatureIndex(IS_BRACKET), DetectCodeComponent.isBracket(node.form) ? 1 : 0);
                /**
                 * Is this token a Camal case variable name?
                 */

                // features.add(new FeatureNode(getFeatureIndex(IS_VARIABLE), DetectCodeComponent.isVariable(node.form) ? 1 : 0));
                codeDetect.add(getFeatureIndex(IS_VARIABLE), DetectCodeComponent.isVariable(node.form) ? 1 : 0);

                /**
                 * Is this token a comment "//"?
                 */

                // features.add(new FeatureNode(getFeatureIndex(IS_COMMENT), DetectCodeComponent.isComment(node.form) ? 1 : 0));
                codeDetect.add(getFeatureIndex(IS_COMMENT), DetectCodeComponent.isComment(node.form) ? 1 : 0);

                /**
                 * Is this token a programming operator?
                 */

                // features.add(new FeatureNode(getFeatureIndex(IS_OPERATOR), DetectCodeComponent.isOperator(node.form) ? 1 : 0));
                codeDetect.add(getFeatureIndex(IS_OPERATOR), DetectCodeComponent.isOperator(node.form) ? 1 : 0);

                /**
                 * Is this token a parenthesis?
                 */

                //  features.add(new FeatureNode(getFeatureIndex(IS_PARENTHESIS), DetectCodeComponent.isParenthesis(node.form) ? 1 : 0));
                codeDetect.add(getFeatureIndex(IS_PARENTHESIS), DetectCodeComponent.isParenthesis(node.form) ? 1 : 0);

                /**
                 * Is this token a parenthesis?
                 */

                //     features.add(new FeatureNode(getFeatureIndex(IS_SEMICOLON), DetectCodeComponent.isSemicolon(node.form) ? 1 : 0));
                codeDetect.add(getFeatureIndex(IS_SEMICOLON), DetectCodeComponent.isSemicolon(node.form) ? 1 : 0);

                // keyword contain
                codeDetect.add(getFeatureIndex(KEYWORD_CONTAIN), DetectCodeComponent.isThisKeyword(node.form) ? 1 : 0);
            }
        }

        for(Integer id : dependentRelationBag.elementSet()) {
            if(!featureNodeIndex.contains(id)) {
                featureNodeIndex.add(id);
                features.add(new FeatureNode(id, (double)dependentRelationBag.count(id)/(double)size));
            }

        }
        for(Integer id : posTagBag.elementSet()) {
            if(!featureNodeIndex.contains(id)) {
                featureNodeIndex.add(id);
                features.add(new FeatureNode(id, posTagBag.count(id)));
            }

        }

        if(param.getTagType() == TagConstant.codeTag) {
            System.out.println(param.getCurrentLine());
        }
        for(Integer id : codeDetect.elementSet()) {
            if (!featureNodeIndex.contains(id)) {
                featureNodeIndex.add(id);
                features.add(new FeatureNode(id, codeDetect.count(id)));

     //           if(param.getTagType() == TagConstant.codeTag) {
     //               System.out.print(getFeatureWord(id) + ": " + codeDetect.count(id) + " ");
      //          }
            }
        }

        for(Integer id : grams.elementSet()) {
            if(!featureNodeIndex.contains(id)) {
                featureNodeIndex.add(id);
                features.add(new FeatureNode(id, grams.count(id)));
            }


        }

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
        LinkedList<Feature> features = extractFeatures(param);

        // determine whether the given token is the begin / middle / end of the component
        if (component != null) {
            answers[featureIdx] = component.intermediate;
        } else {
            answers[featureIdx] = TagConstant.TEXT;
        }
   //     System.out.println(param.getTagType() + ": " + answers[featureIdx]);
        originalLines[featureIdx] = param.getCurrentLine();

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




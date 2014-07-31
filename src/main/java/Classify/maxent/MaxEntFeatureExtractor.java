package Classify.maxent;

/**
 * Created by mhjang on 7/9/14.
 */

import Classify.FiveFoldDataSplit;
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
import simple.io.myungha.DirectoryReader;
import simple.io.myungha.SimpleFileReader;
import simple.io.myungha.SimpleFileWriter;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Classify.liblinear.FeatureExtractor;

/**
 * Created by mhjang on 4/27/14.
 */


public class MaxEntFeatureExtractor {

    // 7/31/14: change this implementation to use FeatureExtractor to extract features
    // composition design

    FeatureExtractor fe = new FeatureExtractor();
    static String initiatedTag = null;
    static String[] tags = {TagConstant.codeTag, TagConstant.tableTag, TagConstant.equTag, TagConstant.miscTag};

    static HashMap<String, Integer> featureMap = new HashMap<String, Integer>();
    static HashMap<Integer, String> featureinverseMap = new HashMap<Integer, String>();


    static Pattern numberPattern = Pattern.compile("^[a-zA-Z]*([0-9]+).*");
    static Pattern puctPattern = Pattern.compile("\\p{Punct}");


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

    static private boolean textualFeatureOn = true;
    static private boolean ngramFeatureOn = true;

    static private boolean equationBaselineOn = true;
    static private boolean codeBaselineFeatureOn = true;
    static private boolean tableBaselineOn = true;

    static private boolean posFeatureOn = true;
    static private boolean dependencyFeatureOn = true;

    static private boolean structuralDependencyOn = false;
    static private boolean structuralFeatureOn = true;

    static int maxDependents = 5;

    LinkedList<LinkedList<String>> featureList;

    static class Component {
        public int begin, intermediate, end;
        public Component(int begin, int intermidiate, int end) {
            this.begin = begin;
            this.intermediate = intermidiate;
            this.end = end;
        }
    }

    Component table = new Component(TagConstant.BEGINTABLE, TagConstant.INTTABLE, TagConstant.ENDTABLE);
    Component code = new Component(TagConstant.BEGINCODE, TagConstant.INTCODE, TagConstant.ENDCODE);
    Component equation = new Component(TagConstant.BEGINEQU, TagConstant.INTEQU, TagConstant.ENDEQU);
    Component misc = new Component(TagConstant.BEGINMISC, TagConstant.INTMISC, TagConstant.ENDMISC);



    int featureIdx = 0;

    static int featureNodeNum = 3;


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
    int[] answers = new int[1000000];

    static public void main(String[] args) throws IOException {

        MaxEntFeatureExtractor ef = new MaxEntFeatureExtractor();

        String baseDir = "/Users/mhjang/Desktop/clearnlp/allslides/";
        String allAnnotationDir = baseDir + "annotation/";
        String allParsingDir = baseDir + "parsed/";

        DirectoryReader dr = new DirectoryReader(allAnnotationDir);
//        ef.convertToMaxentFormat(ef.getContext(), ef.generateClassifyFeatures(baseDir, dr.getFileNameList()));
        Map.Entry<LinkedList<LinkedList<String>>, LinkedList<LinkedList<String>>> fivefolds = FiveFoldDataSplit.getFiveFoldSet(allAnnotationDir, true);
        LinkedList<LinkedList<String>> trainingSet = fivefolds.getKey();
        LinkedList<LinkedList<String>> testSet = fivefolds.getValue();
        for(int i=0; i<5; i++) {
            System.out.println("training");
            ef.convertToMaxentFormat(ef.getContext(), ef.generateClassifyFeatures(baseDir, trainingSet.get(i)), true, "training_" + i + ".txt");
            System.out.println("test");
            ef.convertToMaxentFormat(ef.getContext(), ef.generateClassifyFeatures(baseDir, testSet.get(i)), false, "test_" + i + ".txt");
            System.out.println("Training/Test set run " + (i+1));
        }
    }

    public LinkedList<LinkedList<String>> getFeatures() {
        return featureList;
    }


    public void convertToMaxentFormat(LinkedList<String> context, LinkedList<LinkedList<String>> features, boolean isTraining, String filename) throws IOException {
        SimpleFileWriter sw = new SimpleFileWriter(filename);
        SimpleFileWriter sw2 = null;
        if(!isTraining)
            sw2 = new SimpleFileWriter(filename.substring(0,filename.lastIndexOf('.')) + "_answers.txt");

        for(LinkedList<String> flist : features) {
            int i=0;
            for(i=0; i<flist.size()-1; i++) {
                sw.write(context.get(i) + ":" + flist.get(i) +" ");
   //             System.out.print(context.get(i) + ":" + flist.get(i) + " ");
            }
            if(isTraining) sw.write(TagConstant.getTagLabelByComponent(Integer.parseInt(flist.get(i))));
            else {
                sw.write("?");
                sw2.writeLine(TagConstant.getTagLabelByComponent(Integer.parseInt(flist.get(i))));
            }
            sw.write("\n");
  //          System.out.println();
        }
        sw.close();
        if(!isTraining) sw2.close();
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

    private String findMatchingEndTag(String beginTag) {
        if(beginTag == TagConstant.tableTag) return TagConstant.tableCloseTag;
        else if(beginTag == TagConstant.codeTag) return TagConstant.codeCloseTag;
        else if(beginTag == TagConstant.equTag) return TagConstant.equCloseTag;
        else return TagConstant.miscCloseTag;
    }


    private void generateCodeRemovedDocuments() throws IOException {
        String baseDir = "/Users/mhjang/Documents/teaching_documents/extracted/stemmed/parsed/gold/feature_tokens/";
        String newDir = "/Users/mhjang/Documents/teaching_documents/extracted/stemmed/parsed/gold/code_removed/";

        DirectoryReader dr = new DirectoryReader(baseDir);
        for(String filename: dr.getFileNameList()) {
            System.out.println(filename);
            SimpleFileReader sr = new SimpleFileReader(baseDir + filename);
            SimpleFileWriter sw = new SimpleFileWriter(newDir + filename);

            while(sr.hasMoreLines()) {
                String line = sr.readLine();
                if(!DetectCodeComponent.isCodeLine(line))
                    sw.writeLine(line);
            }
            sw.close();
        }
    }

    /**
     * Extract features for applying the model
     * @param
     * @return
     * @throws java.io.IOException

    public void prepareFeatureForDocuments() throws IOException {
        //     read all annotated files from the directory
        //     String directory = "/Users/mhjang/Desktop/clearnlp/trainingdata/annotation/";
        // String parsedDir = "/Users/mhjang/Documents/teaching_documents/extracted/stemmed/parsed/";
        String baseDir = "/Users/mhjang/Documents/teaching_documents/extracted/stemmed/parsed/gold/";
        String parsedDir = baseDir + "parsed/";
        model = Model.load(modelFile);
        String noiseRemovedline;
        ArrayList<Double> ratios = new ArrayList<Double>();
        componentCount = new int[5];
        DirectoryReader dr = new DirectoryReader(parsedDir);
        try {
            for (String filename : dr.getFileNameList()) {
                removedNode = 0;
                remainingNode = 0;
                DEPReader reader = new DEPReader(0, 1, 2, 3, 4, 5, 6);

                if (filename.contains(".DS_Store")) continue;
                // open a corresponding parsed file
                reader.open(UTInput.createBufferedFileReader(parsedDir + filename));
                DEPTree tree;
                ArrayList<DEPTree> treelist = new ArrayList<DEPTree>();
                while ((tree = reader.next()) != null) {
                    treelist.add(tree);

                }
                // then open an annotation file
                int treeIdx = 0;
                String line;
                String filename_ = filename.replace(".cnlp","");
                String featureTokenDir = baseDir + "feature_tokens/";
                String outputDir = baseDir + "noise_removed/";
                SimpleFileReader freader = new SimpleFileReader(featureTokenDir + filename_);
                SimpleFileWriter fwriter = new SimpleFileWriter(outputDir + filename_);

                System.out.println(filename);

                boolean isTagBeginLine = false, tagClosed = false;
                String endTag = null;
                while(freader.hasMoreLines()) {
                    line = freader.readLine();
                    boolean treeIdxSkip = false;
                    line = line.trim();

                    if (!line.isEmpty()) {
                        LinkedList<String> tokens = new LinkedList<String>();
                        int startIdx = 0;
                        noiseRemovedline = classifyAndRemove(treelist.get(treeIdx), DetectCodeComponent.isCodeLine(line), DetectEquation.isEquation(line), DetectTable.isTable(line));
                        treeIdx++;
                        fwriter.writeLine(noiseRemovedline);
                    }

                }
                fwriter.close();
                double ratio = (double)removedNode / (double)(removedNode + remainingNode);
                System.out.println("Noise Ratio: " + ratio);
                ratios.add(ratio);
                // print the number of components predicted
                int sum = 0;
                for(int i=0; i<5; i++) {
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
        for(Double d : ratios) {
            ratioSum += d;
        }
        System.out.println("Average removed noise ratio: " + ratioSum / (double)ratios.size());
    }
*/



    /**
     * Extract features for building the model
     * @param data
     * @return
     * @throws java.io.IOException
     */
    public LinkedList<LinkedList<String>> generateClassifyFeatures(String baseDir, LinkedList<String> data) throws IOException {
        //     read all annotated files from the directory
        //     String directory = "/Users/mhjang/Desktop/clearnlp/trainingdata/annotation/";
        // baseDir = "/Users/mhjang/Desktop/clearnlp/allslides/";
        // initialize the global variable featurelist
        featureList = new LinkedList<LinkedList<String>>();

        String parsedDir = baseDir + "parsed/";
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
                // then open an annotation file
                int treeIdx = 0;
                String line;
                SimpleFileReader freader = new SimpleFileReader(baseDir + "annotation/" + filename);
                System.out.println("loading " + filename);

                boolean isTagBeginLine = false, tagClosed = false;
                String endTag = null;

                //initialize component count

                componentCount = new int[5];
                for(int i=0; i<5; i++) {
                    componentCount[i] = 0;
                }
                int numOfTokensInDoc = 0;



                while(freader.hasMoreLines()) {
                    line = freader.readLine();
                    /**
                     * beginToken: begin of the component over the lines. It is set only if the begin tag is present in the current line; Otherwise set to -1
                     endToken: end of the component over the lines. It is set only if the end tag is present in the current line; Otherwise set to -1
                     componentBegin: begin of the component in this line.
                     componentEnd: begin of the component in this line.
                     */
                    int beginToken = -1, endToken = -1;
                    int componentBegin= -1, componentEnd = -1;
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
                                endTag = findMatchingEndTag(initiatedTag);
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
                            if(line.contains(endTag)) {
                                if (line.replace(endTag, "").trim().length() == 0) treeIdxSkip = true;
                                endIdx = line.indexOf(endTag);
                                tagClosed = true;
                            }
                            else {
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
                        //      System.out.println(treeIdx + ":" + line);
                        //      printTree(treeIdx, treelist.get(treeIdx));
                        extractFeatureFromTree(componentBegin, componentEnd, treelist.get(treeIdx), initiatedTag, beginToken, endToken, DetectCodeComponent.isCodeLine(line), DetectEquation.isEquation(line), DetectTable.isTable(line));
                        numOfTokensInDoc += treelist.get(treeIdx).size() - 1;
                        if (tagClosed) {
                            initiatedTag = null;
                            tagClosed = false;
                        }

                        treeIdx++;
                    }

                }
 /*               for(int i=0; i<5; i++) {
                    System.out.print(((double)componentCount[i] / (double)numOfTokensInDoc) + "\t");
                }
                System.out.println(numOfTokensInDoc);
                //     System.out.println();
 */
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return featureList;
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
     * 6/1/2014
     * extract features for each token in a parsing tree
     * @param tree
     * @param componentFragBegin, componentFragEnd: a fragment idx that should be at least tagged as <COMPONENT-I> in this line
     * @param beginTokenIdx: begin of the component, not the line. If this line doesn't contain the begin token, it is set to -1
     *        endTokenIdx: end of the component, not the line.
     *                            * @return
     */
    private void extractFeatureFromTree(int componentFragBegin, int componentFragEnd, DEPTree tree, String tagType, int beginTokenIdx, int endTokenIdx, boolean isThisLineCode, boolean isThisLineEquation, boolean isThisLineTable) {
        int i, size = tree.size(), npSum = 0;
        tree.resetDependents();
        DEPNode node;
        int noiseCount = 0;

        /** select the component tag **/
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
        String[] tokens = new String[size - 1];
        for (i = 1; i < size; i++) {
            node = tree.get(i);
            List<DEPArc> dependents = node.getDependents();
            if(dependents.size() > maxDependents)
                dependents = dependents.subList(0, maxDependents);

            // check feature index duplicate
            HashSet<Integer> featureIndex = new HashSet<Integer>();
            tokens[i - 1] = node.form;
            LinkedList<String> features = new LinkedList<String>();
            /***********************************************
             *                 Textual Features            *
             ***********************************************/
            if (textualFeatureOn) {
                /***
                 * Feature 1: Does this token contain numbers?
                 */

                Matcher m1 = numberPattern.matcher(node.form);
                if (m1.find())
                    features.add("True");
                else
                    features.add("False");

                /**
                 * Feature 2: Is this token a punctuation?
                 */

                Matcher m2 = puctPattern.matcher(node.form);
                // whether token is a punctuation
                if (m2.find())
                    features.add("True");
                else
                    features.add("False");

                /**
                 * Feature 3: the length of the token
                 */
                int charlen = node.form.length();
                features.add(String.valueOf(charlen));
            }

            if (ngramFeatureOn) {

                /**
                 * Feature 4: 1-gram of the token
                 */
                features.add(node.form);
                /**
                 * bi-gram - previous token
                 */
                features.add("P_" + tree.get(i - 1).form);

                /**
                 * bi-gram - next token
                 */
                if (i != size - 1)
                    features.add("N_" + tree.get(i + 1).form);

                else
                    features.add("N_END");

                /**
                 * bi-gram - bigram token (prev + current)
                 */
                features.add(tree.get(i - 1).form + " " + node.form);

                /**
                 * bi-gram - bigram token (prev + current)
                 */
                if (i != size - 1)
                    features.add(node.form + " " + tree.get(i + 1).form);
                else
                    features.add(node.form + " " + "N_END");

            }

            /****************************
             * Code component baseline
             ****************************/
            if (codeBaselineFeatureOn) {
                /**
                 * Is this token a bracket?
                 */

                features.add(DetectCodeComponent.isBracket(node.form) ? "True" : "False");

                /**
                 * Is this token a Camal case variable name?
                 */

                features.add(DetectCodeComponent.isVariable(node.form) ? "True" : "False");

                /**
                 * Is this token a comment "//"?
                 */

                features.add(DetectCodeComponent.isComment(node.form) ? "True" : "False");

                /**
                 * Is this token a programming operator?
                 */

                features.add(DetectCodeComponent.isOperator(node.form) ? "True" : "False");

                /**
                 * Is this token a parenthesis?
                 */

                features.add(DetectCodeComponent.isParenthesis(node.form) ? "True" : "False");

                /**
                 * Is this token a parenthesis?
                 */

                features.add(DetectCodeComponent.isSemicolon(node.form) ? "True" : "False");

                /**
                 * Is this line of the token a code line?
                 */
                features.add(isThisLineCode ? "True" : "False");
            }

            /****************************
             * Equation component baseline
             ****************************/
            if (equationBaselineOn)
                features.add(isThisLineEquation ? "True" : "False");


            /****************************
             * Table component baseline
             ****************************/
            if (tableBaselineOn)
                features.add(isThisLineTable ?"True" : "False");


            /**************************************************
             *      Parsing features (1) POS TAG Features     *
             * ************************************************/

            /**
             * POS tag of the token
             */
            if (posFeatureOn) {
                features.add(node.pos);

                /**
                 * POS tags of the token's dependents
                 */
                for (DEPArc arc : dependents) {
                    features.add("D_" + arc.getNode().pos);
                }
                int leftover = maxDependents - dependents.size();
                for(int k=0; k<leftover; k++) {
                    features.add("D_null");
                }

                /**
                 * POS tags of the token's heads
                 */
                if (node.hasHead()) {
                    features.add("H_" + node.getHead().pos);
                }
                else
                    features.add("H_null");

                /**
                 * Prev word (i-1) + POS tag of the current word
                 */

                features.add(tree.get(i - 1).form + " " + node.pos);

                /**
                 * POS tag of the prev word + the current word
                 */
                features.add(tree.get(i - 1).pos + " " + node.form);

                /**
                 * the current word + POS tag of the next word
                 */

                if (i != size - 1) {
                    features.add(node.form + " " + tree.get(i + 1).pos);
                }
                else {
                    features.add(node.form + " END");
                }

                /**
                 * the current pos + the next word
                 */

                if (i != size - 1) {
                    features.add(node.pos + " " + tree.get(i + 1).form);
                }
                else {
                    features.add(node.pos + " END");
                }
            }

            /**************************************************
             *      Parsing features (2) RELATION Features     *
             * ************************************************/
            if (dependencyFeatureOn) {
                for (DEPArc arc : dependents) {
                    features.add(arc.getNode().getLabel());
                }
                int leftover = maxDependents - dependents.size();
                for(int k=0; k<leftover; k++) {
                    features.add("null");
                }
                features.add(String.valueOf(dependents.size()));
           }
            /**************************************************
             *      Structural Features                       *
             **************************************************/
            if (structuralFeatureOn) {
                if (featureIdx > 0)
                    features.add(String.valueOf(answers[featureIdx-1]));
                else
                    features.add("-1");
            }

            /*System.out.println(node.form);
            for(Feature f : features) {
                System.out.println(f.getIndex()+ ": " + f.getValue());
            }*/


            if (component != null) {
                if (beginTokenIdx == i - 1) answers[featureIdx] = component.begin;
                else if (beginTokenIdx + 1 == i - 1 && componentFragEnd > i - 1)
                    answers[featureIdx] = component.intermediate;
                else if (endTokenIdx - 1 == i - 1) answers[featureIdx] = component.end;
                else if (componentFragBegin <= i - 1 && componentFragEnd > i - 1)
                    answers[featureIdx] = component.intermediate;
                else
                    answers[featureIdx] = TagConstant.TEXT;
            } else {
                answers[featureIdx] = TagConstant.TEXT;
            }
            componentCount[toIndex(TagConstant.getTagLabelByComponent((int)answers[featureIdx]))]++;

            features.add(String.valueOf(answers[featureIdx]));
            featureList.add(features);
            featureIdx++;
        }
    }

    /**
     * For MaxEnt algorithm; returns a list of context names
     * @return
     */
    private LinkedList<String> getContext() {
        LinkedList<String> context = new LinkedList<String>();
        if (textualFeatureOn) {
            context.add("Numberpattern");
            context.add("Punctuation");
            context.add("Length");

        }
        if (ngramFeatureOn) {
            context.add("Current");
            context.add("Previous");
            context.add("Next");
            context.add("PrevBigram");
            context.add("NextBigram");
        }

        /****************************
         * Code component baseline
         ****************************/
        if (codeBaselineFeatureOn) {
            context.add("Code_bracket");
            context.add("Code_camal");
            context.add("Code_comment");
            context.add("Code_operator");
            context.add("Code_parenthesis");
            context.add("Code_semicolon");
            context.add("Code_codeline");
        }


        /****************************
         * Equation component baseline
         ****************************/
        if (equationBaselineOn)
            context.add("Equation_baseline");

        /****************************
         * Table component baseline
         ****************************/
        if (tableBaselineOn)
            context.add("Table_baseline");


        /**************************************************
         *      Parsing features (1) POS TAG Features     *
         * ************************************************/

        /**
         * POS tag of the token
         */
        if (posFeatureOn) {
            context.add("POStag");

            /**
             * POS tags of the token's dependents
             */
            for(int i=1; i<=maxDependents; i++) {
                context.add("DepPOS_"+i);
            }

            /**
             * POS tags of the token's heads
             */
            context.add("Head");

            /**
             * Prev word (i-1) + POS tag of the current word
             */
            context.add("Prev_POS");

            /**
             * POS tag of the prev word + the current word
             */
            context.add("Prev_Current");

            /**
             * the current word + POS tag of the next word
             */
            context.add("Current_POS");

            /**
             * the current pos + the next word
             */
            context.add("Current_Next");
        }

        /**************************************************
         *      Parsing features (2) RELATION Features     *
         * ************************************************/
        if (dependencyFeatureOn) {
            for (int i = 1; i <= maxDependents; i++) {
                context.add("DepRel_" + i);
            }
            context.add("Dep_size");
        }

        /**************************************************
         *      Structural Features                       *
         **************************************************/
        if (structuralFeatureOn) {
                context.add("Previous_Class");
        }
        return context; 

    }

    private int toIndex(String tagName) {
        if(tagName.equals(TagConstant.tableTag)) return 0;
        if(tagName.equals(TagConstant.codeTag)) return 1;
        if(tagName.equals(TagConstant.equTag)) return 2;
        if(tagName.equals(TagConstant.miscTag)) return 3;
        if(tagName.equals(TagConstant.textTag)) return 4;
        return -1;
    }



}





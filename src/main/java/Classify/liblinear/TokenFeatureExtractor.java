package Classify.liblinear;

import Classify.StringTokenizerIndx;
import Classify.TagConstant;
import Classify.liblinear.datastructure.NonTextualComponent;
import Classify.liblinear.datastructure.FeatureParameter;
import Classify.liblinear.datastructure.FragmentIndex;
import Classify.noisegenerator.TableGenerator;
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


public class TokenFeatureExtractor extends BasicFeatureExtractor {

    File modelFile = new File("slide_model_0608");
    Model model;
    int removedNode = 0, remainingNode = 0;

    int[] componentCount;

    static Pattern numberPattern = Pattern.compile("^[a-zA-Z]*([0-9]+).*");
    static Pattern puctPattern = Pattern.compile("\\p{Punct}");


    static FeatureNode feature1True = new FeatureNode(1, 1);
    static FeatureNode feature1False = new FeatureNode(1, 0);

    static FeatureNode feature2True = new FeatureNode(2, 1);
    static FeatureNode feature2False = new FeatureNode(2, 0);

    DetectTable tableDetecter;

    public TokenFeatureExtractor(boolean isTrainingMode) {
        this.isLearningMode = isTrainingMode;
        tableDetecter = new DetectTable();
    }

    public TableGenerator getTableGenerator() {
        /// yet to be implemented
        return null;
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
                        FeatureParameter param = new FeatureParameter.Builder(treelist.get(treeIdx), DetectCodeComponent.codeLineEvidence(line), DetectCodeComponent.keywordContainSize(line),DetectEquation.isEquation(line), tableDetecter.isTable(line), true).build();
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



    /**
     * Extract features for building the model
     * @override
     * @param data
     * @param applyModel: whether you use a trained model to fill out "previous label" feature
     * @return
     * @throws java.io.IOException
     */
    public LinkedList<Feature[]> run(String baseDir, ArrayList<String> data, boolean applyModel)  {
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
                            for (String tag : startTags) {
                                if (line.contains(tag)) {
                                    initiatedTag = tag;
                                    endTag = TagConstant.findMatchingEndTag(initiatedTag);
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
                        param = new FeatureParameter.Builder(treelist.get(treeIdx), DetectCodeComponent.codeLineEvidence(line), keywordContain, DetectEquation.isEquation(line), tableDetecter.isTable(line), applyModel).componentFrag(new FragmentIndex(componentBegin, componentEnd)).tagType(initiatedTag).tokenLocation(new FragmentIndex(beginToken, endToken)).build();

                        addFeature(param);
                        numOfTokensInDoc += treelist.get(treeIdx).size() - 1;
                        if (tagClosed) {
                            initiatedTag = null;
                            tagClosed = false;
                        }

                        treeIdx++;
                    }

                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return allFeatures;
    }


    protected void addFeature(FeatureParameter param)  {
        NonTextualComponent component = NonTextualComponent.getComponent(param.getTagType());
        DEPTree tree = param.getParsingTree();
        int i, size = tree.size();
        tree.resetDependents();
        for (i = 1; i < size; i++) {
            try {
                param.setCurrentIndex(i);
            } catch (Exception e) {
                e.printStackTrace();
            }
            LinkedList<Feature> features = extractFeatures(param);
            // determine whether the given token is the begin / middle / end of the component
            if (component != null) {
                if (param.getTokenFrag().getStart() == i - 1) answers.add((double)component.begin);
                else if (param.getTokenFrag().getStart() + 1 == i - 1 && param.getComponentFrag().getEnd() > i - 1)
                    answers.add((double)component.intermediate);
                else if (param.getTokenFrag().getEnd() - 1 == i - 1) answers.add((double)component.end);
                else if (param.getComponentFrag().getStart() <= i - 1 && param.getComponentFrag().getEnd() > i - 1)
                    answers.add((double)component.intermediate);
                else
                    answers.add((double)TagConstant.TEXT);

            } else {
                answers.add((double)TagConstant.TEXT);
            }

            originalTextLines.add(tree.get(param.getCurrentIndex()).form);
            Collections.sort(features, fc);
            Feature[] featureArray;
            featureArray = features.toArray(new Feature[features.size()]);

            // save current prediction to use as a feature for the next label
            if (param.isApplyModel()) {
                previousNodePrediction = (int)Linear.predict(FeatureParameter.firstModel, featureArray);
            }
            featureIdx++;
            allFeatures.add(featureArray);
    }
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
                else features.add(new FeatureNode(getFeatureIndex(PREVIOUS_LABEL), answers.get(featureIdx - 1)));
            else
                features.add(new FeatureNode(getFeatureIndex(PREVIOUS_LABEL), 0));

        }
        return features;
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




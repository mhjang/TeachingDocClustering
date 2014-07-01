package Classify;

import com.clearnlp.dependency.DEPArc;
import com.clearnlp.dependency.DEPNode;
import com.clearnlp.dependency.DEPTree;
import com.clearnlp.reader.DEPReader;
import com.clearnlp.util.UTInput;
import com.drew.metadata.Directory;
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


public class ExtractFeature {



    static String initiatedTag = null;
    static String[] tags = {TagConstant.codeTag, TagConstant.tableTag, TagConstant.equTag, TagConstant.miscTag};
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

    Component table = new Component(TagConstant.BEGINTABLE, TagConstant.INTTABLE, TagConstant.ENDTABLE);
    Component code = new Component(TagConstant.BEGINCODE, TagConstant.INTCODE, TagConstant.ENDCODE);
    Component equation = new Component(TagConstant.BEGINEQU, TagConstant.INTEQU, TagConstant.ENDEQU);
    Component misc = new Component(TagConstant.BEGINMISC, TagConstant.INTMISC, TagConstant.ENDMISC);

    LinkedList<Feature[]> allFeatures = new LinkedList<Feature[]>();

    double[] answers = new double[1000000];
    double[] predictedAnswers = new double[1000000];

    int featureIdx = 0;
    int averageNoiseTokenLength = 0, averageNotNoiseTokenLength = 0;

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

    static public void main(String[] args) throws IOException {

        ExtractFeature ef = new ExtractFeature();
//       apply the classifer to the other documents
        ef.generateCodeRemovedDocuments();
//        ef.prepareFeatureForDocuments();

/*        String baseDir = "/Users/mhjang/Desktop/clearnlp/allslides/";
        String allAnnotationDir = baseDir + "annotation/";
        String allParsingDir = baseDir + "parsed/";

        DirectoryReader dr = new DirectoryReader(allAnnotationDir);


        LinkedList<Feature[]> allFeatures = ef.generateClassifyFeatures(baseDir, dr.getFileNameList());
        // Generate a problem with trainng set features
        Feature[][] allFeaturesArray = new Feature[allFeatures.size()][];

        for (int i = 0; i < allFeatures.size(); i++) {
            allFeaturesArray[i] = allFeatures.get(i);
        }

        Problem problem = new Problem();
        problem.x = allFeaturesArray;
        problem.n = featureNodeNum - 1;
        problem.y = Arrays.copyOfRange(ef.answers, 0, allFeatures.size());
        problem.l = allFeatures.size();


        SolverType solver = SolverType.L2R_L2LOSS_SVC; // -s 0
        double C = 1.0;    // cost of constraints violation
        double eps = 0.01; // stopping criteria
        int correctItem = 0;

        Parameter param = new Parameter(SolverType.L2R_L2LOSS_SVC, 10, 0.01);
  //      Model model = Linear.train(problem, param);
        File modelFile = new File("slide_model_0608");
  //      model.save(modelFile);
        Model model = Model.load(modelFile);


        int[] componentcounts= new int[5];
        for(int i=0; i<5; i++) {
            componentcounts[i] = 0;
        }

        for(int i=0; i<allFeaturesArray.length; i++) {
            Feature[] instance = allFeaturesArray[i];
            int prediction = (int)Linear.predict(model, instance);
            if(problem.y[i] == prediction) correctItem++;
            componentcounts[prediction]++;
        }

        System.out.println("Accuracy:" + (double)correctItem/(double)problem.y.length);
        for(int i=0; i<5; i++) {
            System.out.println(i + ":" + componentcounts[i]);
        }
/*
        int nr_fold = 5;
        double[] target = new double[problem.l];
    //    Linear.crossValidation(problem, param, nr_fold, target);
        int correctByComponent = 0;

        HashMap<String, Integer> componentAccuracy = new HashMap<String, Integer>();
        HashMap<Integer, Integer> classLabelCount = new HashMap<Integer, Integer>();

        // initialize
        componentAccuracy.put(TagConstant.tableTag, 0);
        componentAccuracy.put(TagConstant.codeTag, 0);
        componentAccuracy.put(TagConstant.equTag, 0);
        componentAccuracy.put(TagConstant.miscTag, 0);
        componentAccuracy.put(TagConstant.textTag, 0);

  /*      double[] target = ef.predictedAnswers;


        int codeComponentCorrect = 0, tableComponentCorrect = 0, equComponentCorrect = 0, miscComponentCorrect = 0;

        HashMap<Integer, Integer> classLabelCount = new HashMap<Integer, Integer>();
        */

/*
        int i=0;
        while(i<target.length) {
            System.out.println("predicted label: " + target[i] + " answer: " +  ef.answers[i]);
            int answerType = (int) ef.answers[i];
            int predictedType = (int) target[i];

            /*************************
             * Exact match computation
             *************************/
     /*       if (answerType == predictedType && predictedType == TagConstant.BEGINCODE) {
                while (true) {
                    if (answerType == predictedType) {
                        if (answerType == TagConstant.ENDCODE) {
                            codeComponentCorrect++;
                            break;
                        }
                    } else break;
                    i++;
                    if(i>= target.length-1) break;

                    answerType = (int) ef.answers[i];
                    predictedType = (int) target[i];

                }
            }

            if (answerType == predictedType && predictedType == TagConstant.BEGINTABLE) {
                while (true) {
                    if (answerType == predictedType) {
                        if (answerType == TagConstant.ENDTABLE) {
                            tableComponentCorrect++;
                            break;
                        }
                    } else break;
                    i++;
                    if(i>= target.length-1) break;

                    answerType = (int) ef.answers[i];
                    predictedType = (int) target[i];

                }
            }

            if (answerType == predictedType && predictedType == TagConstant.BEGINEQU) {
                while (true) {
                    if (answerType == predictedType) {
                        if (answerType == TagConstant.ENDEQU) {
                            equComponentCorrect++;
                            break;
                        }
                    } else break;
                    i++;
                    if(i>= target.length-1) break;
                    answerType = (int) ef.answers[i];
                    predictedType = (int) target[i];

                }
            }

            if (answerType == predictedType && predictedType == TagConstant.BEGINMISC) {
                while (true) {
                    if (answerType == predictedType) {
                        if (answerType == TagConstant.ENDMISC) {
                            miscComponentCorrect++;
                            break;
                        }
                    } else break;
                    i++;
                    if(i>= target.length-1) break;
                    answerType = (int) ef.answers[i];
                    predictedType = (int) target[i];
                }
            }
            i++;
        }
        */
/*            correctItem = 0;
            int predictedComponents = 0;
            for(int i=0; i<target.length; i++) {
                if ((int) ef.answers[i] == (int) target[i]) correctItem++;
                if((int) ef.answers[i] != (int)TagConstant.TEXT) {
                    predictedComponents++;
                }
                if (TagConstant.getTagLabelByComponent((int) ef.answers[i]).equals(TagConstant.getTagLabelByComponent((int) target[i]))) {
                    String component = TagConstant.getTagLabelByComponent((int) ef.answers[i]);
                    componentAccuracy.put(component, componentAccuracy.get(component) + 1);
                    correctByComponent++;
                }
                if (classLabelCount.containsKey((int) ef.answers[i])) {
                    int c = classLabelCount.get((int) ef.answers[i]);
                    classLabelCount.put((int) ef.answers[i], c + 1);
                } else
                    classLabelCount.put((int) ef.answers[i], 1);
            }

        int tableCount = classLabelCount.get(TagConstant.BEGINTABLE);
        int codeCount = classLabelCount.get(TagConstant.BEGINCODE);
        int equCount = classLabelCount.get(TagConstant.BEGINEQU);
        int miscCount = classLabelCount.get(TagConstant.BEGINMISC);


   /*     int tableCount = classLabelCount.get(TagConstant.BEGINTABLE) + classLabelCount.get(TagConstant.INTTABLE) + classLabelCount.get(TagConstant.ENDTABLE);
        int codeCount = classLabelCount.get(TagConstant.BEGINCODE) + classLabelCount.get(TagConstant.INTCODE) + classLabelCount.get(TagConstant.ENDCODE);
        int equCount = classLabelCount.get(TagConstant.BEGINEQU) + classLabelCount.get(TagConstant.INTEQU) + classLabelCount.get(TagConstant.ENDEQU);
        int miscCount = classLabelCount.get(TagConstant.BEGINMISC) + classLabelCount.get(TagConstant.INTMISC) + classLabelCount.get(TagConstant.ENDMISC);
*/
        /**************************
         * print out exp settings
         ***************************/
  /*      System.out.println("EXP SETTING");
        System.out.println("textual feature : " + textualFeatureOn);
        System.out.println("N-gram feature : " + ngramFeatureOn);
        System.out.println("equation feature : " + equationBaselineOn);
        System.out.println("table feature : " + tableBaselineOn);
        System.out.println("POS feature : " + posFeatureOn);
        System.out.println("dependency feature : " + dependencyFeatureOn);
        System.out.println("structural feature : " + structuralFeatureOn);
        System.out.println("structural depdency feature : " + structuralDependencyOn);

        System.out.println("Predicted components: " + predictedComponents);
        System.out.println("Total token: " + ef.answers.length);

        for(int i=0; i<13; i++) {
            System.out.println(TagConstant.getTagLabel(i) + ":" + classLabelCount.get(i));
        }

        System.out.println("Correct by component: "+ correctByComponent);
        System.out.println("Correct by class:" + correctItem);
        System.out.println(target.length);

        System.out.println("error : " + (double)correctItem / (double) target.length);
        System.out.println("error by component: " + (double)correctByComponent / (double) target.length);



        System.out.println("Text Accuracy: " + (double)componentAccuracy.get(TagConstant.textTag) / (double) classLabelCount.get(TagConstant.TEXT));
        System.out.println("Table Accuracy: " + (double)componentAccuracy.get(TagConstant.tableTag) / (double) tableCount);
        System.out.println("Code Accuracy: " + (double)componentAccuracy.get(TagConstant.codeTag) / (double) codeCount);
        System.out.println("Equation Accuracy: " + (double)componentAccuracy.get(TagConstant.equTag) / (double) equCount);
        System.out.println("Misc Accuracy: " + (double)componentAccuracy.get(TagConstant.miscTag) / (double) miscCount);

 /*       System.out.println("Exact Match");
        System.out.println("Table Accuracy: " + tableComponentCorrect/ (double) classLabelCount.get(TagConstant.BEGINTABLE));
        System.out.println("Code Accuracy: " + codeComponentCorrect / (double)classLabelCount.get(TagConstant.BEGINCODE));
        System.out.println("Equation Accuracy: " + equComponentCorrect / (double) classLabelCount.get(TagConstant.BEGINEQU));
        System.out.println("Misc Accuracy: " + miscComponentCorrect / (double) classLabelCount.get(TagConstant.BEGINMISC));
*/

    }




    public static LinkedList<LinkedList<String>> readCNLPFile(String parsedFile) {
        DEPReader reader = new DEPReader(0, 1, 2, 3, 4, 5, 6);
        //	PrintStream fout = UTOutput.createPrintBufferedFileStream(outputFile);
        LinkedList<LinkedList<String>> parsedSenList = new LinkedList<LinkedList<String>>();
        try {
            reader.open(UTInput.createBufferedFileReader(parsedFile));
            Set<String> set = new HashSet<String>();
            DEPTree tree;
            int idx = 0;
            ArrayList<DEPTree> treelist = new ArrayList<DEPTree>();
            while ((tree = reader.next()) != null) {
                treelist.add(tree);
            }
            for (DEPTree t : treelist) {
                LinkedList<String> sentence = new LinkedList<String>();
                for (int i = 0; i < t.size(); i++) {
                    DEPNode node = t.get(i);
                    sentence.add(node.form);
                }
                parsedSenList.add(sentence);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return parsedSenList;
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
     */
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




    /**
     * Extract features for building the model
     * @param data
     * @return
     * @throws java.io.IOException
     */
    public LinkedList<Feature[]> generateClassifyFeatures(String baseDir, ArrayList<String> data) throws IOException {
        //     read all annotated files from the directory
        //     String directory = "/Users/mhjang/Desktop/clearnlp/trainingdata/annotation/";
        // baseDir = "/Users/mhjang/Desktop/clearnlp/allslides/";
         String parsedDir = baseDir + "parsed/";
        model = Model.load(modelFile);
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
           //     System.out.println("loading " + filename);

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
                for(int i=0; i<5; i++) {
                    System.out.print(((double)componentCount[i] / (double)numOfTokensInDoc) + "\t");
                }
                System.out.println(numOfTokensInDoc);
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
        LinkedList<LinkedList<Feature>> featureList = new LinkedList<LinkedList<Feature>>();
        LinkedList<HashSet<Integer>> featureIndexList = new LinkedList<HashSet<Integer>>();
        int featureIndx;
        for (i = 1; i < size; i++) {
            node = tree.get(i);
            List<DEPArc> dependents = node.getDependents();

            // check feature index duplicate
            HashSet<Integer> featureIndex = new HashSet<Integer>();
            tokens[i - 1] = node.form;
            LinkedList<Feature> features = new LinkedList<Feature>();
            /***********************************************
             *                 Textual Features            *
             ***********************************************/
            if (textualFeatureOn) {
                /***
                 * Feature 1: Does this token contain numbers?
                 */

                Matcher m1 = numberPattern.matcher(node.form);
                if (m1.find())
                    features.add(feature1True);
                else
                    features.add(feature1False);

                /**
                 * Feature 2: Is this token a punctuation?
                 */

                Matcher m2 = puctPattern.matcher(node.form);
                // whether token is a punctuation
                if (m2.find())
                    features.add(feature2True);
                else
                    features.add(feature2False);

                /**
                 * Feature 3: the length of the token
                 */
                int charlen = node.form.length();
                features.add(new FeatureNode(getFeatureIndex(CHAR_LEN), charlen));
            }

            if (ngramFeatureOn) {

                /**
                 * Feature 4: 1-gram of the token
                 */
                featureIndx = getFeatureIndex(node.form);
                if (!featureIndex.contains(featureIndx)) {
                    features.add(new FeatureNode(getFeatureIndex(node.form), 1));
                    featureIndex.add(featureIndx);
                }
                /**
                 * bi-gram - previous token
                 */
                featureIndx = getFeatureIndex("P_" + tree.get(i - 1).form);
                if (!featureIndex.contains(featureIndx)) {
                    features.add(new FeatureNode(featureIndx, 1));
                    featureIndex.add(featureIndx);
                }

                /**
                 * bi-gram - next token
                 */
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
                features.add(new FeatureNode(getFeatureIndex(IS_CODELINE), isThisLineCode ? 1 : 0));
            }

            /****************************
             * Equation component baseline
             ****************************/
            if (equationBaselineOn)
                features.add(new FeatureNode(getFeatureIndex(IS_EQUATION), isThisLineEquation ? 1 : 0));


            /****************************
             * Table component baseline
             ****************************/
            if (tableBaselineOn)
                features.add(new FeatureNode(getFeatureIndex(IS_TABLE), isThisLineTable ? 1 : 0));


            /**************************************************
             *      Parsing features (1) POS TAG Features     *
             * ************************************************/

            /**
             * POS tag of the token
             */
            if (posFeatureOn) {
                featureIndx = getFeatureIndex(node.pos);
                if (!featureIndex.contains(featureIndx)) {
                    features.add(new FeatureNode(featureIndx, 1));
                    featureIndex.add(featureIndx);
                }

                /**
                 * POS tags of the token's dependents
                 */
                for (DEPArc arc : dependents) {
                    featureIndx = getFeatureIndex("D_" + arc.getNode().pos);
                    if (!featureIndex.contains(featureIndx)) {
                        features.add(new FeatureNode(featureIndx, 1));
                        featureIndex.add(featureIndx);
                    }
                }

                /**
                 * POS tags of the token's heads
                 */
                if (node.hasHead()) {
                    features.add(new FeatureNode(getFeatureIndex("H_" + node.getHead().pos), 1));
                }

                /**
                 * Prev word (i-1) + POS tag of the current word
                 */

                featureIndx = getFeatureIndex(tree.get(i - 1).form + " " + node.pos);
                if (!featureIndex.contains(featureIndx)) {
                    features.add(new FeatureNode(featureIndx, 1));
                    featureIndex.add(featureIndx);
                }

                /**
                 * POS tag of the prev word + the current word
                 */

                featureIndx = getFeatureIndex(tree.get(i - 1).pos + " " + node.form);
                if (!featureIndex.contains(featureIndx)) {
                    features.add(new FeatureNode(featureIndx, 1));
                    featureIndex.add(featureIndx);
                }

                /**
                 * the current word + POS tag of the next word
                 */

                if (i != size - 1) {
                    featureIndx = getFeatureIndex(node.form + " " + tree.get(i + 1).pos);
                    if (!featureIndex.contains(featureIndx)) {
                        features.add(new FeatureNode(featureIndx, 1));
                        featureIndex.add(featureIndx);
                    }
                }

                /**
                 * the current pos + the next word
                 */

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
                    features.add(new FeatureNode(getFeatureIndex(PREVIOUS_LABEL), predictedAnswers[featureIdx - 1]));
                else
                    features.add(new FeatureNode(getFeatureIndex(PREVIOUS_LABEL), 0));
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


            featureList.add(features);
            featureIndexList.add(featureIndex);
            Collections.sort(features, fc);

            Feature[] featureArray;
            featureArray = features.toArray(new Feature[features.size()]);
            allFeatures.add(featureArray);

            predictedAnswers[featureIdx] = Linear.predict(model, featureArray);
            //       predictedAnswers[featureIdx] = 0;
            featureIdx++;

        }

        /****************************************
         * Structural features of the dependents
         **************************************/
        if (structuralDependencyOn) {

            for (i = 1; i < size; i++) {

                node = tree.get(i);
                LinkedList<Feature> features = featureList.get(i - 1);

                List<DEPArc> dependents = node.getDependents();
                HashSet<Integer> featureIndex = featureIndexList.get(i - 1);
                for (DEPArc arc : dependents) {
                    int dependentIdx = featureIdx - (size - arc.getNode().id);
                    String label = "DEP_" + predictedAnswers[dependentIdx];
                    featureIndx = getFeatureIndex(label);
                    if (!featureIndex.contains(featureIndx)) {
                        for (int j = 0; j < features.size(); j++) {
                            FeatureNode fn = (FeatureNode) features.get(j);
                            if (fn.getIndex() > featureIndx) {
                                features.add(j, fn);
                                break;
                            }
                            if (j == features.size() - 1)
                                features.add(fn);
                        }
                        featureIndex.add(featureIndx);
                    }
                }

            //          Collections.sort(features, fc);
            Feature[] featureArray;
            featureArray = features.toArray(new Feature[features.size()]);

        }
    }
    }

    private int toIndex(String tagName) {
        if(tagName.equals(TagConstant.tableTag)) return 0;
        if(tagName.equals(TagConstant.codeTag)) return 1;
        if(tagName.equals(TagConstant.equTag)) return 2;
        if(tagName.equals(TagConstant.miscTag)) return 3;
        if(tagName.equals(TagConstant.textTag)) return 4;
        return -1;

    }

    /**
     * 6/1/2014
     * extract features for each token in a parsing tree
     * @param tree
     *        endTokenIdx: end of the component, not the line.
     *                            * @return
     */
    private String classifyAndRemove(DEPTree tree, boolean isThisLineCode, boolean isThisLineEquation, boolean isThisLineTable) {
        int i, size = tree.size(), npSum = 0;
        tree.resetDependents();
        DEPNode node;
        int noiseCount = 0;

        /** select the component tag **/
        String[] tokens = new String[size - 1];
        LinkedList<LinkedList<Feature>> featureList = new LinkedList<LinkedList<Feature>>();
        LinkedList<HashSet<Integer>> featureIndexList = new LinkedList<HashSet<Integer>>();
        StringBuilder builder = new StringBuilder();
        int featureIndx;
        for (i = 1; i < size; i++) {
            node = tree.get(i);
            List<DEPArc> dependents = node.getDependents();

            // check feature index duplicate
            HashSet<Integer> featureIndex = new HashSet<Integer>();
            tokens[i - 1] = node.form;
            LinkedList<Feature> features = new LinkedList<Feature>();
            /***********************************************
             *                 Textual Features            *
             ***********************************************/
            if (textualFeatureOn) {
                /***
                 * Feature 1: Does this token contain numbers?
                 */

                Matcher m1 = numberPattern.matcher(node.form);
                if (m1.find())
                    features.add(feature1True);
                else
                    features.add(feature1False);

                /**
                 * Feature 2: Is this token a punctuation?
                 */

                Matcher m2 = puctPattern.matcher(node.form);
                // whether token is a punctuation
                if (m2.find())
                    features.add(feature2True);
                else
                    features.add(feature2False);

                /**
                 * Feature 3: the length of the token
                 */
                int charlen = node.form.length();
                features.add(new FeatureNode(getFeatureIndex(CHAR_LEN), charlen));
            }

            if (ngramFeatureOn) {

                /**
                 * Feature 4: 1-gram of the token
                 */
                featureIndx = getFeatureIndex(node.form);
                if (!featureIndex.contains(featureIndx)) {
                    features.add(new FeatureNode(getFeatureIndex(node.form), 1));
                    featureIndex.add(featureIndx);
                }
                /**
                 * bi-gram - previous token
                 */
                featureIndx = getFeatureIndex("P_" + tree.get(i - 1).form);
                if (!featureIndex.contains(featureIndx)) {
                    features.add(new FeatureNode(featureIndx, 1));
                    featureIndex.add(featureIndx);
                }

                /**
                 * bi-gram - next token
                 */
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
                features.add(new FeatureNode(getFeatureIndex(IS_CODELINE), isThisLineCode ? 1 : 0));
            }

            /****************************
             * Equation component baseline
             ****************************/
            if (equationBaselineOn)
                features.add(new FeatureNode(getFeatureIndex(IS_EQUATION), isThisLineEquation ? 1 : 0));


            /****************************
             * Table component baseline
             ****************************/
            if (tableBaselineOn)
                features.add(new FeatureNode(getFeatureIndex(IS_TABLE), isThisLineTable ? 1 : 0));


            /**************************************************
             *      Parsing features (1) POS TAG Features     *
             * ************************************************/

            /**
             * POS tag of the token
             */
            if (posFeatureOn) {
                featureIndx = getFeatureIndex(node.pos);
                if (!featureIndex.contains(featureIndx)) {
                    features.add(new FeatureNode(featureIndx, 1));
                    featureIndex.add(featureIndx);
                }

                /**
                 * POS tags of the token's dependents
                 */
                for (DEPArc arc : dependents) {
                    featureIndx = getFeatureIndex("D_" + arc.getNode().pos);
                    if (!featureIndex.contains(featureIndx)) {
                        features.add(new FeatureNode(featureIndx, 1));
                        featureIndex.add(featureIndx);
                    }
                }

                /**
                 * POS tags of the token's heads
                 */
                if (node.hasHead()) {
                    features.add(new FeatureNode(getFeatureIndex("H_" + node.getHead().pos), 1));
                }

                /**
                 * Prev word (i-1) + POS tag of the current word
                 */

                featureIndx = getFeatureIndex(tree.get(i - 1).form + " " + node.pos);
                if (!featureIndex.contains(featureIndx)) {
                    features.add(new FeatureNode(featureIndx, 1));
                    featureIndex.add(featureIndx);
                }

                /**
                 * POS tag of the prev word + the current word
                 */

                featureIndx = getFeatureIndex(tree.get(i - 1).pos + " " + node.form);
                if (!featureIndex.contains(featureIndx)) {
                    features.add(new FeatureNode(featureIndx, 1));
                    featureIndex.add(featureIndx);
                }

                /**
                 * the current word + POS tag of the next word
                 */

                if (i != size - 1) {
                    featureIndx = getFeatureIndex(node.form + " " + tree.get(i + 1).pos);
                    if (!featureIndex.contains(featureIndx)) {
                        features.add(new FeatureNode(featureIndx, 1));
                        featureIndex.add(featureIndx);
                    }
                }

                /**
                 * the current pos + the next word
                 */

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
                    features.add(new FeatureNode(getFeatureIndex(PREVIOUS_LABEL), predictedAnswers[featureIdx - 1]));
                else
                    features.add(new FeatureNode(getFeatureIndex(PREVIOUS_LABEL), 0));
            }

            /*System.out.println(node.form);
            for(Feature f : features) {
                System.out.println(f.getIndex()+ ": " + f.getValue());
            }*/


            featureList.add(features);
            featureIndexList.add(featureIndex);
            Collections.sort(features, fc);

            Feature[] featureArray;
            featureArray = features.toArray(new Feature[features.size()]);
            allFeatures.add(featureArray);

            predictedAnswers[featureIdx] = Linear.predict(model, featureArray);
            if(TagConstant.getTagLabelByComponent((int)predictedAnswers[featureIdx]) == TagConstant.textTag)
            {
                builder.append(node.form + " ");
                remainingNode++;
            }
            else {
                removedNode++;
             //   System.out.println(node.form);
            }
            // count the number of components predicted
            componentCount[(int)predictedAnswers[featureIdx]]++;

            //       predictedAnswers[featureIdx] = 0;
            featureIdx++;

        }

        /****************************************
         * Structural features of the dependents
         **************************************/
        if (structuralDependencyOn) {

            for (i = 1; i < size; i++) {

                node = tree.get(i);
                LinkedList<Feature> features = featureList.get(i - 1);

                List<DEPArc> dependents = node.getDependents();
                HashSet<Integer> featureIndex = featureIndexList.get(i - 1);
                for (DEPArc arc : dependents) {
                    int dependentIdx = featureIdx - (size - arc.getNode().id);
                    String label = "DEP_" + predictedAnswers[dependentIdx];
                    featureIndx = getFeatureIndex(label);
                    if (!featureIndex.contains(featureIndx)) {
                        for (int j = 0; j < features.size(); j++) {
                            FeatureNode fn = (FeatureNode) features.get(j);
                            if (fn.getIndex() > featureIndx) {
                                features.add(j, fn);
                                break;
                            }
                            if (j == features.size() - 1)
                                features.add(fn);
                        }
                        featureIndex.add(featureIndx);
                    }
                }

                //          Collections.sort(features, fc);
                Feature[] featureArray;
                featureArray = features.toArray(new Feature[features.size()]);

            }
        }
        return builder.toString();
    }


    }




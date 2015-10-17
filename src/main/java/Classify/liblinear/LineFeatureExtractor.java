package Classify.liblinear;

import Classify.TagConstant;
import Classify.liblinear.datastructure.NonTextualComponent;
import Classify.liblinear.datastructure.FeatureParameter;
import Classify.liblinear.datastructure.WordVector;
import Classify.noisegenerator.TableGenerator;
import com.clearnlp.dependency.DEPArc;
import com.clearnlp.dependency.DEPNode;
import com.clearnlp.dependency.DEPTree;
import com.clearnlp.reader.DEPReader;
import com.clearnlp.util.UTInput;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import componentDetection.DetectCodeComponent;
import componentDetection.DetectTable;
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import simple.io.myungha.SimpleFileReader;
import simple.io.myungha.SimpleFileWriter;

import java.io.*;
import java.util.*;

/**
 * Created by mhjang on 8/25/14.
 */
public class LineFeatureExtractor extends BasicFeatureExtractor {
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
    double previousNodePrediction;
    double currentNodePrediction;
    boolean print = false;

    boolean tagClosed = false;
    DetectTable tableDetecter;
    TableGenerator tableGenerator;
    BufferedWriter bw;

    VectorLookUp vlu;

    SimpleFileWriter nnInput;

    int[] vectorNullCount;
    int[] vectorTokenCount;
    public LineFeatureExtractor(boolean isLearningMode) {
        this.isLearningMode = isLearningMode;
        tableDetecter = new DetectTable();
    //    tableGenerator = new TableGenerator();
        vlu = new VectorLookUp();
        vectorNullCount = new int[5];
        vectorTokenCount = new int[5];
        try {
            bw = new BufferedWriter(new FileWriter(new File("test.dat")));
            nnInput = new SimpleFileWriter("nn_input.txt");
        } catch (IOException e) {

            e.printStackTrace();
        }
    }


    public TableGenerator getTableGenerator() {
        return tableGenerator;
    }

    /**
     * Extract features for building the model
     * @override
     * @param data
     * @return
     * @throws java.io.IOException
     */
    public LinkedList<Feature[]> run(String baseDir, ArrayList<String> data, boolean learningMode) {
        allFeatures = new LinkedList<Feature[]>();
        String parsedDir = baseDir + "parsed/";
        String annotationDir = baseDir + "annotation/";
        String originalDir = baseDir + "text/";
        String removed = baseDir + "removed/";
        String htmlDir = baseDir + "html/";
//        String tableRemoved = baseDir + "tableRemoved/";
//        String codeRemoved = baseDir + "codeRemoved/";
//        String equRemoved = baseDir + "equRemoved/";
//        String miscRemoved = baseDir + "miscRemoved/";

        String output = baseDir + "output/";
        String sent2vecDir = baseDir + "sentvec/";

        SimpleFileWriter writer = null;
        SimpleFileWriter removedWriter = null;
        SimpleFileWriter removedTableWriter = null;
        SimpleFileWriter removedCodeWriter = null;
        SimpleFileWriter removedEquWriter = null;
        SimpleFileWriter removedMiscWriter = null;
        SimpleFileWriter htmlWriter = null;



        //initialize component count

        componentCount = new int[5];
        for (int i = 0; i < 5; i++) {
            componentCount[i] = 0;
        }


        // read feature.dat
        if(!FeatureParameter.featureIndexReset) {
            try {
                System.out.println("reading features..");
                BufferedReader br = new BufferedReader(new FileReader(new File("feature.dat")));
                String line;
                while ((line = br.readLine()) != null) {
                    StringTokenizer st = new StringTokenizer(line, ":", true);
                    int index = Integer.parseInt(st.nextToken());
                    st.nextToken(); // throwing the first delim
                    StringBuilder sb = new StringBuilder();
                    while (st.hasMoreTokens()) {
                        sb.append(st.nextToken());
                    }
                    String value = sb.toString();
                    System.out.println(index + ": " + value);
                    featureMap.put(value, index);
                    featureinverseMap.put(index, value);
                }
                featureNodeNum = featureMap.size();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Multiset<String> globalComponentCount = HashMultiset.create();

        try {
            for (String filename : data) {
                DEPReader reader = new DEPReader(0, 1, 2, 3, 4, 5, 6);
                initiatedTag = null;
                // resetting for each doucment
                previousNodePrediction  = TagConstant.TEXT;
                Multiset<String> componentCount = HashMultiset.create();
                if (filename.contains(".DS_Store")) continue;
                print = false;
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
                System.out.println(filename);

                SimpleFileReader treader = new SimpleFileReader(originalDir + filename);
       //         SimpleFileReader vecReader = new SimpleFileReader(sent2vecDir + filename + ".vec");
                boolean isTagBeginLine = false;
                String endTag = null;

                // current line - 1 previous line
                String prevLine_1 = "";
                // current line - 2 previous line
                String prevLine_2 = "";
                String textLine;


                // reading "/annotated" files
                ArrayList<String> annotatedLines = new ArrayList<String>();
                String l = "";
                String tagRemoved = "";
                while (freader.hasMoreLines()) {
                    l = freader.readLine();
                    if (l.isEmpty() || l.trim().length() == 0 || l.replace(" ", "").length() == 0) continue;
                    annotatedLines.add(l.trim());
                }
                // reading "/text" files
                ArrayList<String> originalLines = new ArrayList<String>();
       //         vecReader.hasMoreLines(); // reading away the first line
                String v = "";
                ArrayList<double[]> vectorLines = new ArrayList<double[]>();
                while (treader.hasMoreLines()) {
      //              vecReader.hasMoreLines();
                    l = treader.readLine();
     //               v = vecReader.readLine();
                    if (l.isEmpty() || l.trim().length() == 0 || l.replace(" ", "").length() == 0) continue;
                    else {
                        originalLines.add(l);

                        String[] tokens = v.split(" ");
                        double[] vector = new double[200];
                        for(int i=0; i<tokens.length - 1; i++) {
                            vector[i] = Double.parseDouble(tokens[i+1]);
                        }

                        vectorLines.add(vector);
                    }
                }
                assert(originalLines.size() == vectorLines.size());
                //     System.out.println(annotatedLines.size() + " lines vs " + originalLines.size());


                if (!isLearningMode) {
                    writer = new SimpleFileWriter(output + filename);
                    removedWriter = new SimpleFileWriter(removed + filename);
              //      removedTableWriter = new SimpleFileWriter(tableRemoved + filename);
              //      removedCodeWriter = new SimpleFileWriter(codeRemoved + filename);
              //      removedEquWriter = new SimpleFileWriter(equRemoved + filename);
              //      removedMiscWriter = new SimpleFileWriter(miscRemoved + filename);
                    htmlWriter = new SimpleFileWriter(htmlDir + filename + ".html");
                    htmlWriter.write("<!DOCTYPE html>\n" +
                           "<link rel=\"stylesheet\" href=\"./css/bootstrap.css\">\n" +
                            "<center>" +
                            "<h2> " + filename + "</h2>" +
                            "<table style=\"width:80%\">");
                    htmlWriter.writeLine("<Table>");

                }
                int j = 0;

                LinkedList<String> table = new LinkedList<String>();
                int documentLength = annotatedLines.size();
                double[] vector;
                for (int i = 0; i < annotatedLines.size(); i++) {
                    boolean treeIdxSkip = false;
                    line = annotatedLines.get(i);
                    tagRemoved = line;

                    for (String t : startTags) {
                        if (tagRemoved.contains(t)) {
                            tagRemoved = tagRemoved.replace(t, "");
                            initiatedTag = t;
  //                          endTag = TagConstant.findMatchingEndTag(initiatedTag);
                            tagClosed = false;
                        }
                    }

                    if (tagRemoved.isEmpty() || tagRemoved.trim().length() == 0) {
                        continue;
                    }

                    for (String t : closetags) {
                        if (tagRemoved.contains(t)) {
                            tagRemoved = tagRemoved.replace(t, "");
      //                      endTag = t;
                            tagClosed = true;
                        }
                    }
                    if (tagRemoved.isEmpty() || tagRemoved.trim().length() == 0) {
                        initiatedTag = null;
                        continue;
                    }


                    textLine = originalLines.get(j);
                    vector = vectorLines.get(j++);
                    System.out.println(filename + ": " + line + "vs  "+ textLine);

             //       if(line.trim().toLowerCase().charAt(0) != textLine.trim().toLowerCase().charAt(0))
             //           System.out.println(i + ":" + line + " vs " + textLine);

                    if(initiatedTag != null) {
                        if (initiatedTag.equals(TagConstant.tableTag)) {
                            table.add(textLine);
                        }
                    }
                    if (i == 0) {
                        prevLine_1 = "";
                        prevLine_2 = "";
                    } else if (i == 1) {
                        prevLine_1 = originalLines.get(j - 1);
                        prevLine_2 = "";
                    } else {
                        prevLine_1 = originalLines.get(j - 1);
                        prevLine_2 = originalLines.get(j - 2);
                    }


                    if (!onlyContainsTag(line)) {
                        FeatureParameter param;
                        double location = (double)i / (double)documentLength;
                        param = new FeatureParameter.Builder(treelist.get(treeIdx), location)
                                .tagType(initiatedTag).setLines(prevLine_2, prevLine_1, line).setVector(vector).build();
                        int component = addFeature(param);
                        if(!isLearningMode) {
                            if (component == TagConstant.TEXT) {
                                writer.writeLine(line);
                                htmlWriter.writeLine("<tr><td>" + line + "</td></tr>");
                                componentCount.add("TEXT");

                            }
                            else {
                                removedWriter.writeLine(line);
                                if (component == TagConstant.BEGINTABLE) {
                                    htmlWriter.writeLine("<tr><td class=\"table\"> [Table] " + line + "</td></tr>");
                                    componentCount.add("TABLE");
                          //          System.out.println(line);
                           //         removedTableWriter.writeLine(line);
                                } else if (component == TagConstant.BEGINCODE) {
                                    htmlWriter.writeLine("<tr><td class=\"code\"> [Code] " + line + "</td></tr>");
                                    componentCount.add("CODE");

                                    //         removedCodeWriter.writeLine(line);
                                } else if (component == TagConstant.BEGINEQU) {
                                    htmlWriter.writeLine("<tr><td class=\"equ\"> [Equ] " + line + "</td></tr>");
                                    componentCount.add("EQU");

                                    //         removedEquWriter.writeLine(line);
                                } else if (component == TagConstant.BEGINMISC) {
                           //         removedMiscWriter.writeLine(line);
                                    htmlWriter.writeLine("<tr><td class=\"misc\"> [Misc] " + line + "</td></tr>");
                                    componentCount.add("MISC");

                                }
                            }

                        }
                        treeIdx++;
                        //      prevLine_2 = prevLine_1;
                        //      prevLine_1 = line;
                    }


                    if (tagClosed) {
                        if(initiatedTag != null) {
                            if (initiatedTag.equals(TagConstant.tableTag)) {
                      //          tableGenerator.addTable(table);
                                table = new LinkedList<String>();
                            }
                        }
                        initiatedTag = null;
                        tagClosed = false;

                    }
                }
                if (!isLearningMode) {
                    writer.close();
                    htmlWriter.write("</table>");
                    int size = componentCount.size();
                    for(String comp : componentCount.elementSet()) {
                        htmlWriter.writeLine(comp  + ":"  + (double)componentCount.count(comp)/(double)size + "<br>");
                    }
                    globalComponentCount.addAll(componentCount);
                /*    removedCodeWriter.close();
                    removedTableWriter.close();
                    removedEquWriter.close();
                    removedMiscWriter.close();
               */
                }
            }
            int size = globalComponentCount.size();
            for(String comp : globalComponentCount.elementSet()) {
                System.out.println(comp  + ":"  + (double)globalComponentCount.count(comp)/(double)size);
                System.out.println(comp  + ":"  + globalComponentCount.count(comp) + "\t" + size);

            }

        //    nnInput.close();
        //    System.out.println("NNinput successfully written");
    } catch (Exception e) {
            e.printStackTrace();
        }

        // all the stats for feature analysis
          return allFeatures;

    }

    // how much of the tokens were unseen?
    public void printEmbeddingNullRatio() {
        for(int i=0; i<5; i++) {
            System.out.println(TagConstant.getTagLabel(i) + "\t" + (double)vectorNullCount[i]/(double)vectorTokenCount[i]);
        }
    }

    // I want to skip this line if it only contains tag without any main text because parsing tree doesn't exist for this line
    private boolean onlyContainsTag(String line) {
     //   System.out.println(line);
        if (line.replace("</EQUATION>", "").trim().isEmpty()) {
            tagClosed = true;
            return true;
        }
        else if (line.replace("</TABLE>", "").trim().isEmpty()) {
            tagClosed = true;
            return true;
        }
        else if (line.replace("</CODE>", "").trim().isEmpty()) {
            tagClosed = true;
            return true;
        }
        else if (line.replace("</MISCELLANEOUS>", "").trim().isEmpty()) {
            tagClosed = true;
            return true;
        }
        else if (line.replace("<MISCELLANEOUS>", "").trim().isEmpty()) return true;
        else if (line.replace("<CODE>", "").trim().isEmpty()) return true;
        else if (line.replace("<TABLE>", "").trim().isEmpty()) return true;
        else if (line.replace("<EQUATION>", "").trim().isEmpty()) return true;

        return false;
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
        Multiset<String> codeDetect = HashMultiset.create();
        Multiset<Integer> posTagBag = HashMultiset.create();

       // Table component baseline
        String line = param.getCurrentLine().replace("<TABLE>","").replace("</TABLE>","");
        String prev_1_line = param.getPrev_1_line().replace("<TABLE>","").replace("</TABLE>","");
        String prev_2_line = param.getPrev_2_line().replace("<TABLE>","").replace("</TABLE>","");

        String rline = tableDetecter.encodeString(tableDetecter.restoreParenthesis(line));
        String rprev_1_line = tableDetecter.encodeString(tableDetecter.restoreParenthesis(prev_1_line));
        String rprev_2_line = tableDetecter.encodeString(tableDetecter.restoreParenthesis(prev_2_line));
        int edcount = tableDetecter.getEditDistance(rline, rprev_1_line);
        int edcount2 = tableDetecter.getEditDistance(rline, rprev_2_line);
        double probability = tableDetecter.tableLMProbability(rline);
        double rightMatchingRatio = tableDetecter.getRatioRightMatching(rline, rprev_1_line);
      //  double leftMatchingRatio = DetectTable.getRatioLeftMatching(rline, rprev_1_line);
      //  int edcount2 = DetectTable.getEditDistance(rline, rprev_2_line);

        HashSet<Integer> featureNodeIndex = new HashSet<Integer>();


  /*      else {
            editDistanceSum += edcount;
            editDistanceCount++;
            patternProbWeightedED += edcount * probability;
            linelinegth += rline.length();
            rightMatchingRatioForOther += rightMatchingRatio;
            leftMatchingRatioForOther += leftMatchingRatio;

        }
*/

        features.add(new FeatureNode(getFeatureIndex("FEATURE_EDIT_DISTANCE"), edcount));
        features.add(new FeatureNode(getFeatureIndex("FEATURE_EDIT_DISTANCE2"), edcount2));
        features.add(new FeatureNode(getFeatureIndex("FEATURE_COMPLEXITY"), edcount + probability));
   //   features.add(new FeatureNode(getFeatureIndex("WEIGHTED_ED"), edcount * probability));
   //   features.add(new FeatureNode(getFeatureIndex))
        features.add(new FeatureNode(getFeatureIndex(rline), 1));
  //    features.add(new FeatureNode(getFeatureIndex("NUMBERTOKEN"), DetectTable.getNumberTokenCount(rline)));
  //    features.add(new FeatureNode(getFeatureIndex("NUMBERTOKEN2"), Math.abs(DetectTable.getNumberTokenCount(rline) - DetectTable.getNumberTokenCount(rprev_1_line))));
        features.add(new FeatureNode(getFeatureIndex("FEATURE_LENGTH_DIFF"), Math.abs(rline.length() - rprev_1_line.length())));
  //    features.add(new FeatureNode(getFeatureIndex("RIGHTMATCHING"), DetectTable.getRatioRightMatching(line, prev_1_line)));
        features.add(new FeatureNode(getFeatureIndex("FEATURE_RIGHT_MATCHING"), rightMatchingRatio));
  //    features.add(new FeatureNode(getFeatureIndex("LEFT_MATCHING"), leftMatchingRatio));



        // added word embeddings features 9/26/15

         WordVector lineVector = new WordVector();


        featureNodeIndex.add(getFeatureIndex(rline));

        for(int i=1; i<size; i++) {
            DEPNode node = tree.get(i);

            WordVector wordVector = vlu.getWordVector(node.form.toLowerCase());
            if(wordVector != null) {
                lineVector.addVector(wordVector);
            }
            if(wordVector == null)
                vectorNullCount[TagConstant.getComponentID(param.getTagType())]++;
            vectorTokenCount[TagConstant.getComponentID(param.getTagType())]++;


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

            // Code component baseline


            if (codeBaselineFeatureOn) {

                 // Is this token a bracket?
                codeDetect.add(IS_BRACKET, DetectCodeComponent.isBracket(node.form) ? 1 : 0);

                // Is this token a Camal case variable name?
                codeDetect.add(IS_VARIABLE, DetectCodeComponent.isVariable(node.form) ? 1 : 0);

                // Is this token a comment "//"?
                codeDetect.add(IS_COMMENT, DetectCodeComponent.isComment(node.form) ? 1 : 0);

                // Is this token a programming operator?
                codeDetect.add(IS_OPERATOR, DetectCodeComponent.isOperator(node.form) ? 1 : 0);

                // Is this token a parenthesis?
                codeDetect.add(IS_PARENTHESIS, DetectCodeComponent.isParenthesis(node.form) ? 1 : 0);

                // Is this token a parenthesis?
                codeDetect.add(IS_SEMICOLON, DetectCodeComponent.isSemicolon(node.form) ? 1 : 0);

                // keyword contain
                codeDetect.add(KEYWORD_CONTAIN, DetectCodeComponent.isThisKeyword(node.form) ? 1 : 0);
            }

        }




        //embedding features
        boolean sent2vec = false;

        if(sent2vec) {
            double[] sentenceEmbedding = param.getVector();
            for (int i = 0; i < 200; i++) {
                features.add(new FeatureNode(getFeatureIndex("EMBEDDING_" + i), sentenceEmbedding[i]));
            }
        }
        else { // word2vec avg
            for(int i=0; i<200; i++) {
                features.add(new FeatureNode(getFeatureIndex("EMBEDDING_"+i), lineVector.doubleAt(i)/(double)(size)));
            }
        }

        for(Integer id : dependentRelationBag.elementSet()) {
            if(!featureNodeIndex.contains(id)) {
                featureNodeIndex.add(id);
                features.add(new FeatureNode(id, (double)dependentRelationBag.count(id)/(double)(size)));
            }

        }
        for(Integer id : posTagBag.elementSet()) {
            if(!featureNodeIndex.contains(id)) {
                featureNodeIndex.add(id);
                features.add(new FeatureNode(id, posTagBag.count(id)));
            }

        }


     /*   for(Integer id : codeDetect.elementSet()) {
            if (!featureNodeIndex.contains(id)) {
                featureNodeIndex.add(id);
                features.add(new FeatureNode(id, codeDetect.count(id)));
            }
        }
        */

        for(Integer id : grams.elementSet()) {
            if(!featureNodeIndex.contains(id)) {
                featureNodeIndex.add(id);
                features.add(new FeatureNode(id, grams.count(id)));
            }
        }

        features.add(new FeatureNode(getFeatureIndex("PREVIOUS_NODE_PREDICTION"), previousNodePrediction));

        return features;
    }



    /**
     * write feature index for neural network
     */
    private void extractFeatureVocabulary(FeatureParameter param) {
        HashSet<Integer> featurebag = new HashSet<Integer>();
        DEPTree tree = param.getParsingTree();
        int size = tree.size(), featureIndx;
        Multiset<Integer> dependentRelationBag = HashMultiset.create();
        Multiset<Integer> grams = HashMultiset.create();
        Multiset<String> codeDetect = HashMultiset.create();
        Multiset<Integer> posTagBag = HashMultiset.create();

        // Table component baseline
        String line = param.getCurrentLine().replace("<TABLE>","").replace("</TABLE>","");
        String prev_1_line = param.getPrev_1_line().replace("<TABLE>","").replace("</TABLE>","");
        String prev_2_line = param.getPrev_2_line().replace("<TABLE>","").replace("</TABLE>","");

        String rline = tableDetecter.encodeString(tableDetecter.restoreParenthesis(line));
        String rprev_1_line = tableDetecter.encodeString(tableDetecter.restoreParenthesis(prev_1_line));
        String rprev_2_line = tableDetecter.encodeString(tableDetecter.restoreParenthesis(prev_2_line));


        featurebag.add(getFeatureIndex(rline));
        featurebag.add(getFeatureIndex(rprev_1_line));
        featurebag.add(getFeatureIndex(rprev_2_line));

        // added word embeddings features 9/26/15

        for(int i=1; i<size; i++) {
            DEPNode node = tree.get(i);
            List<DEPArc> dependents = node.getDependents();
            // check feature index duplicate

            for (DEPArc darc : dependents) {
                int depId = getFeatureIndex(darc.getLabel());
                featurebag.add(getFeatureIndex(node.pos + " " + darc.getNode().pos));
                featurebag.add(depId);
            }


            // unigram feature
            featurebag.add(getFeatureIndex(node.form));
            // bigram features
            if (i < size - 1) {
                featurebag.add(getFeatureIndex(node.form + " " + tree.get(i + 1).form));
            }
            featurebag.add(getFeatureIndex(node.pos));

            // Code component baseline
        }

        try {

            nnInput.write(TagConstant.getComponentID(param.getTagType()) + "\t");
            for (Integer i : featurebag) {
                nnInput.write(i + "\t");
            }
            nnInput.write("\n");
        }catch(Exception e) {
            e.printStackTrace();
        }
    }



    // this includes annotation tag, features, and using annotation as "previous label" features for cross validation
    protected int addFeature(FeatureParameter param) {
        NonTextualComponent component = NonTextualComponent.getComponent(param.getTagType());
        DEPTree tree = param.getParsingTree();
        int i, size = tree.size();
        tree.resetDependents();
        if(param.getCurrentLine().equals("linear run-time complexity , our parser is")) {
            boolean a = true;
        }
        LinkedList<Feature> features = extractFeatures(param);
//        extractFeatureVocabulary(param);

        // System.out.println(param.getTagType() + ": " + answers[featureIdx]);

        originalTextLines.add(param.getCurrentLine());
        Collections.sort(features, fc);
        Feature[] featureArray;
        featureArray = features.toArray(new Feature[features.size()]);

        double answer;
        if (component != null)
            answer = component.intermediate;
        else
            answer = TagConstant.TEXT;


        if(FeatureParameter.useSequentialFeature) {
        // for the second model learning
            if (FeatureParameter.predictPreviousNode) {
                currentNodePrediction = Linear.predict(FeatureParameter.firstModel, featureArray);
            } else {
                // but feature array shouldn't include previous node prediction here
                currentNodePrediction = answer;
            }
        }
        // for the first model learning
        else {
            currentNodePrediction = 0.0;
        }

        previousNodePrediction = currentNodePrediction;
        allFeatures.add(featureArray);
        answers.add(answer);
        // return the prediction for apply mode
        int prediction;
        // if it's during the first model learning mode
        if(FeatureParameter.firstModel == null) prediction = -1; // no prediction's made yet
        // if it's during the first model testing mode
        else {
            if(FeatureParameter.secondModel == null)
                prediction = (int) Linear.predict(FeatureParameter.firstModel, featureArray);
            else
                prediction = (int) Linear.predict(FeatureParameter.secondModel, featureArray);
        }

        featureIdx++;
        return prediction;
    }

    /*

    protected int isThisLineNoise(FeatureParameter param){
        DEPTree tree = param.getParsingTree();
        int i, size = tree.size();
        tree.resetDependents();
        LinkedList<Feature> features = extractFeatures(param);
        Collections.sort(features, fc);

         Feature[] featureArray;
         featureArray = features.toArray(new Feature[features.size()]);
         int prediction = (int) Linear.predict(FeatureParameter.secondModel, featureArray);
         componentCount[prediction]++;
         previousNodePrediction = prediction;


          return prediction;
        }

    */
}




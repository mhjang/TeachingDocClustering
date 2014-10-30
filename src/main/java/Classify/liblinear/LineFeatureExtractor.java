package Classify.liblinear;

import Classify.TagConstant;
import Classify.liblinear.datastructure.NonTextualComponent;
import Classify.liblinear.datastructure.FeatureParameter;
import Classify.noisegenerator.TableGenerator;
import com.clearnlp.dependency.DEPArc;
import com.clearnlp.dependency.DEPNode;
import com.clearnlp.dependency.DEPTree;
import com.clearnlp.reader.DEPReader;
import com.clearnlp.util.UTInput;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import componentDetection.DetectCodeComponent;
import componentDetection.DetectEquation;
import componentDetection.DetectTable;
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import simple.io.myungha.SimpleFileReader;
import simple.io.myungha.SimpleFileWriter;

import java.io.IOException;
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

    boolean tagClosed = false;
    DetectTable tableDetecter;
    TableGenerator tableGenerator;
    public LineFeatureExtractor(boolean isLearningMode) {
        this.isLearningMode = isLearningMode;
        tableDetecter = new DetectTable();
        tableGenerator = new TableGenerator();
    }

    public TableGenerator getTableGenerator() {
        return tableGenerator;
    }

    /**
     * Extract features for building the model
     * @override
     * @param data
     * @param applyModel: whether you use a trained model to fill out "previous label" feature
     * @return
     * @throws java.io.IOException
     */
    public LinkedList<Feature[]> run(String baseDir, ArrayList<String> data, boolean applyModel) {
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

        SimpleFileWriter writer = null;
        SimpleFileWriter removedWriter = null;
        SimpleFileWriter removedTableWriter = null;
        SimpleFileWriter removedCodeWriter = null;
        SimpleFileWriter removedEquWriter = null;
        SimpleFileWriter removedMiscWriter = null;
        SimpleFileWriter htmlWriter = null;



        if (applyModel) {
            model = loadModel();
        }

        //initialize component count

        componentCount = new int[5];
        for (int i = 0; i < 5; i++) {
            componentCount[i] = 0;
        }

        Multiset<String> globalComponentCount = HashMultiset.create();

        try {
            for (String filename : data) {
                DEPReader reader = new DEPReader(0, 1, 2, 3, 4, 5, 6);
                initiatedTag = null;

                Multiset<String> componentCount = HashMultiset.create();
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
                System.out.println(filename);

                SimpleFileReader treader = new SimpleFileReader(originalDir + filename);

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
                    annotatedLines.add(l.trim());
                }
                // reading "/text" files
                ArrayList<String> originalLines = new ArrayList<String>();
                while (treader.hasMoreLines()) {
                    l = treader.readLine();
                    if (l.isEmpty() || l.trim().length() == 0 || l.replace(" ", "").length() == 0) continue;
                    else originalLines.add(l);
                }
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
                for (int i = 0; i < annotatedLines.size(); i++) {
                    boolean treeIdxSkip = false;
                    //     System.out.println(filename + ": " + annotatedLines.get(i) + "vs  "+ originalLines.get(i));
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


                    textLine = originalLines.get(j++);

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
                        int keywordContain = DetectCodeComponent.keywordContainSize(line);
                        FeatureParameter param;
                        param = new FeatureParameter.Builder(treelist.get(treeIdx), DetectCodeComponent.codeLineEvidence(line),
                                keywordContain, DetectEquation.isEquation(line), tableDetecter.isTable(textLine),
                                applyModel).tagType(initiatedTag).setLines(prevLine_2, prevLine_1, line).build();
                        if (isLearningMode) addFeature(param);
                        else {
                            int component = isThisLineNoise(param);
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
                                    System.out.println(line);
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
                                tableGenerator.addTable(table);
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
    } catch (Exception e) {
            e.printStackTrace();
        }


        // all the stats for feature analysis
          return allFeatures;

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
        Multiset<Integer> codeDetect = HashMultiset.create();
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

        features.add(new FeatureNode(getFeatureIndex("EDIT_DISTANCE"), edcount));
        features.add(new FeatureNode(getFeatureIndex("EDIT_DISTANCE2"), edcount2));
        features.add(new FeatureNode(getFeatureIndex("COMPLEXITY"), edcount + probability));
   //   features.add(new FeatureNode(getFeatureIndex("WEIGHTED_ED"), edcount * probability));
   //   features.add(new FeatureNode(getFeatureIndex))
        features.add(new FeatureNode(getFeatureIndex(rline), 1));
  //    features.add(new FeatureNode(getFeatureIndex("NUMBERTOKEN"), DetectTable.getNumberTokenCount(rline)));
  //    features.add(new FeatureNode(getFeatureIndex("NUMBERTOKEN2"), Math.abs(DetectTable.getNumberTokenCount(rline) - DetectTable.getNumberTokenCount(rprev_1_line))));
        features.add(new FeatureNode(getFeatureIndex("LENGTH_DIFF"), Math.abs(rline.length() - rprev_1_line.length())));
  //    features.add(new FeatureNode(getFeatureIndex("RIGHTMATCHING"), DetectTable.getRatioRightMatching(line, prev_1_line)));
        features.add(new FeatureNode(getFeatureIndex("RIGHT_MATCHING"), rightMatchingRatio));
  //    features.add(new FeatureNode(getFeatureIndex("LEFT_MATCHING"), leftMatchingRatio));

        featureNodeIndex.add(getFeatureIndex(rline));

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

            // Code component baseline

            if (codeBaselineFeatureOn) {

                 // Is this token a bracket?
                codeDetect.add(getFeatureIndex(IS_BRACKET), DetectCodeComponent.isBracket(node.form) ? 1 : 0);

                // Is this token a Camal case variable name?
                codeDetect.add(getFeatureIndex(IS_VARIABLE), DetectCodeComponent.isVariable(node.form) ? 1 : 0);

                // Is this token a comment "//"?
                codeDetect.add(getFeatureIndex(IS_COMMENT), DetectCodeComponent.isComment(node.form) ? 1 : 0);

                // Is this token a programming operator?
                codeDetect.add(getFeatureIndex(IS_OPERATOR), DetectCodeComponent.isOperator(node.form) ? 1 : 0);

                // Is this token a parenthesis?
                codeDetect.add(getFeatureIndex(IS_PARENTHESIS), DetectCodeComponent.isParenthesis(node.form) ? 1 : 0);

                // Is this token a parenthesis?
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

        for(Integer id : codeDetect.elementSet()) {
            if (!featureNodeIndex.contains(id)) {
                featureNodeIndex.add(id);
                features.add(new FeatureNode(id, codeDetect.count(id)));
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
                else features.add(new FeatureNode(getFeatureIndex(PREVIOUS_LABEL), answers.get(featureIdx - 1)));
            else
                features.add(new FeatureNode(getFeatureIndex(PREVIOUS_LABEL), 0));

        }
        return features;
    }



    // this includes annotation tag, features, and using annotation as "previous label" features for cross validation
    protected void addFeature(FeatureParameter param) {
        NonTextualComponent component = NonTextualComponent.getComponent(param.getTagType());
        DEPTree tree = param.getParsingTree();
        int i, size = tree.size();
        tree.resetDependents();
        LinkedList<Feature> features = extractFeatures(param);

        // determine whether the given token is the begin / middle / end of the component
        if (component != null) {
            answers.add((double)component.intermediate);
        } else {
            answers.add((double)TagConstant.TEXT);
        }
        // System.out.println(param.getTagType() + ": " + answers[featureIdx]);
        originalTextLines.add(param.getCurrentLine());

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


}




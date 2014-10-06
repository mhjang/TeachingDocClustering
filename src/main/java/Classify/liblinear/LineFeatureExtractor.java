package Classify.liblinear;

import Classify.TagConstant;
import Classify.liblinear.datastructure.NonTextualComponent;
import Classify.liblinear.datastructure.FeatureParameter;
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

    public LineFeatureExtractor(boolean isLearningMode) {
        this.isLearningMode = isLearningMode;
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
        String output = baseDir + "output/";
        SimpleFileWriter writer = null;

        if (applyModel) {
            model = loadModel();
        }

        //initialize component count

        componentCount = new int[5];
        for (int i = 0; i < 5; i++) {
            componentCount[i] = 0;
        }


        try {
            for (String filename : data) {
                DEPReader reader = new DEPReader(0, 1, 2, 3, 4, 5, 6);
                initiatedTag = null;

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

                boolean isTagBeginLine = false, tagClosed = false;
                String endTag = null;

                // current line - 1 previous line
                String prevLine_1 = "";
                // current line - 2 previous line
                String prevLine_2 = "";
                String textLine;


                // reading "/annotated" files
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
                // reading "/text" files
                ArrayList<String> originalLines = new ArrayList<String>();
                while (treader.hasMoreLines()) {
                    l = treader.readLine();
                    if (l.isEmpty() || l.trim().length() == 0 || l.replace(" ", "").length() == 0) continue;
                    else originalLines.add(l);
                }
         //     System.out.println(annotatedLines.size() + " lines vs " + originalLines.size());


                if(!isLearningMode) {
                    writer = new SimpleFileWriter(output + filename);

                }
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


                        // If currently no tag was opened
                    if (initiatedTag == null) {
                         for (String tag : tags) {
                            if (line.contains(tag)) {
                                initiatedTag = tag;
                                endTag = TagConstant.findMatchingEndTag(initiatedTag);
                                break;
                            }
                        }
                     }
                     if (initiatedTag!= null && line.contains(endTag)) {
                            tagClosed = true;
                        }

                    if(!onlyContainsTag(line)) {
                            int keywordContain = DetectCodeComponent.keywordContainSize(line);
                            FeatureParameter param;
                            param = new FeatureParameter.Builder(treelist.get(treeIdx), DetectCodeComponent.codeLineEvidence(line),
                                    keywordContain, DetectEquation.isEquation(line), DetectTable.isTable(textLine),
                                    applyModel).tagType(initiatedTag).setLines(prevLine_2, prevLine_1, line).build();
                            if(isLearningMode) addFeature(param);
                            else {
                                String tagRemovedLine = predictAndRemoveComponent(param);
                                writer.writeLine(tagRemovedLine);
                            }
                            treeIdx++;
                      //      prevLine_2 = prevLine_1;
                      //      prevLine_1 = line;
                    }

                    if (tagClosed) {
                        initiatedTag = null;
                        tagClosed = false;
                    }
                }
                if(!isLearningMode)
                    writer.close();
            }
         } catch (Exception e) {
            e.printStackTrace();
        }


        // all the stats for feature analysis
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

    // I want to skip this line if it only contains tag without any main text because parsing tree doesn't exist for this line
    private boolean onlyContainsTag(String line) {
        if (line.replace("</EQUATION>", "").trim().isEmpty()) return true;
        else if (line.replace("</TABLE>", "").trim().isEmpty()) return true;
        else if (line.replace("</CODE>", "").trim().isEmpty()) return true;
        else if (line.replace("</MISCELLANEOUS>", "").trim().isEmpty()) return true;
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

        String rline = DetectTable.encodeString(DetectTable.restoreParenthesis(line));
        String rprev_1_line = DetectTable.encodeString(DetectTable.restoreParenthesis(prev_1_line));
        String rprev_2_line = DetectTable.encodeString(DetectTable.restoreParenthesis(prev_2_line));
        int edcount = DetectTable.getEditDistance(rline, rprev_1_line);
        int edcount2 = DetectTable.getEditDistance(rline, rprev_2_line);
        double probability = DetectTable.tableLMProbability(rline);
        double rightMatchingRatio = DetectTable.getRatioRightMatching(rline, rprev_1_line);
      //  double leftMatchingRatio = DetectTable.getRatioLeftMatching(rline, rprev_1_line);
      //  int edcount2 = DetectTable.getEditDistance(rline, rprev_2_line);

        HashSet<Integer> featureNodeIndex = new HashSet<Integer>();
        if(param.getTagType() == TagConstant.tableTag) {
            System.out.println("TABLE: " + line + " --> "+ rline);
        /*  System.out.println(line + "vs " + prev_1_line);
            System.out.println(rline + " vs " + rprev_1_line + ": " + edcount + ":" + probability + ": " + rightMatchingRatio);
            editDistanceSumForTable += edcount;
            editDistanceCountForTable++;
            patternProbWeightedEDForTable += edcount * probability;
            linelinegthForTable += rline.length();
            rightMatchingRatioForTable += rightMatchingRatio;
            leftMatchingRatioForTable += leftMatchingRatio;
        */
     //     DetectTable.addAnnotationLine(DetectTable.encodeString(rline));
        }

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

        if(param.getTagType() == TagConstant.codeTag) {
            System.out.println(param.getCurrentLine());
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


    protected String predictAndRemoveComponent(FeatureParameter param){
        DEPTree tree = param.getParsingTree();
        int i, size = tree.size();
        tree.resetDependents();
        StringBuilder noiseRemovedLine = new StringBuilder();
        for (i = 1; i < size; i++) {
            try {
                param.setCurrentIndex(i);
            } catch (Exception e) {
                e.printStackTrace();
            }
            LinkedList<Feature> features = extractFeatures(param);
            Collections.sort(features, fc);

            Feature[] featureArray;
            featureArray = features.toArray(new Feature[features.size()]);
            int prediction = (int) Linear.predict(FeatureParameter.secondModel, featureArray);
            if (param.isNoise(prediction))
                noiseRemovedLine.append(tree.get(i).form + " ");
            componentCount[prediction]++;

        }
        return noiseRemovedLine.toString();
    }


}




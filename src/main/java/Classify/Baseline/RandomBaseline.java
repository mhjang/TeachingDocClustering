package Classify.Baseline;

import Classify.TagConstant;
import Classify.liblinear.BasicFeatureExtractor;
import Classify.liblinear.datastructure.DatasetDir;
import simple.io.myungha.DirectoryReader;
import simple.io.myungha.SimpleFileReader;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by mhjang on 10/8/15.
 */
public class RandomBaseline {

    public static void main(String[] args) {
        String baseDir = DatasetDir.CLASSIFY_acl;
        RandomRatio ratio = new ACLRatio();
        DirectoryReader dr = new DirectoryReader(baseDir);
        ArrayList<String> data = dr.getFileNameList();
        String initiatedTag;
        boolean tagClosed = false;
        Random random = new Random();
        int[] componentCorrect = new int[5];
        int[] componentCount = new int[5];
        int[] componentPredict = new int[5];
        try {
            for (String filename : data) {
                initiatedTag = null;
                if (filename.contains(".DS_Store")) continue;
                SimpleFileReader freader = new SimpleFileReader(baseDir + "/annotation/" + filename);
                boolean isTagBeginLine = false;
                String endTag = null;

                // reading "/annotated" files
                ArrayList<String> annotatedLines = new ArrayList<String>();
                String l = "";
                String tagRemoved = "";
                while (freader.hasMoreLines()) {
                    l = freader.readLine();
                    if (l.isEmpty() || l.trim().length() == 0 || l.replace(" ", "").length() == 0) continue;
                    annotatedLines.add(l.trim());
                }

                String line;
                for (int i = 0; i < annotatedLines.size(); i++) {
                    line = annotatedLines.get(i);
                    tagRemoved = line;
                    for (String t : BasicFeatureExtractor.startTags) {
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
                    for (String t : BasicFeatureExtractor.closetags) {
                        if (tagRemoved.contains(t)) {
                            tagRemoved = tagRemoved.replace(t, "");
                            //                      endTag = t;
                            tagClosed = true;
                        }
                    }
                    if (tagRemoved.isEmpty() || tagRemoved.trim().length() == 0) {
                        initiatedTag = TagConstant.textTag;
                        continue;
                    }


                    String prediction;
                    double diceRange = random.nextDouble() * 100;
                    if (diceRange < ratio.TABLE)
                        prediction = TagConstant.tableTag;
                    else if (diceRange < ratio.CODE)
                        prediction = TagConstant.codeTag;
                    else if (diceRange < ratio.FORMULA)
                        prediction = TagConstant.equTag;
                    else if (diceRange < ratio.MISC)
                        prediction = TagConstant.miscTag;
                    else
                        prediction = TagConstant.textTag;

                    if(prediction.equals(initiatedTag))
                        componentCorrect[TagConstant.getComponentID(prediction)]++;
                    componentPredict[TagConstant.getComponentID(prediction)]++;
                    componentCount[TagConstant.getComponentID(initiatedTag)]++;
                    if (tagClosed) {
                        initiatedTag = TagConstant.textTag;
                        tagClosed = false;

                    }
                }


            }

            for(int i=0; i<5; i++) {
                double prec = (double)componentCorrect[i]/(double)componentPredict[i];
                double recall = (double)componentCorrect[i]/(double)componentCount[i];
                System.out.println(TagConstant.getTagLabel(i) + "\t" + String.format("%.4f", prec)
                        + "\t" + String.format("%.4f", recall) + "\t" + String.format("%.4f", 2*prec*recall/(prec+recall)));        //        System.out.println(TagConstant.getTagLabel(i) + "\t" + (double)componentCorrect[i] + "\t" + componentPredict[i] + "\t" + (double)componentCount[i]);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

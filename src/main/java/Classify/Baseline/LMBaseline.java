package Classify.Baseline;

import Classify.TagConstant;
import Classify.liblinear.BasicFeatureExtractor;
import Classify.liblinear.datastructure.ExperimentConstant;
import TeachingDocParser.Stemmer;
import TeachingDocParser.StopWordRemover;
import simple.io.myungha.DirectoryReader;
import simple.io.myungha.SimpleFileReader;

import java.util.ArrayList;

/**
 * Created by mhjang on 10/9/15.
 */
public class LMBaseline {

    private LanguageModel textModel;
    private LanguageModel tableModel;
    private LanguageModel codeModel;
    private LanguageModel equModel;
    private LanguageModel miscModel;
    private LanguageModel collectionModel;

    StopWordRemover stopWordRemover;
    Stemmer stemmer;


    int[] componentCorrect = new int[5];
    int[] componentCount = new int[5];
    int[] componentPredict = new int[5];




    public static void main(String[] args) {
        LMBaseline lmb = new LMBaseline();
        lmb.evaluateFiveFold();
      //        lmb.parameterSweep();
    }


    public void evaluateFiveFold() {
        stopWordRemover = new StopWordRemover();
        stemmer = new Stemmer();

        LanguageModel.lambda = 1.0;

        // construct five-fold
        String trainingDir = ExperimentConstant.DATASET_training5fold_acl;
        for(int i=1; i<=5; i++) {
            textModel = new LanguageModel();
            tableModel = new LanguageModel();
            codeModel = new LanguageModel();
            equModel = new LanguageModel();
            miscModel = new LanguageModel();
            collectionModel = new LanguageModel();
            System.out.println("Five-fold # " + i + "-------------------------------");
            System.out.print("Train data: ");

            for (int j = 1; j <= 5; j++) {
                if (j != i) {
                    System.out.print("f" + j + "\t");
                    constructComponentModels(trainingDir + "f" + j);
                }
            }
            System.out.println();
            LanguageModel.collectionModel = collectionModel;
            System.out.println("Test data: " + "f" + i);
            testModels(trainingDir + "f" + i);
        }

        // print the performance
        for(int i=0; i<5; i++) {
            double prec = (double)componentCorrect[i]/(double)componentPredict[i];
            double recall = (double)componentCorrect[i]/(double)componentCount[i];
            System.out.println(TagConstant.getTagLabel(i) + "\t" + String.format("%.4f", prec)
                    + "\t" + String.format("%.4f", recall) + "\t" + String.format("%.4f", 2*prec*recall/(prec+recall)));        //        System.out.println(TagConstant.getTagLabel(i) + "\t" + (double)componentCorrect[i] + "\t" + componentPredict[i] + "\t" + (double)componentCount[i]);
        }

    }

    public void parameterSweep() {
        for(int k=1; k<=10; k++) {
            LanguageModel.lambda = 0.1 * k;
            stopWordRemover = new StopWordRemover();
            stemmer = new Stemmer();

            // construct five-fold
            String trainingDir = ExperimentConstant.DATASET_training5fold_combined;
            for (int i = 1; i <= 5; i++) {
                textModel = new LanguageModel();
                tableModel = new LanguageModel();
                codeModel = new LanguageModel();
                equModel = new LanguageModel();
                miscModel = new LanguageModel();
                collectionModel = new LanguageModel();
                //      System.out.println("Five-fold # " + i + "-------------------------------");
                //      System.out.print("Train data: ");

                for (int j = 1; j <= 5; j++) {
                    if (j != i) {
                        //               System.out.print("f" + j + "\t");
                        constructComponentModels(trainingDir + "f" + j);
                    }
                }
                //      System.out.println();
                LanguageModel.collectionModel = collectionModel;
                //      System.out.println("Test data: " + "f" + i);
                testModels(trainingDir + "f" + i);
            }

            double prec = 0.0, recall = 0.0;
            // print the performance
            for (int i = 1; i < 5; i++) {
                prec += (double) componentCorrect[i] / (double) componentPredict[i];
                recall += (double) componentCorrect[i] / (double) componentCount[i];
            }
            prec /= 4.0;
            recall /= 4.0;

            double fscore = prec * recall * 2 / (prec + recall);
            System.out.println(LanguageModel.lambda + "\t" + fscore);
            // }
        }

    }


    public String computeLikelyModel(String line) {

        LanguageModel[] models = new LanguageModel[5];
        models[TagConstant.BEGINTABLE] = tableModel;
        models[TagConstant.BEGINCODE] = codeModel;
        models[TagConstant.BEGINMISC] = miscModel;
        models[TagConstant.BEGINEQU] = equModel;
        models[TagConstant.TEXT] = textModel;

        double[] compProb = new double[5];

        String[] rawwords = line.split("[^a-zA-Z0-9]+");
        String[] words = stopWordRemover.removeStopWords(rawwords);

        for(String word : words) {
            String w = stemmer.stemString(word);
            compProb[TagConstant.BEGINTABLE] += Math.log10(models[TagConstant.BEGINTABLE].getProbability(w));
            compProb[TagConstant.BEGINCODE] += Math.log10(models[TagConstant.BEGINCODE].getProbability(w));
            compProb[TagConstant.BEGINMISC] += Math.log10(models[TagConstant.BEGINMISC].getProbability(w));
            compProb[TagConstant.BEGINEQU] += Math.log10(models[TagConstant.BEGINEQU].getProbability(w));
            compProb[TagConstant.TEXT] += Math.log10(models[TagConstant.TEXT].getProbability(w));
        }

        double maxProb = Integer.MIN_VALUE;
        int max_i = -1;
        for(int i=0; i<5; i++) {
            if(compProb[i] > maxProb) {
                maxProb = compProb[i];
                max_i = i;
            }
        }
        return TagConstant.getTagLabel(max_i);

    }

    public void testModels(String testDir) {

        String baseDir = testDir;
        DirectoryReader dr = new DirectoryReader(baseDir);
        ArrayList<String> data = dr.getFileNameList();
        String initiatedTag = TagConstant.textTag;

        int[] local_componentCorrect = new int[5];
        int[] local_componentCount = new int[5];
        int[] local_componentPredict = new int[5];

        boolean tagClosed = false;
        try {
            for (String filename : data) {
                initiatedTag = null;
                if (filename.contains(".DS_Store")) continue;
                SimpleFileReader freader = new SimpleFileReader(ExperimentConstant.DATASET_training5fold_combined + "/annotation/" + filename);

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
                            tagClosed = false;
                        }
                    }
                    if (tagRemoved.isEmpty() || tagRemoved.trim().length() == 0) {
                        continue;
                    }
                    for (String t : BasicFeatureExtractor.closetags) {
                        if (tagRemoved.contains(t)) {
                            tagRemoved = tagRemoved.replace(t, "");
                            tagClosed = true;
                        }
                    }
                    if (tagRemoved.isEmpty() || tagRemoved.trim().length() == 0) {
                        initiatedTag = TagConstant.textTag;
                        continue;
                    }

                    if (tagClosed) {
                        initiatedTag = TagConstant.textTag;
                        tagClosed = false;
                    }

                    String prediction = computeLikelyModel(tagRemoved);
                    if(prediction.equals(initiatedTag)) {
                        componentCorrect[TagConstant.getComponentID(prediction)]++;
                        local_componentCorrect[TagConstant.getComponentID(prediction)]++;
                        if(initiatedTag==TagConstant.tableTag) {
                            System.out.println(tagRemoved);
                            computeLikelyModel(tagRemoved);

                        }
                    }


                        componentPredict[TagConstant.getComponentID(prediction)]++;
           //             local_componentPredict[TagConstant.getComponentID(prediction)]++;

                    componentCount[TagConstant.getComponentID(initiatedTag)]++;
            //        local_componentCount[TagConstant.getComponentID(initiatedTag)]++;

                }


            }


            // print the performance
/*
            for(int i=0; i<5; i++) {
                double prec = (double)local_componentCorrect[i]/(double)local_componentPredict[i];
                double recall = (double)local_componentCorrect[i]/(double)local_componentCount[i];
                System.out.println(TagConstant.getTagLabel(i) + "\t" + String.format("%.4f", prec)
                        + "\t" + String.format("%.4f", recall) + "\t" + String.format("%.4f", 2*prec*recall/(prec+recall)));        //        System.out.println(TagConstant.getTagLabel(i) + "\t" + (double)componentCorrect[i] + "\t" + componentPredict[i] + "\t" + (double)componentCount[i]);
            }
*/

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    private void constructComponentModels(String trainingDir) {
        String baseDir = trainingDir;
        DirectoryReader dr = new DirectoryReader(baseDir);
        ArrayList<String> data = dr.getFileNameList();
        String initiatedTag = TagConstant.textTag;

        boolean tagClosed = false;
        try {
            for (String filename : data) {
                initiatedTag = null;
                if (filename.contains(".DS_Store")) continue;
                SimpleFileReader freader = new SimpleFileReader(ExperimentConstant.DATASET_training5fold_combined + "/annotation/" + filename);

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
                            tagClosed = false;
                        }
                    }
                    if (tagRemoved.isEmpty() || tagRemoved.trim().length() == 0) {
                        continue;
                    }
                    for (String t : BasicFeatureExtractor.closetags) {
                        if (tagRemoved.contains(t)) {
                            tagRemoved = tagRemoved.replace(t, "");
                            tagClosed = true;
                        }
                    }
                    if (tagRemoved.isEmpty() || tagRemoved.trim().length() == 0) {
                        initiatedTag = TagConstant.textTag;
                        continue;
                    }

                   if(initiatedTag == TagConstant.textTag) {
                       textModel.addCorpus(tagRemoved);
                   }
                   else if(initiatedTag == TagConstant.tableTag) {
                       tableModel.addCorpus(tagRemoved);
                   }
                   else if(initiatedTag == TagConstant.codeTag) {
                       codeModel.addCorpus(tagRemoved);
                   }
                   else if(initiatedTag == TagConstant.equTag) {
                       equModel.addCorpus(tagRemoved);
                   }
                   else {
                       miscModel.addCorpus(tagRemoved);
                   }
                    collectionModel.addCorpus(tagRemoved);
                    if (tagClosed) {
                        initiatedTag = TagConstant.textTag;
                        tagClosed = false;

                    }
                }


            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


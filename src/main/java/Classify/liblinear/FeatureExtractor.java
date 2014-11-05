package Classify.liblinear;

import Classify.liblinear.datastructure.FeatureParameter;
import Classify.noisegenerator.TableGenerator;
import de.bwaldvogel.liblinear.*;

import java.io.*;
import java.util.*;

/**
 * Created by mhjang on 4/27/14.
 */


public class FeatureExtractor {

    int[] componentCount;

    public static int LINE_BASED = 0;
    public static int TOKEN_BASED = 1;

    BasicFeatureExtractor extractor;
    String baseDir;
    ArrayList<String> fileList;
    boolean isLearningMode;
    public FeatureExtractor(String baseDir_, ArrayList<String> fileList_, boolean isLearningMode) {
        this.baseDir = baseDir_;
        this.fileList = fileList_;
        this.isLearningMode = isLearningMode;
    }

    public TableGenerator getTableGenerator() {
        return extractor.getTableGenerator();
    }

    public LinkedList<Feature[]>  extractFeatures(int unit) {
        if(unit == FeatureExtractor.LINE_BASED) {
            extractor = new LineFeatureExtractor(isLearningMode);
        }
        else
            extractor = new TokenFeatureExtractor(isLearningMode);
   /*     if(!isLearningMode) {
            if(FeatureParameter.firstModel == null || FeatureParameter.secondModel == null) {
                System.out.println("Error: Models have to be set at FeatureParameter to apply classifiers to the documents!");
                System.out.println("Set the saved models at FeatureParameter.setModels(first, second)");
                return null;
            }
        }
        */




        return extractor.run(baseDir, fileList, isLearningMode);
    }

    public void generateFeatureIndexFile() {
        HashMap<String, Integer> featureMap = extractor.getFeatureDictionary();
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File("feature.dat")));
            for(String word : featureMap.keySet()) {
                // the index number first because it's easier for parsing later
                bw.write(featureMap.get(word) + ":" + word + "\n");
            }
            bw.close();
            System.out.println("features are written at feature.dat");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    // # of training instances
    public int getNumOfInstances() {
        return extractor.featureNodeNum;
    }
    // for evaluation
    public double[] getTrainingAnswers() {
        ArrayList<Double> answers = extractor.answers;
        double[] trainingAnswers = new double[answers.size()];
        for(int i=0; i<trainingAnswers.length; i++) {
            trainingAnswers[i] = answers.get(i).doubleValue();
        }
        return trainingAnswers;
    }

    public HashMap<Integer, String> getFeatureInverseDic() {
        return extractor.getFeatureInverseDictionary();
    }

    public ArrayList<String> getOriginalText() {
        return extractor.originalTextLines;
    }
/*
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
    */


}




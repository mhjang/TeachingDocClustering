package Classify.liblinear;

/**
 * Created by mhjang on 10/17/15.
 */
public class FeatureExperiment {
    static boolean tableFeature = false;
    static boolean codeFeature = false;
    static boolean sequentialFeature = false;
    static boolean embeddingFeature = false;
    static boolean ngramFeature = false;
    static boolean parsingFeature = false;
    static boolean[] features = {FeatureExperiment.ngramFeature, FeatureExperiment.embeddingFeature, FeatureExperiment.parsingFeature, FeatureExperiment.tableFeature, FeatureExperiment.sequentialFeature};

}

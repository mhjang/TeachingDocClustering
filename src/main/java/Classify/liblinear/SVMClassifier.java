package Classify.liblinear;

import Classify.FeatureExtractor;
import Classify.TagConstant;
import de.bwaldvogel.liblinear.*;
import simple.io.myungha.DirectoryReader;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by mhjang on 7/16/14.
 * It is a wrapper class that uses liblinear API for SVM Classification.
 */
public class SVMClassifier {
    /***
     * Directory structure
     * base directory |-- /annotation : contains annotation files
     *                |-- /parsed : contains *.cnlp files
     * Generates a Problem instance with feature extracted from the given base directory
     * @param baseDir
     * @return
     * @throws java.io.IOException
     */
    FeatureExtractor ef = new FeatureExtractor();

    private Problem generateProblem(String baseDir) throws IOException {
        String allAnnotationDir = baseDir + "annotation/";

        DirectoryReader dr = new DirectoryReader(allAnnotationDir);
        // generate features from all annotation files because it's for five fold cross validation
        LinkedList<Feature[]> allFeatures = ef.generateClassifyFeatures(baseDir, dr.getFileNameList());
        // convert it to 2D array format
        Feature[][] allFeaturesArray = new Feature[allFeatures.size()][];
        for (int i = 0; i < allFeatures.size(); i++) {
            allFeaturesArray[i] = allFeatures.get(i);
        }
        // generating Problem instance
        Problem problem = new Problem();
        problem.x = allFeaturesArray;
        problem.n = ef.featureNodeNum - 1;
        problem.y = Arrays.copyOfRange(ef.answers, 0, allFeatures.size());
        problem.l = allFeatures.size();

        return problem;
    }


    public void runFiveFoldCrossValidation(String baseDir) throws IOException {
        //       Problem problem = generateProblem("/Users/mhjang/Desktop/clearnlp/allslides/");
        Problem problem = generateProblem(baseDir);

        SolverType solver = SolverType.L2R_L2LOSS_SVC; // -s 0
        double C = 1.0;    // cost of constraints violation
        double eps = 0.01; // stopping criteria

        Parameter param = new Parameter(SolverType.L2R_L2LOSS_SVC, 10, 0.01);
        int nr_fold = 5;
        double[] target = new double[problem.l];
        // the predicted results are saved at "target" array
        Linear.crossValidation(problem, param, nr_fold, target);
        // evaluate it by comparing target and ef.answers
        evaluate(target, ef.answers, true);
    }

    /**
     * It saves the model to the given modelFileName from learning with features extracted annotations and parsing results in the base directory.
     * @param baseDir, modelFileName
     * @throws IOException
     */
    public void createModel(String baseDir, String modelFileName) throws IOException {
//        Problem problem = generateProblem("/Users/mhjang/Desktop/clearnlp/allslides/");
        Problem problem = generateProblem(baseDir);

        SolverType solver = SolverType.L2R_L2LOSS_SVC; // -s 0
        double C = 1.0;    // cost of constraints violation
        double eps = 0.01; // stopping criteria

        Parameter param = new Parameter(SolverType.L2R_L2LOSS_SVC, 10, 0.01);

        Model model = Linear.train(problem, param);
        File modelFile = new File(modelFileName);
        model.save(modelFile);


    }

    public void applyModelToDocuments(String modelFileName) throws IOException {
        FeatureExtractor ef = new FeatureExtractor();
        File modelFile = new File(modelFileName);
        Model model = Model.load(modelFile);
        String baseDir = "/Users/mhjang/Desktop/clearnlp/allslides/";
        String allParsingDir = baseDir + "parsed/";

        ef.prepareFeatureForDocuments(allParsingDir, model);
    }

    /**
     * Simply compares results
     * @param predictions
     * @param answers
     */
    public void evaluate(double[] predictions, double[] answers, boolean byComponent) {
        int correctItem = 0;
        int predictedComponents = 0;
        HashMap<Integer, Integer> occurenceMap = new HashMap<Integer, Integer>();
        HashMap<Integer, Integer> truthMap = new HashMap<Integer, Integer>();
        int truth, prediction;
        for(int i=0; i<answers.length; i++) {
            if(byComponent) {
                truth = (int) answers[i];
                prediction = (int) predictions[i];
            }
            else {
                truth = TagConstant.getTagIDByComponent((int) answers[i]);
                prediction = TagConstant.getTagIDByComponent((int)predictions[i]);
            }

            if (truth == prediction) correctItem++;
            if(prediction != (int) TagConstant.TEXT) {
                predictedComponents++;
            }
            // counting occurrences of each label in the prediction
            if(!occurenceMap.containsKey(prediction))
                occurenceMap.put(prediction, 1);
            else
                occurenceMap.put(prediction, occurenceMap.get(prediction) + 1);
            // counting occurrences of each label in the truth data
            if(!truthMap.containsKey(truth))
                truthMap.put(truth, 1);
            else
                truthMap.put(truth, truthMap.get(truth) + 1);
       }

        System.out.println("Total Accuracy: " + (double) correctItem / (double) predictions.length);
        for(Integer label: truthMap.keySet()) {
            System.out.println(TagConstant.getTagLabel(label) + ": " + (double) (occurenceMap.get(label) / (double) (truthMap.get(label))));
        }
    }

    public void evaluateExactMatch() {
         /*
        while(i<target.length) {
            System.out.println("predicted label: " + target[i] + " answer: " +  ef.answers[i]);
            int answerType = (int) ef.answers[i];
            int predictedType = (int) target[i];

           if (answerType == predictedType && predictedType == TagConstant.BEGINCODE) {
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
    }



}

package Classify.liblinear;

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
    private Model model;


    public Problem generateProblem(String baseDir) {
        return generateProblemRoutine(baseDir, false, null);

    }

    public Problem generateProblem(String baseDir, Model model) {
        return generateProblemRoutine(baseDir, true, model);
    }

    private Problem generateProblemRoutine(String baseDir, boolean applyModel, Model model) {
        try {
            String allAnnotationDir = baseDir + "annotation/";

            DirectoryReader dr = new DirectoryReader(allAnnotationDir);
            // generate features from all annotation files because it's for five fold cross validation
            LinkedList<Feature[]> allFeatures;
            if(applyModel)
                ef.setModel(model);
            allFeatures = ef.generateClassifyFeatures(baseDir, dr.getFileNameList(), applyModel);
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Generate Problem with features extracted so that liblinear package can run "Train" method on it.
     * @param baseDir
     * @return
     * @throws IOException
     */
    private Problem generateProblem(String baseDir, boolean useAnnotation) throws IOException {
        String allAnnotationDir = baseDir + "annotation/";

        DirectoryReader dr = new DirectoryReader(allAnnotationDir);
        // generate features from all annotation files because it's for five fold cross validation
        LinkedList<Feature[]> allFeatures = ef.generateClassifyFeatures(baseDir, dr.getFileNameList(), useAnnotation);

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

    /**
     * @param baseDir
     * @param applyModel
     */
    public void runFiveFoldCrossValidation(String baseDir, boolean applyModel) {
        System.out.println("Running five fold cross validation with annotation " + ((applyModel)? "off" : "on"));
        try {
            Problem problem;
            if(!applyModel) {
                problem = generateProblem(baseDir);
            }
            else {
                problem = generateProblem(baseDir, model);
            }

            SolverType solver = SolverType.L2R_L2LOSS_SVC; // -s 0
            double C = 1.0;    // cost of constraints violation
            double eps = 0.01; // stopping criteria

            Parameter param = new Parameter(SolverType.L2R_L2LOSS_SVC, 10, 0.01);
            int nr_fold = 5;
            double[] target = new double[problem.l];
            // the predicted results are saved at "target" array
            Linear.crossValidation(problem, param, nr_fold, target);
            // evaluate it by comparing target and ef.answers
            evaluate(target, ef.answers, applyModel);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setModel(String modelName) {
        try {
            File file = new File(modelName);
            Model model_ = Linear.loadModel(file);
            this.model = model_;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * It first trains the model using all files and annotation, and saves the model to output file.
     * @param baseDir, modelFileName
     * @throws IOException
     */
    public void learnModel(String baseDir, String modelFileName) {
//        Problem problem = generateProblem("/Users/mhjang/Desktop/clearnlp/allslides/");
        try {
            Problem problem = generateProblem(baseDir, false);

            SolverType solver = SolverType.L2R_L2LOSS_SVC; // -s 0
            double C = 1.0;    // cost of constraints violation
            double eps = 0.01; // stopping criteria

            Parameter param = new Parameter(SolverType.L2R_L2LOSS_SVC, C, eps);

            Model model = Linear.train(problem, param);
            File modelFile = new File(modelFileName);
            model.save(modelFile);
            setModel(modelFileName);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void applyModelToDocuments(String modelFileName) {
        try {
            FeatureExtractor ef = new FeatureExtractor();
            File modelFile = new File(modelFileName);
            Model model = Model.load(modelFile);
            String baseDir = "/Users/mhjang/Documents/teaching_documents/extracted/stemmed/";
            String allParsingDir = baseDir + "parsed/";

            ef.prepareFeatureForDocuments(allParsingDir, model);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Simply compares results
     * @param predictions
     * @param answers
     */
    public void evaluate(double[] predictions, double[] answers, boolean byComponent) {
        int correctItem = 0;
        int predictedComponents = 0;
        // recall denominator
        HashMap<Integer, Integer> occurenceMap = new HashMap<Integer, Integer>();
        // precision denominator
        HashMap<Integer, Integer> truthMap = new HashMap<Integer, Integer>();
        // nominator
        HashMap<Integer, Integer> correctItemMap = new HashMap<Integer, Integer>();
        int truth, prediction;
        for(int i=0; i<predictions.length; i++) {
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
            if(prediction == truth) {
                if (!correctItemMap.containsKey(prediction))
                    correctItemMap.put(prediction, 1);
                else
                    correctItemMap.put(prediction, correctItemMap.get(prediction) + 1);
            }
       }

        System.out.println("Total Accuracy: " + (double) correctItem / (double) predictions.length);
        System.out.println("Component: Precision, Recall");
        for(Integer label: truthMap.keySet()) {
            System.out.println(TagConstant.getTagLabel(label) + ": " + (double) (correctItemMap.get(label) / (double) (occurenceMap.get(label))) + ", " + (double) (correctItemMap.get(label) / (double) (truthMap.get(label))));
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

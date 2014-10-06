package Classify.liblinear;

import Classify.TagConstant;
import Classify.liblinear.datastructure.FeatureParameter;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import componentDetection.DetectTable;
import de.bwaldvogel.liblinear.*;
import simple.io.myungha.DirectoryReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Created by mhjang on 7/16/14.
 * It is a wrapper class that uses liblinear API for SVM Classification.
 */
public class SVMClassifier {

    // default
    int featureUnit = FeatureExtractor.LINE_BASED;



    SolverType solver = SolverType.L2R_L2LOSS_SVC; // -s 0
    double C = 1.0;    // cost of constraints violation
    double eps = 0.01; // stopping criteria

    Parameter param = new Parameter(SolverType.L2R_L2LOSS_SVC, C, eps);


    public static void main(String[] args) {
        SVMClassifier svm = new SVMClassifier();
        final boolean useAnnotationForPreviousNode = false;
        final boolean useModel = true;
        final boolean writeModel = true;

        int featureUnit = FeatureExtractor.LINE_BASED;
        //     String baseDir = "/Users/mhjang/Desktop/clearnlp/allslides/";
        //String baseDir = "/Users/mhjang/Desktop/clearnlp/allslides/annotation_processed/br/";
        //String applyDir = null;
               String baseDir = "/Users/mhjang/Desktop/clearnlp/acl/training/";
        //     String baseDir = "/Users/mhjang/Documents/teaching_documents/extracted/stemmed/parsed/gold/feature_tokens/";
        // routine 1: do five fold cross validation with annotation to evaluate the accuracy
        svm.runFiveFoldCrossValidation(baseDir, useAnnotationForPreviousNode, writeModel, featureUnit);

        // routine 2: use all data for learning a model and use the model for five fold cross validation by predicting "previous_label" field
     //   String firstModel = "slide_model_0723";
     //   String secondModel = "final_slide_model";
     //   String firstModel = "acl_initial_model";
     //   String secondModel = "acl_final_model";
     //   svm.learnFirstModel(baseDir, firstModel);
     //   svm.learnSecondModel(baseDir, firstModel, secondModel);
   //     svm.runFiveFoldCrossValidation(baseDir, useModel, false);
    /*    String applyDir = "/Users/mhjang/Desktop/clustering/extracted/br/";

        // routine 3: apply the learned model to generate the noise-free version of documents
        svm.applyModelToDocuments(applyDir, firstModel, secondModel);
    */

    }

    /***
     * Directory structure
     * base directory |-- /annotation : contains annotation files
     *                |-- /parsed : contains *.cnlp files
     * Generates a Problem instance with feature extracted from the given base directory
     * @param baseDir
     * @return
     * @throws java.io.IOException
     */
    FeatureExtractor ef;
    private Model model;


    private Problem generateProblem(String baseDir, boolean useAnnotationForPreviousNode, Model model, boolean writeFile) {
        try {
            String allAnnotationDir = baseDir + "annotation/";
            DetectTable.readStats();
            DirectoryReader dr = new DirectoryReader(allAnnotationDir);
            // generate features from all annotation files because it's for five fold cross validation
            LinkedList<Feature[]> allFeatures;
            // line or token-based?
            ef = new FeatureExtractor(baseDir, dr.getFileNameList(), true);

            if(!useAnnotationForPreviousNode)
                FeatureParameter.setFirstModel(model);
            allFeatures = ef.extractFeatures(FeatureExtractor.LINE_BASED);

            // convert it to 2D array format
            Feature[][] allFeaturesArray = new Feature[allFeatures.size()][];
            int n = allFeatures.size();
            for (int i = 0; i < n; i++) {
                allFeaturesArray[i] = allFeatures.get(i);
            }
            // generating Problem instance
            Problem problem = new Problem();
            problem.x = allFeaturesArray;
            problem.n = ef.getNumOfInstances();
            problem.y = Arrays.copyOfRange(ef.getTrainingAnswers(), 0, allFeatures.size());
            problem.l = allFeatures.size();

            if(writeFile) writeTrainingFile(allFeaturesArray);
    //        DetectTable.buildStatistics();
            return problem;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void writeTrainingFile(Feature[][] allFeaturesArray) {
        try {
            // write file
            int n = allFeaturesArray.length;

            BufferedWriter bw = new BufferedWriter(new FileWriter(new File("training.dat")));
            for (int i = 0; i < n; i++) {
                Feature[] features = allFeaturesArray[i];
                int answer = (int) ef.getTrainingAnswers()[i];
                bw.write(answer + " ");
                for (int j = 0; j < features.length; j++) {
                    bw.write(features[j].getIndex() + ":" + features[j].getValue() + " ");
                }
                bw.write("\n");
                bw.flush();

            }
            bw.close();


            bw = new BufferedWriter(new FileWriter(new File("feature_dic.dat")));
            HashMap<Integer, String> dictionary = ef.getFeatureInverseDic();
            ArrayList<Integer> keys = new ArrayList<Integer>(dictionary.keySet());
            Collections.sort(keys);
            for(Integer key : keys) {
                bw.write(key + " : " + dictionary.get(key) + "\n");
            }
            bw.close();

        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param baseDir
     */
    public void runFiveFoldCrossValidation(String baseDir, boolean useAnnotationForPreviousNode, boolean writeModel, int featureUnit) {
        System.out.println("Running five fold cross validation with annotation " + ((useAnnotationForPreviousNode)? "on" : "off"));
        try {
            Problem problem;
            // for five-fold cross validation
            if(!useAnnotationForPreviousNode) {
                problem = generateProblem(baseDir, useAnnotationForPreviousNode, null, writeModel);
            }
            // to save the first model
            else {
                problem = generateProblem(baseDir, useAnnotationForPreviousNode, model, writeModel);
            }
            FeatureParameter.predictPreviousNode = false;
            SolverType solver = SolverType.L2R_L2LOSS_SVC; // -s 0
            double C = 1.0;    // cost of constraints violation
            double eps = 0.01; // stopping criteria

            Parameter param = new Parameter(SolverType.L2R_L2LOSS_SVC, 10, 0.01);
            int nr_fold = 5;
            double[] target = new double[problem.l];
            // the predicted results are saved at "target" array
            Linear.crossValidation(problem, param, nr_fold, target);
            // evaluate it by comparing target and ef.answers
            evaluate(target, ef.getTrainingAnswers(), ef.getOriginalText(), true);
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
    public void learnFirstModel(String baseDir, String modelOutput) {
//        Problem problem = generateProblem("/Users/mhjang/Desktop/clearnlp/allslides/");
        try {
            boolean useAnnotationForPreviousNode = true;
            boolean writeModel = false;
            Problem problem = generateProblem(baseDir, useAnnotationForPreviousNode, null, writeModel);
            FeatureParameter.predictPreviousNode = false;

            Model model = Linear.train(problem, param);
            File modelFile = new File(modelOutput);
            model.save(modelFile);
            setModel(modelOutput);
            // setting the first model
            FeatureParameter.setFirstModel(model);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public void learnSecondModel(String baseDir, String firstModelName, String secondModelName) {
//        Problem problem = generateProblem("/Users/mhjang/Desktop/clearnlp/allslides/");
        try {
            File file = new File(firstModelName);
            boolean useAnnotationForPreviousNode = false;
            boolean writeModel = false;
            Problem problem = generateProblem(baseDir, useAnnotationForPreviousNode, model, writeModel);
            FeatureParameter.predictPreviousNode = true;

            Model secondModel = Linear.train(problem, param);
            File newModelName = new File(secondModelName);
            secondModel.save(newModelName);
           // setModel(secondModelName);
            FeatureParameter.setSecondModel(secondModel);
            System.out.println("Learned a second model using first model with previous sequence prediction feature!");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public void applyModelToDocuments(String dir, String firstModelName, String secondModelName) {
        try {
                Problem problem;
                boolean useAnnotationForPreviousNode = false;
                boolean writeModel = false;
                // read the models
                File file1 = new File(firstModelName);
                Model firstModel = Linear.loadModel(file1);
                File file2 = new File(secondModelName);
                Model secondModel = Linear.loadModel(file2);
                FeatureParameter.setModelSet(firstModel, secondModel);
                boolean isTrainingMode = false;

                DirectoryReader dr = new DirectoryReader(dir);
                // generate features from all annotation files because it's for five fold cross validation
                LinkedList<Feature[]> allFeatures;
                // line or token-based?
                boolean isLearningMode = false;
                ef = new FeatureExtractor(dir, dr.getFileNameList(), isLearningMode);
                ef.extractFeatures(FeatureExtractor.LINE_BASED);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Simply compares results
     * @param predictions
     * @param answers
     */
    /**
     * Simply compares results
     * @param predictions
     * @param answers
     */
    public void evaluate(double[] predictions, double[] answers, ArrayList<String> lines, boolean evaluateByComponent) {
        int correctItem = 0;
        int predictedComponents = 0;
        // recall denominator
        Multiset<Integer> occurrenceSet = HashMultiset.create();
        // precision denominato
        Multiset<Integer> truthSet = HashMultiset.create();
        // nominator
        Multiset<Integer> correctItemSet = HashMultiset.create();

        int truth, prediction;
        Random rn = new Random();
        for (int i = 0; i < predictions.length; i++) {
             if (!evaluateByComponent) {
                truth = (int) answers[i];
                prediction = (int) predictions[i];
            } else {
                truth = TagConstant.getTagIDByComponent((int) answers[i]);
                prediction = TagConstant.getTagIDByComponent((int) predictions[i]);
            }

            if (prediction != (int) TagConstant.TEXT) {
                predictedComponents++;
            }
            // counting occurrences of each label in the prediction
            occurrenceSet.add(prediction);
            // counting occurrences of each label in the truth data
            truthSet.add(truth);

            if (prediction == truth) {
                correctItemSet.add(prediction);
            } else {
                System.out.println("Wrong (" + TagConstant.getTagLabel(truth)+ "-> " + TagConstant.getTagLabel(prediction) +"= " + lines.get(i));
            }
        }

        System.out.println("Total Accuracy: " + (double) correctItem / (double) predictions.length);
        System.out.println("NonTextualComponent: Precision, Recall");
        int truthTotal = 0;
        for (Integer label : truthSet.elementSet()) {
            System.out.println(TagConstant.getTagLabel(label) + "\t" + (double) (correctItemSet.count(label) / (double) (occurrenceSet.count(label))) + "\t" + (double) (correctItemSet.count(label) / (double) (truthSet.count(label))));
            truthTotal += truthSet.count(label);

        }
        System.out.println("truth data ratio");
        for (Integer label: truthSet.elementSet()) {
            System.out.println(TagConstant.getTagLabel(label) + "\t" + (double) (truthSet.count(label) / (double) truthTotal));
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

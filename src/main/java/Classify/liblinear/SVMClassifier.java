package Classify.liblinear;

import Classify.TagConstant;
import Classify.liblinear.datastructure.ExperimentConstant;
import Classify.liblinear.datastructure.FeatureParameter;
import Classify.noisegenerator.TableGenerator;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import de.bwaldvogel.liblinear.*;
import simple.io.myungha.DirectoryReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static de.bwaldvogel.liblinear.SolverType.*;

/**
 * Created by mhjang on 7/16/14.
 * It is a wrapper class that uses liblinear API for SVM Classification.
 */
public class SVMClassifier {

    // default
    String firstModel;
    String secondModel;

    Parameter param;
    public SVMClassifier() {


        SolverType solver = L2R_L2LOSS_SVC; // -s 0
        double C = 1.0;    // cost of constraints violation
        double eps = 0.01; // stopping criteria

        param = new Parameter(SolverType.L2R_L2LOSS_SVC, C, eps);

    }




    public static void main(String[] args) {
        SVMClassifier svm = new SVMClassifier();

        int featureUnit = FeatureExtractor.LINE_BASED;
        String baseDir = ExperimentConstant.DATASET_trainingcombined;

  //    routine 1: do five fold cross validation with annotation to evaluate the accuracy
   //     boolean useAnnotationForPreviousNode = true;
   //     boolean writeModel = true;
   //     svm.runFiveFoldCrossValidation(baseDir, useAnnotationForPreviousNode, writeModel, featureUnit);

        // routine 2: use all data for learning a model and use the model for five fold cross validation by predicting "previous_label" field
        svm.firstModel = "combined_embedding_1st";
        svm.secondModel = "combined_embedding_final";

         svm.learnFirstModel(baseDir);
  //      svm.learnSecondModel(baseDir);

        boolean isFirstModelEvaluation = false; // in ohter words, do we use sequential features?
        boolean useAnnotation = false; // true when evaluating  "acl_second_annotation_model", false when evaluating "acl_second_prediction_model"

 //       svm.runFiveFoldCrossValidation(baseDir,isFirstModelEvaluation, useAnnotation, FeatureExtractor.LINE_BASED);


   /*    TableGenerator t = svm.getTableGenerator();
        for(int i=0; i<10; i++) {
            System.out.println("Table # " + (i+1));
            t.generateTable();
        }
*/
        String applyDir = ExperimentConstant.DATASET_acl;
        // routine 3: apply the learned model to generate the noise-free version of documents
        svm.applyModelToDocuments(applyDir);


    }

    public TableGenerator getTableGenerator() {
        return ef.getTableGenerator();
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


    private Problem generateProblem(String baseDir) {
        try {
            String allAnnotationDir = baseDir + "annotation/";
            DirectoryReader dr = new DirectoryReader(allAnnotationDir);
            // generate features from all annotation files because it's for five fold cross validation
            LinkedList<Feature[]> allFeatures;
            // line or token-based?
            ef = new FeatureExtractor(baseDir, dr.getFileNameList(), true);
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

            System.out.println("# of features: " + problem.n);
            System.out.println("# of instances: " + problem.y);
            System.out.println("# of training examples: " + problem.l);

            ef.generateFeatureIndexFile();
    //        writeTrainingFile(allFeaturesArray);
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
    public void runFiveFoldCrossValidation(String baseDir, boolean firstModelEvaluation, boolean useAnnotation, int featureUnit) {
        System.out.println("Running five fold cross validation with annotation " + ((useAnnotation)? "on" : "off"));
        try {
            Problem problem;
            // for five-fold cross validation
            FeatureParameter.featureIndexReset = false;
            // if this is the first model evaluation
            if(firstModelEvaluation) {
                FeatureParameter.useSequentialFeature = false;
                FeatureParameter.setFirstModel(Model.load(new File(firstModel)));

            }
            else { // second Model evaluation
                FeatureParameter.setSecondModel(Model.load(new File(secondModel)));
                FeatureParameter.useSequentialFeature = true;

                if (useAnnotation) {
                    FeatureParameter.predictPreviousNode = false;
                }
                else {
                    FeatureParameter.predictPreviousNode = true;
                    FeatureParameter.setFirstModel(Model.load(new File(firstModel)));
                }
             }

            problem = generateProblem(baseDir);

            SolverType solver = L2R_L2LOSS_SVC; // -s 0
            double C = 1.0;    // cost of constraints violation
            double eps = 0.01; // stopping criteria

            Parameter param = new Parameter(L2R_L2LOSS_SVC, 10, 0.01);
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
    public void learnFirstModel(String baseDir) {
//        Problem problem = generateProblem("/Users/mhjang/Desktop/clearnlp/allslides/");
        try {
            FeatureParameter.predictPreviousNode = false;
            FeatureParameter.useSequentialFeature = false;
            FeatureParameter.featureIndexReset = true;
            Problem problem = generateProblem(baseDir);

            Model model = Linear.train(problem, param);

            File modelFile = new File(firstModel);
            model.save(modelFile);
        //    setModel(firstModel);
            // setting the first model
            FeatureParameter.setFirstModel(model);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public void learnSecondModel(String baseDir) {
//        Problem problem = generateProblem("/Users/mhjang/Desktop/clearnlp/allslides/");
        try {
            boolean useAnnotationForPreviousNode = false;
            boolean writeModel = false;
            FeatureParameter.setFirstModel(Linear.loadModel(new File(firstModel)));
            FeatureParameter.featureIndexReset = false;
            FeatureParameter.predictPreviousNode = true;
            FeatureParameter.useSequentialFeature = true;

            Problem problem = generateProblem(baseDir);

            Model model = Linear.train(problem, param);
            File modelFile = new File(secondModel);
            model.save(modelFile);
           // setModel(secondModelName);
            FeatureParameter.setSecondModel(model);
            System.out.println("Learned a second model using first model with previous sequence prediction feature!");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public void applyModelToDocuments(String dir) {
        try {
                // read the models
                File file1 = new File(firstModel);
                Model firstModel = Linear.loadModel(file1);
                FeatureParameter.setFirstModel(firstModel);
          //      File file2 = new File(secondModel);
          //      Model secondModel = Linear.loadModel(file2);
          //      FeatureParameter.setModelSet(firstModel, secondModel);
                FeatureParameter.predictPreviousNode = true;

                DirectoryReader dr = new DirectoryReader(dir + "/annotation");
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

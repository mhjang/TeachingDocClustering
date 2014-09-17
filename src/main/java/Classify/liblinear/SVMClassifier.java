package Classify.liblinear;

import Classify.TagConstant;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.sun.tools.javac.util.List;
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

    /***
     * Directory structure
     * base directory |-- /annotation : contains annotation files
     *                |-- /parsed : contains *.cnlp files
     * Generates a Problem instance with feature extracted from the given base directory
     * @param baseDir
     * @return
     * @throws java.io.IOException
     */
//    FeatureExtractor ef = new FeatureExtractor();
    LineFeatureExtractor ef = new LineFeatureExtractor();
    private Model model;


    public Problem generateProblem(String baseDir, boolean writeFile) {
        return generateProblemRoutine(baseDir, false, null, writeFile);

    }

    public Problem generateProblem(String baseDir, Model model, boolean writeFile) {
        return generateProblemRoutine(baseDir, true, model, writeFile);
    }

    private Problem generateProblemRoutine(String baseDir, boolean applyModel, Model model, boolean writeFile) {
        try {
            String allAnnotationDir = baseDir + "annotation/";
            DetectTable.readStats();
            DirectoryReader dr = new DirectoryReader(allAnnotationDir);
            // generate features from all annotation files because it's for five fold cross validation
            LinkedList<Feature[]> allFeatures;
            if(applyModel)
                ef.setModel(model);
            allFeatures = ef.generateClassifyFeaturesLineBased(baseDir, dr.getFileNameList(), applyModel);

            // convert it to 2D array format
            Feature[][] allFeaturesArray = new Feature[allFeatures.size()][];
            int n = allFeatures.size();
            for (int i = 0; i < n; i++) {
                allFeaturesArray[i] = allFeatures.get(i);
            }
            // generating Problem instance
            Problem problem = new Problem();
            problem.x = allFeaturesArray;
            problem.n = ef.featureNodeNum - 1;
            problem.y = Arrays.copyOfRange(ef.answers, 0, allFeatures.size());
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
                int answer = (int) ef.answers[i];
                bw.write(answer + " ");
                for (int j = 0; j < features.length; j++) {
                    bw.write(features[j].getIndex() + ":" + features[j].getValue() + " ");
                }
                bw.write("\n");
                bw.flush();

            }
            bw.close();


            bw = new BufferedWriter(new FileWriter(new File("feature_dic.dat")));
            HashMap<Integer, String> dictionary = ef.getFeatureInverseDictionary();
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
     * Generate Problem with features extracted so that liblinear package can run "Train" method on it.
     * @param baseDir
     * @return
     * @throws IOException
     */
    private Problem generateProblem(String baseDir, boolean useAnnotation, boolean writeFile) throws IOException {
        String allAnnotationDir = baseDir + "annotation/";

        DirectoryReader dr = new DirectoryReader(allAnnotationDir);
        // generate features from all annotation files because it's for five fold cross validation
        LinkedList<Feature[]> allFeatures = ef.generateClassifyFeaturesLineBased(baseDir, dr.getFileNameList(), useAnnotation);

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

        if(writeFile) writeTrainingFile(allFeaturesArray);


        return problem;
    }

    /**
     * @param baseDir
     * @param applyModel
     */
    public void runFiveFoldCrossValidation(String baseDir, boolean applyModel, boolean writeModel) {
        System.out.println("Running five fold cross validation with annotation " + ((applyModel)? "off" : "on"));
        try {
            Problem problem;
            if(!applyModel) {
                problem = generateProblem(baseDir, writeModel);
            }
            else {
                problem = generateProblem(baseDir, model, writeModel);
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
            evaluate(target, ef.answers, ef.originalLines, true);
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
    public void learnFirstModel(String baseDir, String modelFileName) {
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
            Problem problem = generateProblem(baseDir, model, false);

            SolverType solver = SolverType.L2R_L2LOSS_SVC; // -s 0
            double C = 1.0;    // cost of constraints violation
            double eps = 0.01; // stopping criteria

            Parameter param = new Parameter(SolverType.L2R_L2LOSS_SVC, C, eps);

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


    public void applyModelToDocuments(String firstModelName, String secondModelName) {
        try {
            FeatureExtractor ef = new FeatureExtractor();
            File modelFile = new File(firstModelName);
            Model firstModel = Model.load(modelFile);

            File modelFile2 = new File(secondModelName);
            Model secondModel = Model.load(modelFile2);

//            String baseDir = "/Users/mhjang/Documents/teaching_documents/extracted/stemmed/parsed/gold/";
//            String baseDir = "/Users/mhjang/Desktop/clearnlp/allslides/";
            String baseDir = "/Users/mhjang/Documents/teaching_documents/extracted/dataset";
            String allParsingDir = baseDir + "parsed/";

            FeatureParameter.setModelSet(firstModel, secondModel);
            ef.prepareFeatureForDocuments(baseDir);

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
    public void evaluate(double[] predictions, double[] answers, String[] lines, boolean evaluateByComponent) {
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
                System.out.println("Wrong (" + TagConstant.getTagLabel(truth)+ "-> " + TagConstant.getTagLabel(prediction) +"= " + lines[i]);
            }
        }

        System.out.println("Total Accuracy: " + (double) correctItem / (double) predictions.length);
        System.out.println("Component: Precision, Recall");
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

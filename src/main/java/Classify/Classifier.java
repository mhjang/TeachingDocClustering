package Classify;

import java.io.File;
import java.io.IOException;

import de.bwaldvogel.liblinear.*;

/**
 * Created by mhjang on 5/18/14.
 */
public class Classifier {
    public static void main(String[] args) {
        Problem problem = new Problem();
        problem.l = 2; // number of training examples
        problem.n = 2; // number of features
        problem.x = new Feature[][]{{new FeatureNode(1, 4), new FeatureNode(2, 2)}, {new FeatureNode(1, 10), new FeatureNode(2, 5)}}; // feature nodes
        problem.y = new double[]{3.5, 4.0}; // target values

        SolverType solver = SolverType.L2R_LR; // -s 0
        double C = 1.0;    // cost of constraints violation
        double eps = 0.01; // stopping criteria

        Parameter parameter = new Parameter(solver, C, eps);
        Model model = Linear.train(problem, parameter);
        File modelFile = new File("model");
        try {
            model.save(modelFile);
            model = Model.load(modelFile);

        } catch (IOException e) {
            e.printStackTrace();
        }
// load model or use it directly

        Feature[] instance = { new FeatureNode(1, 4), new FeatureNode(2, 2) };
        double prediction = Linear.predict(model, instance);
    }

}

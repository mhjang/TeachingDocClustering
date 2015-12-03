package Classify.liblinear;

/**
 * Created by mhjang on 12/2/15.
 */
public class CommandLineRunner {
    public static void main(String[] args) {

        String applyDir = args[1];
        String filename = args[2];
        SVMClassifier svm = new SVMClassifier();
        svm.firstModel = args[3];
        svm.secondModel = args[4];
        svm.applyModelToDocument(applyDir, filename);

        // routine 3: apply the learned model to generate the noise-free version of documents
        //      svm.applyModelToDocuments(applyDir);

    }
}

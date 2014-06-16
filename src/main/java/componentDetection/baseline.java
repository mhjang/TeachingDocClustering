package componentDetection;

import simple.io.myungha.*;

import java.io.IOException;

/**
 * Created by mhjang on 6/8/14.
 */
public class baseline {
    public static void main(String[] args) throws IOException {
        DirectoryReader reader = new DirectoryReader("/Users/mhjang/Documents/teaching_documents/extracted/stemmed/parsed/feature_tokens");
        for(String filename : reader.getFileNameList()) {
            SimpleFileReader sr = new SimpleFileReader(filename);
            while(sr.hasMoreLines()) {
                String line = sr.readLine();
                DetectCodeComponent.isCodeLine(line);
          //      isLineEquation(line);
         //       isLineTable(line);


            }
        }
    }


}

package Classify.maxent;

import Classify.TagConstant;
import simple.io.myungha.SimpleFileWriter;

import java.io.IOException;
import java.util.LinkedList;

/**
 * Created by mhjang on 7/17/14.
 */
public class MaximumEntClassifier {

    public void convertToMaxentFormat(LinkedList<String> context, LinkedList<LinkedList<String>> features, boolean isTraining, String filename) throws IOException {
        SimpleFileWriter sw = new SimpleFileWriter(filename);
        SimpleFileWriter sw2 = null;
        if(!isTraining)
            sw2 = new SimpleFileWriter(filename.substring(0,filename.lastIndexOf('.')) + "_answers.txt");

        for(LinkedList<String> flist : features) {
            int i=0;
            for(i=0; i<flist.size()-1; i++) {
                sw.write(context.get(i) + ":" + flist.get(i) +" ");
                //             System.out.print(context.get(i) + ":" + flist.get(i) + " ");
            }
            if(isTraining) sw.write(TagConstant.getTagLabelByComponent(Integer.parseInt(flist.get(i))));
            else {
                sw.write("?");
                sw2.writeLine(TagConstant.getTagLabelByComponent(Integer.parseInt(flist.get(i))));
            }
            sw.write("\n");
            //          System.out.println();
        }
        sw.close();
        if(!isTraining) sw2.close();
    }

}

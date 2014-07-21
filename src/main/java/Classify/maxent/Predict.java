package Classify.maxent;///////////////////////////////////////////////////////////////////////////////
// Copyright (C) 2001 Chieu Hai Leong and Jason Baldridge
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//////////////////////////////////////////////////////////////////////////////   

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import Classify.TagConstant;
import opennlp.maxent.BasicContextGenerator;
import opennlp.maxent.ContextGenerator;
import opennlp.maxent.DataStream;
import opennlp.maxent.PlainTextByLineDataStream;
import opennlp.model.GenericModelReader;
import opennlp.model.MaxentModel;
import opennlp.model.RealValueFileEventStream;
import simple.io.myungha.SimpleFileReader;

/**
 * Test the model on some input.
 *
 * @author  Jason Baldridge
 * @version $Revision: 1.4 $, $Date: 2008/11/06 20:00:34 $
 */
public class Predict {
    MaxentModel _model;
    ContextGenerator _cg = new BasicContextGenerator();
    
    public Predict (MaxentModel m) {
	_model = m;
    }
    
    private void eval (String predicates) {
      eval(predicates,false);
    }
    
    private String eval (String predicates, boolean real) {
      String[] contexts = predicates.split(" ");
      double[] ocs;
      if (!real) {
        ocs = _model.eval(contexts);
      }
      else {
        float[] values = RealValueFileEventStream.parseContexts(contexts);
        ocs = _model.eval(contexts,values);

      }
//      System.out.println("For context: " + predicates+ "\n" + _model.getBestOutcome(ocs) + "\n");
       return _model.getBestOutcome(ocs);
	
    }
    
    private static void usage() {
      
    }

    /**
     * Main method. Call as follows:
     * <p>
     * java Predict dataFile (modelFile)
     */
    public static void main(String[] args) throws IOException {
        String dataFileName, modelFileName, truthFileName;
        boolean real = false;

        String type = "maxent";
        int ai = 0;
        for (int i = 0; i < 5; i++) {
            dataFileName = "/Users/mhjang/Documents/workspace/TeachingDocClustering/test_" + i + ".txt";
            modelFileName = "/Users/mhjang/Documents/workspace/TeachingDocClustering/training_" + i + "Model.txt";
            truthFileName = dataFileName.substring(0, dataFileName.lastIndexOf(".")) + "_answers.txt";
            SimpleFileReader sr = new SimpleFileReader(truthFileName);
            int[] correctCount = {0, 0, 0, 0, 0};
            int[] allCount = {0, 0, 0, 0, 0};

            ArrayList<String> truthData = new ArrayList<String>();
            String answer;
            while (sr.hasMoreLines()) {
                answer = sr.readLine();
                truthData.add(answer);
            }
            Predict predictor = null;
            try {
                MaxentModel m = new GenericModelReader(new File(modelFileName)).getModel();
                predictor = new Predict(m);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(0);
            }
            int k = 0, correct = 0, wrong = 0;
            try {
                DataStream ds =
                        new PlainTextByLineDataStream(
                                new FileReader(new File(dataFileName)));

                while (ds.hasNext()) {
                    String s = (String) ds.nextToken();
                    String prediction = predictor.eval(s.substring(0, s.lastIndexOf(' ')), real);
                    if (prediction.equals(truthData.get((k)))) {
                        correctCount[TagConstant.getComponentID(prediction)]++;
                    }
             //       System.out.println(prediction);
                    allCount[TagConstant.getComponentID(truthData.get(k++))]++;
                }
                for(int t=0; t<5; t++) {
                    System.out.println(TagConstant.getTagLabel(t) + ": " + (double)correctCount[t]/(double)allCount[t]);
                }
                System.out.println(correct + "\t" + wrong + "\t" + (double) (correct) / (double) (wrong + correct));
            } catch (Exception e) {
                System.out.println("Unable to read from specified file: " + modelFileName);
                System.out.println();
                e.printStackTrace();
            }

        }
    }
}

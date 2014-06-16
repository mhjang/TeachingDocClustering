package NoiseRemoval;
;

import componentDetection.DetectCodeComponent;

import java.io.*;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by mhjang on 5/10/14.
 */


public class NoiseRemoval {
    static Pattern numberPattern = Pattern.compile("^[a-zA-Z]*([0-9]+).*");
    static Pattern puctPattern = Pattern.compile("[\\p{Punct}‘]");

    public static void main(String[] args) throws IOException {
        NoiseRemoval nrm = new NoiseRemoval();
        nrm.decodeClassification();
   //     nrm.heuristics();
    }

    /**
     * let's try removing noises using heursitics
     */
    public void heuristics() throws IOException {
 //       File folder = new File("/Users/mhjang/Documents/SVMlight/Source/svm_light/result/");
        String docDir = "/Users/mhjang/Documents/teaching_documents/extracted/stemmed/parsed/feature_tokens/";
        String outputDir = "/Users/mhjang/Documents/teaching_documents/extracted/stemmed/parsed/noise_removed_h/";
        File folder = new File(docDir);

        DetectCodeComponent dcc = new DetectCodeComponent();
        for (final File fileEntry : folder.listFiles()) {
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputDir + fileEntry.getName()));
            int notNoisyTokens = 0, noiseTokens = 0;
            if (fileEntry.getName().contains(".DS_Store")) continue;
            System.out.print(fileEntry.getName() + "\t");
            BufferedReader br = new BufferedReader(new FileReader(fileEntry));
            String line;
            ArrayList<Boolean> noisyLabels = new ArrayList<Boolean>();
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (dcc.isCodeLine(line)) {
           //         System.out.println(line);
                    noiseTokens += line.split(" ").length;
                    continue;
                } // remove
                String[] tokens = line.split(" ");
                for (int i = 0; i < tokens.length; i++) {
                    tokens[i] = tokens[i].trim();
                    Matcher m1 = numberPattern.matcher(tokens[i]);
                    if (m1.find()) {
                 //       System.out.print(tokens[i] + " ");
                        noiseTokens++;
                        continue;
                    }
                    Matcher m2 = puctPattern.matcher(tokens[i]);
                    if (m2.find()) {
               //         System.out.print(tokens[i] + " ");
                        noiseTokens++;
                        continue;
                    }

                    if (tokens[i].length() == 1) {
               //         System.out.print(tokens[i] + " ");
                        noiseTokens++;
                        continue;
                    }
                    if (tokens[i].contains("ln") || tokens[i].contains("log") || tokens[i].contains("lg")) {
                        noiseTokens++;
                        continue;
                    }
                    bw.write(tokens[i] + " ");
                    notNoisyTokens++;
                }
                bw.write("\n");
            }
            bw.close();
            System.out.println(noiseTokens + "\t" + notNoisyTokens + "\t" + (double)(noiseTokens)/(double)(noiseTokens + notNoisyTokens));
        }
    }


    public void decodeClassification() {
        try {
            File folder = new File("/Users/mhjang/Documents/SVMlight/Source/svm_light/result/");
            String docDir = "/Users/mhjang/Documents/teaching_documents/extracted/stemmed/parsed/feature_tokens/";
            String outputDir = "/Users/mhjang/Documents/teaching_documents/extracted/stemmed/parsed/noise_removed/";
            for (final File fileEntry : folder.listFiles()) {
                int notNoisyTokens = 0, noisyTokens = 0;
                if(fileEntry.getName().contains(".DS_Store")) continue;
                System.out.print(fileEntry.getName() + "\t");
                BufferedReader br = new BufferedReader(new FileReader(fileEntry));
                String line;
                ArrayList<Boolean> noisyLabels = new ArrayList<Boolean>();
                while((line = br.readLine()) != null) {
                    double score = Double.parseDouble(line);
                    if(score > 0)
                        noisyLabels.add(Boolean.TRUE);
                    else
                        noisyLabels.add(Boolean.FALSE);
                }
                BufferedReader br2 = new BufferedReader(new FileReader(docDir + fileEntry.getName()));
                try {
                    BufferedWriter bw = new BufferedWriter(new FileWriter(outputDir + fileEntry.getName()));

                    while ((line = br2.readLine()) != null) {
                        String[] tokens = line.split(" ");
                        for (int i = 0; i < tokens.length; i++) {
                            if (!noisyLabels.get(i)) {
                                bw.write(tokens[i] + " ");
                                notNoisyTokens++;
                            }
                            else {
                            //    System.out.print(tokens[i] + " ");
                                noisyTokens++;
                            }
                        }
                        bw.write("\n");
                    //          System.out.println();
                    }
                    bw.close();
                    System.out.println((double)(noisyTokens) / (double)(notNoisyTokens + noisyTokens));
                    //        removedTokens = 0;

                }catch(FileNotFoundException fe) {
                    fe.printStackTrace();
                }


            }
        }catch (Exception e) {
            e.printStackTrace();
        }

    }



}

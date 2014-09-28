package NLP;

import simple.io.myungha.DirectoryReader;
import simple.io.myungha.SimpleFileReader;

import java.io.*;
import java.util.ArrayList;

/**
 * Created by mhjang on 9/24/14.
 */
public class EncodingCleanUp {
    public static void main(String[] args) {
        String baseDir = "/Users/mhjang/Desktop/clearnlp/dataset/acl/";
        String parsedDir = baseDir + "parsed/";
        String annotationDir = baseDir + "annotation/";
        String originalDir = baseDir + "text/";
        DirectoryReader dr = new DirectoryReader(annotationDir);

        try {
            for (String filename : dr.getFileNameList()) {
                ArrayList<String> annotatedLines = new ArrayList<String>();
                String l = "";
                System.out.println("reading " + annotationDir + filename);
                BufferedReader freader = new BufferedReader(new InputStreamReader(new FileInputStream(baseDir + filename + ".tagged"), "UTF8"));
                BufferedReader treader = new BufferedReader(new InputStreamReader(new FileInputStream(originalDir + filename), "UTF8"));
                //  BufferedReader freader = new BufferedReader(new FileReader(new File(annotationDir + filename)));
                //  BufferedReader treader = new BufferedReader(new FileReader(new File(originalDir + filename)));

                while ((l = freader.readLine()) != null) {
                    if (l.isEmpty() || l.trim().length() == 0) continue;
                    else annotatedLines.add(l.replace("<BR>", "").trim());
                }

                ArrayList<String> originalLines = new ArrayList<String>();
                while ((l = treader.readLine()) != null) {
                    if (l.isEmpty() || l.trim().length() == 0) continue;
                    else originalLines.add(l);
                }
                for (int i = 0; i < originalLines.size(); i++) {
                    int startLoc = 0, endLoc = 0;
                    String annotatedLine = annotatedLines.get(i);
                    String originalLine = originalLines.get(i);
                    if (annotatedLines.get(i).contains("<EQUATION>")) {
                        startLoc = annotatedLine.indexOf("<EQUATION>") - 1;
                        if (annotatedLines.get(i).contains("</EQUATION>")) {
                            endLoc = annotatedLine.indexOf("</EQUATION>") - 1;
                        } else
                            endLoc = annotatedLine.length() - 1;


                        annotatedLine = annotatedLine.replace(annotatedLine.substring(0, startLoc), originalLine.substring(0, startLoc));
                        annotatedLine = annotatedLine.replace(annotatedLine.substring(startLoc + 10, endLoc), originalLine.substring(startLoc + 1, endLoc- 10));
                        annotatedLine = annotatedLine.replace(annotatedLine.substring(endLoc + 11, annotatedLine.length()), originalLine.substring(endLoc - 9, originalLine.length()));

                //        System.out.println(annotatedLines.get(i));
                //        System.out.println(annotatedLine);

                    }


                }
            }



        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

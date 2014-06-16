package TeachingDocParser;

import org.lemurproject.galago.core.parse.stem.KrovetzStemmer;

import java.io.*;
import java.util.StringTokenizer;

/**
 * Created by mhjang on 2/2/14.
 * Just a few different interface methods for using KrovetzStemmer
 */
public class Stemmer {
    KrovetzStemmer stemmer;
    public Stemmer() {
        stemmer = new KrovetzStemmer();

    }

    public static void main(String[] args) {
        Stemmer stemmer = new Stemmer();
      //  stemmer.stemOneFileOutput("./topics_resource/topics_v2");
        stemmer.stemAllFilesOutput("/Users/mhjang/Documents/teaching_documents/extracted");
    }

    public void stemAllFilesOutput(String path) {
        File fileEntry = new File(path);
        File[] listOfFiles = fileEntry.listFiles();
        for(File file : listOfFiles) {
            if(file.isFile())
       //     if(file.isFile() && file.getName().endsWith(".html")) {
                stemOneFileOutput(file.getPath());
      //      }
        }
    }

    public String stemOneFileToString(String path) {
        StringBuilder sb = new StringBuilder();
        try {
            File fileEntry = new File(path);
            LineNumberReader reader = new LineNumberReader(new FileReader(fileEntry));
            String line;
            line = reader.readLine();
            while (line != null) {
                line = line.trim();
                StringTokenizer st = new StringTokenizer(line, " ,\n", false);
                while(st.hasMoreTokens()) {
                    String stem = stemmer.stem(st.nextToken());
                    sb.append(stem + " ");
                }
                sb.append("\n");
                line = reader.readLine();
            }

            }catch(Exception e) {
                 e.printStackTrace();
            }
        return sb.toString();

     }

    /**
     *
     * @param text
     * @return text
     * This method assembles the tokenized terms to a string so that it'll be tokenized by Tokenizer later.
     * I know it sounds dumb to put it back and split it later again, but for "modulization", I'll let one component do one job.
     */
    public String stemString(String text) {
        StringBuilder sb = new StringBuilder();
        StringTokenizer st = new StringTokenizer(text, " ,\n", false);
        while(st.hasMoreTokens()) {
            String token = st.nextToken();
            String stem = stemmer.stem(token);
            sb.append(stem + " ");
     //       System.out.println(token + "->" + stem);
        }
        sb.append("\n");
   //     System.out.println();
        return sb.toString();
    }

    public void stemOneFileOutput(String path) {
        try {
            File fileEntry = new File(path);
            LineNumberReader reader = new LineNumberReader(new FileReader(fileEntry));
            String pathPreFix = path.substring(0, path.lastIndexOf('/'));
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(pathPreFix + "/stemmed_annotated/"+fileEntry.getName())));
           // BufferedWriter bw = new BufferedWriter(new FileWriter(new File(pathPreFix +"/" +fileEntry.getName() + "_stemmed")));
            System.out.println(pathPreFix +"/" + fileEntry.getName() + "_stemmed");

            String line;
            try {
                line = reader.readLine();
                while (line != null) {
                    line = line.trim();
                    StringTokenizer st = new StringTokenizer(line, " ,\n", false);
                    // for annotation purpose
                    bw.write("R\t");
                    while(st.hasMoreTokens()) {
                        String stem = stemmer.stem(st.nextToken());
                        bw.write(stem + " ");
                    }
                    bw.write("\n");
                    bw.flush();
                    line = reader.readLine();
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
            bw.close();
        }catch(Exception e) {
            e.printStackTrace();
     }
    }
}


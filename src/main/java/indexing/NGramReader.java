package indexing;


import org.lemurproject.galago.core.btree.simple.DiskMapBuilder;
import org.lemurproject.galago.core.btree.simple.DiskMapReader;
import org.lemurproject.galago.core.btree.simple.DiskMapSortedBuilder;
import org.lemurproject.galago.tupleflow.Utility;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;


/**
 * Created by mhjang on 3/2/14.
 */
public class NGramReader {

    DiskMapReader unigramReader;
    DiskMapReader bigramReader;
    DiskMapReader trigramReader;

    File unigramFile = new File("/Users/mhjang/Documents/workspace/TeachingTest/ngram_index/1gms0");
    File bigramFile = new File("/Users/mhjang/Documents/workspace/TeachingTest/ngram_index/2gms");
    File trigramFile = new File("/Users/mhjang/Documents/workspace/TeachingTest/ngram_index/3gms_1");

    BigInteger unigramTotalCount = BigInteger.valueOf(1024908267);
    BigInteger bigramTotalCount = BigInteger.valueOf(1024908267);
    BigInteger trigramTotalCount = BigInteger.valueOf(1024908267);

    public NGramReader() throws IOException {
        System.out.println("Google N-gram reader initiated :) ");
        unigramReader = new DiskMapReader(unigramFile.getAbsolutePath());
        bigramReader = new DiskMapReader(bigramFile.getAbsolutePath());
        trigramReader = new DiskMapReader(trigramFile.getAbsolutePath());
   //     setTotalCount();
    }


    public double termCollectionProbability(String term) {
        String[] tokens = term.split(" ");
        byte[] data = null;
        BigInteger total = null;
        if(tokens.length == 1) {
            data = unigramReader.get(Utility.fromString(term));
            total = this.unigramTotalCount;
        }
        else if(tokens.length == 2) {
            data = bigramReader.get(Utility.fromString(term));
            total = this.bigramTotalCount;
        }
        else if(tokens.length == 3) {
            data = trigramReader.get(Utility.fromString(term));
            total = this.trigramTotalCount;
        }
        if(data != null) {
            String count = Utility.toString(data);
            double v =  total.doubleValue() / (Double.parseDouble(count));
            return v;
        }
        else {
           return 0.001;
        }
    }
    public int lookUpTerm(String term) {
        String[] tokens = term.split(" ");
        byte[] data;
        if(tokens.length == 1) {
            data = unigramReader.get(Utility.fromString(term));
        }
        else if(tokens.length == 2)
            data = bigramReader.get(Utility.fromString(term));
        else if(tokens.length == 3)
            data = trigramReader.get(Utility.fromString(term));
        else
        {
           System.out.println("NGramReader only supports upto trigram. This is " + tokens.length + "gram.");
           return -1;
        }
        if(data != null) {
            String count = Utility.toString(data);
            return Integer.parseInt(count);
        }
        else {
            return 0;
        }

    }

    private void setTotalCount() {
        System.out.println("here to count");
     /*   for(byte[] key: unigramReader.keySet()) {
            byte[] data = unigramReader.get(key);
            unigramTotalCount = unigramTotalCount.add(new BigInteger(Utility.toString(data)));
        }
      */
        System.out.print("1gram: " + unigramTotalCount);
        for(byte[] key: bigramReader.keySet()) {
            byte[] data = bigramReader.get(key);
            bigramTotalCount = bigramTotalCount.add(new BigInteger(Utility.toString(data)));
        }
        System.out.print("bigram: " + bigramTotalCount);

        for(byte[] key: trigramReader.keySet()) {
            byte[] data = trigramReader.get(key);
            trigramTotalCount = trigramTotalCount.add(new BigInteger(Utility.toString(data)));
        }
        System.out.print("trigram: " + trigramTotalCount);

    }
    public static void main(String[] args) throws IOException {
  //      NGramReader ng = new NGramReader();
  //      ng.getTotalCount();
     // BufferedReader br = new BufferedReader(new FileReader(new File("/Users/mhjang/3gms/extracted/3gm-0000")));
    //    HashMap<byte[], byte[]> data = new HashMap<byte[], byte[]>();

        /*****
         * building a B-tree index for 3-gram data
         ******/

     /*   HashMap map = new HashMap<byte[], byte[]>();
        DiskMapSortedBuilder dmb = new DiskMapSortedBuilder("ngram_index/1gms0");
        String fileName = "/Users/mhjang/1gms/vocab_cs";
        for(int i=0; i<=0; i++) {
            String fileIndex = "";
            fileIndex = Integer.toString(i);
            BufferedReader br = new BufferedReader(new FileReader(new File(fileName + fileIndex)));

            String line = null;

            while((line = br.readLine()) != null) {
                try{
                    StringTokenizer st = new StringTokenizer(line, "\t");
                    String word = st.nextToken();
                    String freq = st.nextToken();
                    byte[] ngram = Utility.fromString(word);
                    byte[] frequency = Utility.fromString(freq);
                    map.put(ngram, frequency);
              //      dmb.put(ngram, frequency);
                }catch(NumberFormatException e) {
                    System.out.println(line);
                    e.printStackTrace();

                }
            }
        }
        DiskMapReader dmr = DiskMapReader.fromMap("ngram_index/1gms0", map);

      //  dmb.close();

        /***
         * loading the index file
         */
    //    File file = new File("ngram_index/1gms");

        /**
         * Build an on-disk map using galago
         */
   /*     DiskMapReader mapReader = new DiskMapReader(file.getAbsolutePath());
        System.out.println(Runtime.getRuntime().maxMemory() / (1024 * 1024 * 1024));
        /*
         * pull keys
         */
  /*      byte[] data = mapReader.get(Utility.fromString("dynamic"));
        if(data != null) {
            String count = Utility.toString(data);
            System.out.println("count: "+ count);
        }

        // java using object equality is dumb
        // assertTrue(memKeys.contains(key)) will fail because it does pointer comparisons...
*/
    }

}


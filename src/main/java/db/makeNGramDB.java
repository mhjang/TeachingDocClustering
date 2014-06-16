package db;

import java.io.*;
import java.sql.PreparedStatement;
import java.util.StringTokenizer;

/**
 * Created by mhjang on 2/23/14.
 */
public class makeNGramDB {
    public static void main(String[] args) throws FileNotFoundException {
        String dir = "/Users/mhjang/3gms/extracted/";
        File folder = new File(dir);
        String query = "INSERT INTO 3gmts VALUES " + "(?,?)";
        DBConnector db = new DBConnector("google_ngrams");
        int n = 3;
        String tablename = "3gms";
        int count = 0;

        PrintStream console = System.out;
        File file = new File("log.txt");
        FileOutputStream fos = new FileOutputStream(file);
        PrintStream ps = new PrintStream(fos);
        System.setOut(ps);

        for (final File fileEntry : folder.listFiles()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(fileEntry));
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();
                while (line != null) {
                    StringTokenizer st = new StringTokenizer(line);
                    String term = "";
                    for(int i=0; i<n; i++) {
                        if(i!=n-1)
                            term += st.nextToken() + " ";
                        else
                            term += st.nextToken();
                    }
                    int occurrence = Integer.parseInt(st.nextToken());
                    term = term.replace('"', '\'');
                    PreparedStatement pst = db.getPreparedStatment(query);
                    pst.setString(1, term);
                    pst.setInt(2, occurrence);
                    pst.executeUpdate();
                    count++;
                    line = br.readLine();
                }
                System.out.print(count + " terms are inserted from " + fileEntry.getName());
                db.closeConnection();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
}

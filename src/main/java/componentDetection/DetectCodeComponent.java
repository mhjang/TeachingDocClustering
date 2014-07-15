package componentDetection;

import java.io.*;
import java.util.StringTokenizer;

/** 
 * 
 * @author Myung-ha Jang 
 * To remove code component from the text, it considers below code features. 
 * 
 * 1. SemiColon :  Semi-colons at the end of a line. This alone would catch a whole bunch of languages.
   2. Parenthesis: Parentheses directly following text with no space to separate it: myFunc()
   3. MemberAccess: A dot or arrow between two words: foo.bar = ptr->val
   4. Bracket: Presence of curly braces, brackets: while (true) { bar[i]; }, nested parentheses, braces, and/or brackets
   5. Comment: Presence of "comment" syntax (/*, //, etc): /* multi-line comment 
   6. Operator: Uncommon characters/operators: +, *, &, &&, |, ||, <, >, ==, !=, >=, <=, >>, <<, ::, __
   7. camelCase: camelCase text in the post.
 * 
 */

public class DetectCodeComponent {
	public static void main(String[] args) throws Exception {
        // the original directory that contains files whose codes shall be removed
		String dir = "/Users/mhjang/Documents/teaching_documents/extracted/stemmed/";
        // the new directory that saves files after removing the code lines
		String cDir = "/Users/mhjang/Documents/teaching_documents/coderm/";
		String line;
		File folder = new File(dir);
        int count = 0;
		for (final File fileEntry : folder.listFiles()) {
			 BufferedReader br = new BufferedReader(new FileReader(fileEntry));
			 BufferedWriter bw = new BufferedWriter(new FileWriter(cDir+fileEntry.getName().toLowerCase()));
			 try {
			        line = br.readLine();
			        while(line != null) {
			        	if(line.length() > 0) { 
			        		line = line.toLowerCase().trim();
			        		if(!isSemicolon(line) && !isParenthesis(line) && !isBracket(line) && !isComment(line) && !isOperator(line) && !isVariable(line) && !isCopyright(line))
			        			bw.write(line + "\n");
                            else
                                count++;
			        	}
			        	line = br.readLine();
			        }
			 bw.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
		}
        System.out.println(count + " lines of codes are removed!");
	}

    // return true if at least one of the token in the line has a true property of one of the rules
    public static boolean isCodeLine(String line) {
        String[] tokens = line.split(" ");
        for(int i=0; i<tokens.length; i++) {
            if(isSemicolon(tokens[i]) || isParenthesis(tokens[i]) || isBracket(tokens[i]) || isComment(tokens[i]) || isOperator(tokens[i]) || isVariable(tokens[i]))
                return true;

        }
        return false;

    }
	/**
	 * This is pretty heuristic, but this copyright statements are pretty ubiquitous in teaching documents, contributing to a fair amount of noises
	 * @param line
	 * @return
	 */
	 public static boolean isCopyright(String line) {
		if(line.contains("©") || line.contains("rights reserved")) {
	//		System.out.println("copyright: " + line);
			return true;
		}
		StringTokenizer st = new StringTokenizer(line);
		while(st.hasMoreTokens()) {
			String token = st.nextToken();
			if(token.matches("[\\w-]+\\@([\\w-]+\\.)+[\\w-]+")) {
	//			System.out.println("email: " + line);
				return true; 
			}
		}
		return false;
		
	}


    public static boolean isSemicolon(String token) {
        if(token.isEmpty()) return false;
        char lastChar = token.charAt(token.length()-1);
		if(lastChar == ';') {
			return true;
		}
		else return false; 
	}

    public static boolean isParenthesis(String token) {
		if(token.matches("\\w*\\[\\w*\\]")) {
				return true;
			}
		return false;
	}

    public static boolean isBracket(String token) {
        if(token.matches("\\w*\\(\\)")) {
            return true;
        }
		 return false; 
	}

    public static boolean isComment(String token) {
        if (token.contains("//") || token.contains("/*") || token.contains("*/")) {
                return true;
        }
        return false;
	}

    public static boolean isOperator(String line) {
		String[] operators = {"+", "&&", "||", "<", ">", "==", "!=", ">=", "<=", ">>", "<<", "::", "__", "</"};
		StringTokenizer st = new StringTokenizer(line);
		while(st.hasMoreTokens()) {
			String token = st.nextToken();
			for(int i=0; i<operators.length; i++) {
				if(token.contains(operators[i])) {
	//				System.out.println("operator: " + line);
					return true;
				}
			}
		}
		return false; 
	}

    public static boolean isVariable(String token) {
            // check if the variable is a camal case
			if(token.matches("[a-z]+[A-Z][a-z]+")) {
				return true;
			}
            // or two strings concatenated by an underscore
			else if(token.matches("\\w+\\_\\w+")) {
				return true;
			}
		
			return false;
	}
}

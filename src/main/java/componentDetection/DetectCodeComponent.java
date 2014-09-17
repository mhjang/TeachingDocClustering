package componentDetection;

import java.io.*;
import java.math.BigDecimal;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

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
   8. keywordContain: how many keywords the given sentence contains.
 * 
 */

public class DetectCodeComponent {

    static Pattern copyrightPattern = Pattern.compile("[\\w-]+\\@([\\w-]+\\.)+[\\w-]+");
    static Pattern parenthesisPattern = Pattern.compile("\\w*\\[\\w*\\]");
    static Pattern bracketPattern =  Pattern.compile("\\w*\\(\\)");
    static Pattern camalCasePattern = Pattern.compile("[a-z]+[A-Z][a-z]+");
    static Pattern underscorePattern = Pattern.compile("\\w+\\_\\w+");

    static String[] reservedKeywords = {"abstract", "assert", "boolean", "break", "byte", "catch",
    "char", "class", "const", "continue", "default", "double", "enum", "extends", "final",
    "finally", "float", "goto", "implements", "import", "instanceof", "int", "interface",
    "native", "package", "private", "protected", "public", "return", "short", "static", "super", "switch", "synchronized",
    "throw", "throws", "transient", "void", "volatile", "false", "null", "true"};



    public static void main(String[] args) throws Exception {
        String testline = "template < typename type > \n" +
                "class ; \n" +
                "the \" : public < type > that this is derive from the \n" +
                "class . every ' binary search node ' is a ' binary node ' . \n" +
                "template < typename type > \n" +
                "class node:public < type > { \n" +
                "binary_node < type > : : element ; \n" +
                "use binary_node < type > : : left_tree ; \n" +
                "use binary_node < type > : : right_tree ; \n" +
                "public : \n" +
                "( type const & \n" +
                "*left const ; \n" +
                "*right const ; \n" +
                "bool empty const ; \n" +
                "size const \n" +
                "height const \n" +
                "bool leaf ( const ; \n" +
                "type front ( const ; \n" +
                "type back const ; \n" +
                "find ( const type & const \n" +
                "void clear ( \n" +
                "insert ( \n" +
                "erase ( type const & binary_search_node * & \n" +
                "< type > ; \n" +
                "} ; \n" +
                "this is derive from the class it the \n" +
                "type retrieve ( const ; \n" +
                "*left const ; \n" +
                "*right const ; ";
        String[] lines = testline.split("\n");
        for(int i=0; i<lines.length; i++) {
            System.out.println(lines[i] + ": " + codeLineEvidence(lines[i]) + ":" + keywordContainSize(lines[i]));
        }
   /*     BigDecimal big = new BigDecimal("1.45");
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
        */
	}

    public static int keywordContainSize(String line) {
        int size = 0;
        String[] tokens = line.split(" ");
        for (int i = 0; i < tokens.length; i++) {
            if(isThisKeyword(tokens[i])) size++;
        }
        return size;
    }

    public static boolean isThisKeyword(String token) {
        int size = 0;
        for (int i = 0; i < reservedKeywords.length; i++) {
             if (token.equals(reservedKeywords[i])) {
                 return true;
             }
        }
        return false;
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


    // return true if at least one of the token in the line has a true property of one of the rules
    public static int codeLineEvidence(String line) {
        String[] tokens = line.split(" ");
        int caseMatch = 0;
        for(int i=0; i<tokens.length; i++) {
            if(isSemicolon(tokens[i]) || isParenthesis(tokens[i]) || isBracket(tokens[i]) || isComment(tokens[i]) || isOperator(tokens[i]) || isVariable(tokens[i]))
                caseMatch++;
        }
        return caseMatch;

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
        if(copyrightPattern.matcher(line).find()) return true;
         else return false;
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
		if(parenthesisPattern.matcher(token).find()) {
				return true;
			}
		return false;
	}

    public static boolean isBracket(String token) {
        if(bracketPattern.matcher(token).find()) {
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
		for(int i=0; i<operators.length; i++) {
            if(line.contains(operators[i]))
                return true;
        }
        return false;
	}

    public static boolean isVariable(String token) {
            // check if the variable is a camal case
            if(camalCasePattern.matcher(token).find())
				return true;
			// or two strings concatenated by an underscore
			else if(underscorePattern.matcher(token).find())
                return true;
			return false;
	}

    public static boolean isPointer(String token) {
        if(token.length() > 1 && token.charAt(0) == '*') return true;
        return false;
    }
}

package org.sqsh.analyzers;

import org.sqsh.SQLAnalyzer;
import org.sqsh.SimpleKeywordTokenizer;

/**
 * This class attempts to do very simplistic parsing of PL/SQL syntax
 * to determine if a terminator validly indicates a "go".
 */
public class PLSQLAnalyzer 
    implements SQLAnalyzer {

    /* (non-Javadoc)
     * @see org.sqsh.SQLAnalyzer#isTerminated(java.lang.String, char)
     */
    public boolean isTerminated (String sql, char terminator) {
        
        /*
         * If the terminator isn't a semicolon, then we just want to check
         * to see if the terminator exists at the very end of the line.
         */
        if (terminator != ';') {
            
            return isLastToken(sql, terminator);
        }
        
        /*
         * If the terminator is a semicolon then we have to look for
         * specific keywords in the SQL to try to determine if the
         * SQL is PL/SQL or just a plain SQL statement.
         */
        boolean isPLSQL = false;
        int semicolonCount = 0;
        SimpleKeywordTokenizer tokenizer =
            new SimpleKeywordTokenizer(sql, terminator);
        String token = tokenizer.next();
        while (token != null) {
            
            /*
             * We count semicolons until we hit a non-semicolon token.
             */
            if (token.length() == 1 && token.charAt(0) == terminator) {
                
                ++semicolonCount;
            }
            else {
                
                semicolonCount = 0;
            }
            
            /*
             * If we haven't yet determined if we are in a PL/SQL block
             * then start looking at the keywords.
             */
            if (isPLSQL == false) {
                
                if ("BEGIN".equals(token)) {
            
                    isPLSQL = isBeginBlock(tokenizer);
                }
                else if ("END".equals(token)
                        || "DECLARE".equals(token)
                        || "EXCEPTION".equals(token)) {
                    
                    isPLSQL = true;
                }
                else if ("CREATE".equals(token)) {
                    
                    isPLSQL = isCreateObject(tokenizer);
                }
            }
            
            token =  tokenizer.next();
        }
        
        /*
         * If we haven't seen a semicolon, then we can bail.
         */
        if (semicolonCount == 0) {
            
            return false;
        }
        
        /*
         * If we determined that this is not a PL/SQL block, then 
         * if we end in a semicolon we are a-ok. If this is a PL/SQL
         * block and we have multiple semicolons, then we are done as well.
         */
        if (isPLSQL == false || semicolonCount > 1) {
            
            return true;
        }
        
        /*
         * Last check. if this is a PL/SQL block and we have only one
         * semicolon, then check to see if it is on a line all by itself.
         */
        int idx = sql.length() - 1;
        char ch = sql.charAt(idx);
        
        /*
         * Find the semicolon.
         */
        while (ch != ';') {
            
            --idx;
            ch = sql.charAt(idx);
        }
        
        /*
         * Look back from the semicolon for a newline. If we hit
         * anything other than whitespace or the newline, then we aren't
         * terminated.
         */
        --idx;
        while (idx >= 0) {
            
            ch = sql.charAt(idx);
            if (Character.isWhitespace(ch)) {
                
                if (ch == '\n') {
                    
                    return true;
                }
            }
            else {
                
                break;
            }
        }
        
        return false;
    }
    
    /**
     * Called after a BEGIN is hit. Checks to see if it is a 
     * PL/SQL block or a BEGIN TRANSACTION.
     * 
     * @param tokenizer The tokenizer.
     * @return true if it is a standalone BEGIN.
     */
    private boolean isBeginBlock(SimpleKeywordTokenizer tokenizer) {
        
        String n = tokenizer.next();
        if (n != null) {
            
            if ("TRANSACTION".equals(n)
                    || "TRAN".equals(n)) {
                
                return false;
            }
            
            tokenizer.unget(n);
            return true;
        }
        
        return true;
    }
    
    /**
     * Called after a CREATE is hit. Checks to see if it is 
     * creating an object that would be a PL/SQL block.
     * 
     * @param tokenizer The tokenizer.
     * @return true if it is a PL/SQL object.
     */
    private boolean isCreateObject(SimpleKeywordTokenizer tokenizer) {
        
        String n = tokenizer.next();
        if (n != null && "OR".equals(n)) {
            
            n = tokenizer.next();
            if (n != null && "REPLACE".equals(n)) {
                
                n =  tokenizer.next();
            }
            else {
                
                if (n != null) {
                    
                    tokenizer.unget(n);
                }
                return false;
            }
        }
        
        if (n != null) {
            
            if ("TYPE".equals(n)
                    || "TRIGGER".equals(n)
                    || "CONTEXT".equals(n)
                    || "FUNCTION".equals(n)
                    || "PACKAGE".equals(n)
                    || "PROCEDURE".equals(n)) {
                
                return true;
            }
        }
        
        return false;
    }
    
    private boolean isLastToken(String sql, char terminator) {
        
        SimpleKeywordTokenizer tokenizer =
            new SimpleKeywordTokenizer(sql, terminator);
        String token = tokenizer.next();
        String prevToken = null;
        
        while (token != null) {
            
            prevToken = token;
            token = tokenizer.next();
        }
        
        if (prevToken != null 
                && prevToken.length() == 1 
                && prevToken.charAt(0) == terminator) {
            
            return true;
        }
        
        return false;
    }
}

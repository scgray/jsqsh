/*
 * Copyright 2007-2012 Scott C. Gray
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sqsh.analyzers;

import org.sqsh.SimpleKeywordTokenizer;

/**
 * This class attempts to do very simplistic parsing of PL/SQL syntax
 * to determine if a terminator validly indicates a "go".
 */
public class PLSQLAnalyzer 
    implements SQLAnalyzer {
    
    @Override
    public String getName() {

        return "PL/SQL";
    }

    /* (non-Javadoc)
     * @see org.sqsh.LanguageAnalyzer#isTerminated(java.lang.String, char)
     */
    public boolean isTerminated (String sql, char terminator) {
        
        /*
         * If the terminator isn't a semicolon, then we just want to check
         * to see if the terminator exists at the very end of the line.
         */
        if (terminator != ';') {
            
            return isLastToken(sql, terminator);
        }
        
        SimpleKeywordTokenizer tokenizer =
            new SimpleKeywordTokenizer(sql, terminator);
        String token = tokenizer.next();
        
        if (token == null) {
            
            return false;
        }
        
        int blockNestCount = 0;
        
        /*
         * First, attempt to classify the statement as whether or not it is
         * a PL/SQL statement that could contain semicolons. If it is, then
         * we enter the hard core parsing efforts.
         */
        if ("DECLARE".equals(token)) {
                    
            if (! seekBegin(tokenizer)) {
                            
                return false;
            }
            
            ++blockNestCount;
            token = tokenizer.next();
        }
        else if ("BEGIN".equals(token)) {
                    
            if (isBeginBlock(tokenizer)) {
                        
                ++blockNestCount;
            }
            token = tokenizer.next();
        }
        
        while (token != null) {
            
            /*
             * I don't check for CREATE PROCEDURE (or FUNCTION) as a top level
             * element because we could run across a construct like:
             *    CREATE SCHEMA FOO
             *       CREATE PROCEDURE ...
             *       CREATE PROCEDURE ...
             * However, we know we can't have a CREATE inside of a CREATE so
             * this check is only done if we aren't already in a block
             */
            if (blockNestCount == 0) {
                
                /*
                 * Did we hit the terminator? Note we can get tripped up PL/SQL's
                 * use of "END;" at the end of a DDL statement because they may
                 * have done:
                 * 
                 *    CREATE SCHEMA FOO
                 *       CREATE PROCEDURE ...BEGIN ... END;
                 *       CREATE PROCEDURE ...BEGIN ... END;
                 *       
                 * and we'll trigger on the first CREATE
                 */
                if (token.length() == 1 && token.charAt(0) == terminator) {
                    
                    if (tokenizer.peek() == null) {
                        
                        return true;
                    }
                }
                else if ("CREATE".equals(token)) {
                    
                    if (skipToRoutineBegin(tokenizer)) {
                        
                        ++blockNestCount;
                    }
                }
                else if ("ALTER".equals(token)) {
                    
                    /*
                     * Look for ALTER MODULE ... ADD/PUBLISH [PROC|FUNC]
                     */
                    token = tokenizer.next();
                    if ("MODULE".equals(token)) {
                        
                        token = tokenizer.next();
                        while (token != null 
                            && ! "ADD".equals(token)
                            && ! "DROP".equals(token)
                            && ! "PUBLISH".equals(token)) {
                            
                            token = tokenizer.next();
                        }
                        
                        if (skipToRoutineBegin(tokenizer)) {
                            
                            ++blockNestCount;
                        }
                    }
                }
            }
            else { // We are in a nested block
                
                /*
                 * CASE has an END and we don't want to interpret it incorrectly
                 */
                if ("CASE".equals(token)) {
                    
                    skipCase(tokenizer);
                }
                else if ("BEGIN".equals(token)) {
                    
                    if (isBeginBlock(tokenizer)) {
                        
                        ++blockNestCount;
                    }
                }
                else if ("END".equals(token)) {
                    
                    if (isEndBlock(tokenizer)) {
                        
                        --blockNestCount;
                    }
                }
            }
            
            token = tokenizer.next();
        }
        
        return false;
    }
    
    /**
     * Called after a CASE keyword has been hit. Skips to the END portion
     * of the CASE
     * 
     * @param tokenizer tokenizer
     */
    private void skipCase (SimpleKeywordTokenizer tokenizer) {
        
        int nestCount = 1;
        String token = tokenizer.next();
        while (token != null && nestCount > 0) {
            
            if ("CASE".equals(token)) {
                
                ++nestCount;
            }
            else if ("END".equals(token)) {
                
                token = tokenizer.next();
                if (! "CASE".equals(token)) {
                    
                    tokenizer.unget(token);
                }
                
                --nestCount;
            }
            else {
                
                token = tokenizer.next();
            }
        }
    }
    
    /**
     * Seek the tokenzer to the start of a BEGIN block
     * @param tokenizer The tokenizer
     * @return True if it was seeked to the BEGIN keyword
     */
    private boolean seekBegin(SimpleKeywordTokenizer tokenizer) {
        
        String tok = tokenizer.next();
        while (tok != null) {
            
            if ("BEGIN".equals(tok)) {
                
                if (isBeginBlock(tokenizer)) {
                    
                    return true;
                }
            }
            
            tok = tokenizer.next();
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
        
        String n = tokenizer.peek();
        
        if ("TRANSACTION".equals(n) || "TRAN".equals(n)) {
                
            return false;
        }
        
        return true;
    }
    
    /**
     * Called after a END is hit. Checks to ensure it isn't END IF
     * 
     * @param tokenizer The tokenizer.
     * @return true if it is a standalone BEGIN.
     */
    private boolean isEndBlock(SimpleKeywordTokenizer tokenizer) {
        
        String n = tokenizer.peek();
        
        /*
         * END IF, END DECLARE
         */
        if ("IF".equals(n) || "DECLARE".equals(n)) {
                
            return false;
        }
            
        return true;
    }
    
    /**
     * Called when some kind of CREATE has been hit to seek forward to 
     * try to find the beginning of the routine body.
     * 
     * @param tokenizer The tokenizer.
     * @return true if we hit the BEGIN token
     */
    private boolean skipToRoutineBegin (SimpleKeywordTokenizer tokenizer) {
        
        String tok = tokenizer.next();
        
        /*
         * Note: my funny style of doing "STRING".equals() is because
         * token could be null and this won't produce an NPE
         */
        if ("OR".equals(tok)) {
            
            tok = tokenizer.next();
            if ("REPLACE".equals(tok)) {
                
                tok = tokenizer.next();
            }
        }
        
        /*
         * A function can be: CREATE FUNCTION ... RETURN <expression>
         * or: CREATE FUNCTION .. BEGIN .. END. Try to figure out which
         */
        if ("FUNCTION".equals(tok)) {
            
            tok = tokenizer.next();
            while (tok != null) {
                
                if ("RETURN".equals(tok)) {
                    
                    return false;
                }
                else if ("BEGIN".equals(tok)) {
                    
                    return true;
                }
                
                tok = tokenizer.next();
            }
        }
        else if ("PROCEDURE".equals(tok) || "TRIGGER".equals(tok)) {
            
            /*
             * Procedures must have a BEGIN
             */
            if (seekBegin(tokenizer)) {
                
                return true;
            }
            
            return false;
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

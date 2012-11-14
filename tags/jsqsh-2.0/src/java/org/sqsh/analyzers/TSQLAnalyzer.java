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
 * Used to analyze Sybase and Microsoft Transact-SQL.
 */
public class TSQLAnalyzer
    implements SQLAnalyzer {

    /**
     * Analyzes a chunk of T-SQL to determine if the provided terminator
     * character is located at the end of the block. Unlike PL/SQL it
     * nearly impossible to determine when the body of a stored procedure
     * or trigger is complete, so it is pretty vital that the chosen
     * terminator character is not part of the language syntax.
     */
    public boolean isTerminated (String sql, char terminator) {
        
        SimpleKeywordTokenizer tokenizer =
            new SimpleKeywordTokenizer(sql, terminator);
        
        String prevToken = null;
        String token = tokenizer.next();
        int blockCount = 0;
        
        while (token != null) {
            
            /*
             * All blocks of SQL are denoted by BEGIN/END.
             */
            if ("BEGIN".equals(token)) {
                
                String nextToken = tokenizer.next();
                if (nextToken != null 
                        && (("TRAN".equals(nextToken)
                                    || "TRANSACTION".equals(nextToken))
                              || "DISTRIBUTED".equals(nextToken))) {
                    
                    tokenizer.unget(nextToken);
                }
                else {
                    
                    ++blockCount;
                }
            }
            else if ("END".equals(token)) {
                
                --blockCount;
            }
            
            prevToken = token;
            token = tokenizer.next();
        }
        
        return (prevToken != null 
                    && prevToken.length() == 1 
                    && prevToken.charAt(0) == terminator);
    }
}

/*
 * Copyright (C) 2007 by Scott C. Gray
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, write to the Free Software Foundation, 675 Mass Ave,
 * Cambridge, MA 02139, USA.
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

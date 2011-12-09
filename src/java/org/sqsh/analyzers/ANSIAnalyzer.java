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
 * A rather generic analyzer that is used to analyze ANSI sql statements.
 * This is used as the default analyzer when no other is available or the
 * necessary type of analyzer is not known.
 */
public class ANSIAnalyzer
    implements SQLAnalyzer {

    /**
     * Analyzes a chunk of ANSI SQL to determine if the provided terminator
     * character is located at the end of the block. This analyzer only
     * ensures that the terminator is not located within a string,
     * a variable name, or an object name.
     */
    public boolean isTerminated (String sql, char terminator) {
        
        SimpleKeywordTokenizer tokenizer =
            new SimpleKeywordTokenizer(sql, terminator);
        
        String prevToken = null;
        String token = tokenizer.next();
        
        while (token != null) {
            
            prevToken = token;
            token = tokenizer.next();
        }
        
        return (prevToken != null 
                    && prevToken.length() == 1 
                    && prevToken.charAt(0) == terminator);
    }
}

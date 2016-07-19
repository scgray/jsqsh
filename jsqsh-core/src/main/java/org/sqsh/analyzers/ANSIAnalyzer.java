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
 * A rather generic analyzer that is used to analyze ANSI sql statements.
 * This is used as the default analyzer when no other is available or the
 * necessary type of analyzer is not known.
 */
public class ANSIAnalyzer
    implements SQLAnalyzer {
    
    @Override
    public String getName() {

        return "ANSI SQL";
    }

    /**
     * Analyzes a chunk of ANSI SQL to determine if the provided terminator
     * character is located at the end of the block. This analyzer only
     * ensures that the terminator is not located within a string,
     * a variable name, or an object name.
     */
    @Override
    public boolean isTerminated (CharSequence sql, char terminator) {
        
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

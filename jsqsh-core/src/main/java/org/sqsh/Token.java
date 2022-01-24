/*
 * Copyright 2007-2022 Scott C. Gray
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
package org.sqsh;

/**
 * Represents an tokenized of a command line. A token can be a string
 * literal.
 */
abstract class Token {
    
    private int position = -1;
    private String line = null;
    
    /**
     * Noarg constructor.
     */
    public Token() {
        
    }
    
    /**
     * Create a token.
     * 
     * @param line Line from which the token came.
     * @param position Position on the line the taken began at.
     */
    public Token (String line, int position) {
        
        this.line = line;
        this.position = position;
    }
    
    /**
     * The line from which the token came.
     * @return The line from which the token came.
     */
    public String getLine () {
    
        return line;
    }
    
    /**
     * The position in the line at which the token began.
     * @return position in the line at which the token began.
     */
    public int getPosition () {
    
        return position;
    }
    
    /**
     * Every token must be able to convert itself back into a string.
     * 
     * @return The string version of the token 
     */
    public abstract String toString();
}

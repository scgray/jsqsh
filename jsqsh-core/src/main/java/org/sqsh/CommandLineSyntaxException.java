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
 * Reprsents an exception encountered while parsing a command line
 */
public class CommandLineSyntaxException
    extends Exception {
    
    /*
     * The number of characters into the command line that the problem
     * was encountered.
     */
    private int position;
    
    /*
     * The line that was being parsed.
     */
    private String line;

    /**
     * Creates a new exception.
     * 
     * @param msg The syntax error that was encountered.
     * @param position The position in the string being parsed that it encountered.
     * @param line The string that was being parsed.
     */
    public CommandLineSyntaxException(String msg, int position, String line) {

        super(msg);
        this.position = position;
        this.line = line;
    }

    
    /**
     * Returns the command line that was being parsed.
     * @return the line that was being parsed.
     */
    public String getLine () {
    
        return line;
    }

    
    /**
     * Returns the position into the line that was being parsed
     * 
     * @return the position The position (character index) into th eline
     *   that was being parsed.
     */
    public int getPosition () {
    
        return position;
    }
}

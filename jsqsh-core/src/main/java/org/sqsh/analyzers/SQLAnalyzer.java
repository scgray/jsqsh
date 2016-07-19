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

/**
 * A SQLAnalyzer is responsible for analyzing the current 
 * input batch so that jsqsh can ask various questions of it. The expectation 
 * here is that there may be different implementations for different database
 * vendors.
 */
public interface SQLAnalyzer {
    
    /**
     * @return The name of this analyzer
     */
    public String getName();
    
    /**
     * Analyzes a SQL of text to see if it is terminated with the provided
     * terminator character (presumably the character is a semicolon most of
     * the time). 
     * 
     * @param batch The batch to analyze
     * @param terminator The terminator character
     * @return True if the batch is terminated.
     */
    boolean isTerminated(CharSequence batch, char terminator);
}

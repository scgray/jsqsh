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
package org.sqsh.input;

/**
 * Exception thrown if the user hits CTRL-C while editing a line
 */
public class ConsoleInterruptedException 
    extends ConsoleException {
    
    private static final long serialVersionUID = 1L;
    private String partialInput;
    
    public ConsoleInterruptedException (String partialLine) {
        
        super("^C", null);
        this.partialInput = partialLine;
    }
    
    /**
     * @return The input that the user was typing at the point that they hit CTRL-C
     */
    public String getPartialInput() {
        
        return partialInput;
    }
}

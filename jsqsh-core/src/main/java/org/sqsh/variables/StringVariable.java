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
package org.sqsh.variables;

import org.sqsh.Variable;

/**
 * Used to represent a basic string variable.
 */
public class StringVariable
    extends Variable {
    
    private String value;
    
    /**
     * Required for digester.
     */
    public StringVariable() {
        
    }
    
    /**
     * Creates a string variable.
     * 
     * @param name The name of the variable.
     * @param value The value that it is to be set to.
     */
    public StringVariable (String name, String value) {
        
        super(name);
        this.value = value; 
    }
    
    /**
     * Creates a exportable string variable.
     * 
     * @param name The name of the variable.
     * @param value The value that it is to be set to.
     * @param isExported True if the variable is to be exported.
     */
    public StringVariable (String name, String value, boolean isExported) {
        
        super(name, isExported);
        this.value = value; 
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String setValue(String value) {
        
        String oldValue = value;
        this.value = value;
        
        return oldValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString () {

        return value;
    }
}

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
package org.sqsh.options;

public class Option {
    
    /**
     * The property/annotation that defined this option.
     */
    protected OptionProperty option;
    
    /**
     * This is the field in the object that is annotated with 
     * the {@link OptionProperty} annotation.
     */
    protected String fieldName;
    
    /**
     * Creates an option that is associated with a field of a java bean. 
     * @param option The option property that is associated with the field of
     *   the java bean
     * @param fieldName The name of the field the property is associated with
     */
    public Option (OptionProperty option, String fieldName) {
        
        this.option      = option;
        this.fieldName   = fieldName;
    }
    
    /**
     * @return The name of the field of the option bean that should be set
     *   with this property
     */
    public String getFieldName() {
        
        return fieldName;
    }
    
    /**
     * @return The short form of the option
     */
    public char getShortOpt() {
        
        return option.option();
    }
    
    /**
     * @return The long form of the option, or null if there is no long form
     */
    public String getLongOpt() {
        
        return option.longOption();
    }
    
    /**
     * @return A description of the option.
     */
    public String getDescription() {
        
        return option.description();
    }
    
    /**
     * @return true if the option could possibly have an argument
     */
    public boolean hasArg() {
        
        return option.arg() != ArgumentRequired.NONE;
    }
    
    /**
     * @return Whether or not the option requires an argument
     */
    public ArgumentRequired getOptRequired() {
        
        return option.arg();
    }
    
    /**
     * @return If the option requires and argument, this is a logic name to
     *   use when displaying the argument name.
     */
    public String getArgName() {
        
        return option.argName();
    }
    
    @Override
    public String toString() {
        
        StringBuilder sb = new StringBuilder();
        sb.append('-').append(option.option());
        if (option.longOption() != null) {
            
            sb.append(" (")
                .append("--").append(option.longOption())
                .append(")");
        }
        
        return sb.toString();
    }
}

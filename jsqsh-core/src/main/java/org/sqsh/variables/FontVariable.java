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

import org.sqsh.CannotSetValueError;
import org.sqsh.Variable;

/**
 * Represents a font specification.
 */
public class FontVariable
    extends Variable {
    
    private String fontName = "Monospaced";
    private int size = 10;

    @Override
    public String setValue (String value)
        throws CannotSetValueError {
        
        String []parts = value.split("-");
        boolean ok = true;
        
        if (parts.length != 2) {
            
            ok = false;
        }
        else {
            
            try {
                
                int tmpSize = Integer.parseInt(parts[1]);
                
                size = tmpSize;
                fontName = parts[0];
            }
            catch (Exception e) {
                
                ok = false;
            }
        }
        
        if (!ok) {
            
            throw new CannotSetValueError("Invalid font specification '"
                + value + "'. Fonts must be specified as Name-Size, such "
                + " as 'Monospace-10'");
        }
        
        return null;
    }

    @Override
    public String toString () {

        return fontName + "-" + size;
    }
    
    public String getFontName() {
        
        return fontName;
    }
    
    public int getFontSize() {
        
        return size;
    }
}

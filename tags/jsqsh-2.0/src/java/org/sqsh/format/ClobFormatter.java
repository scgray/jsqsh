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
package org.sqsh.format;

import java.io.Reader;
import java.sql.Clob;

import org.sqsh.Formatter;

public class ClobFormatter
    implements Formatter {
    
    public String format (Object value) {
        
        Clob clob = (Clob) value;
        StringBuilder sb = new StringBuilder();
        char []chars = new char[512];
        
        try {
            
            Reader in = clob.getCharacterStream();
            while (in.read(chars) >= 0) {
                
                sb.append(chars);
            }
            
            in.close();
        }
        catch (Exception e) {
            
            /* IGNORED */
        }
        
        return sb.toString();
    }

    public int getMaxWidth () {

        return Integer.MAX_VALUE;
    }
}

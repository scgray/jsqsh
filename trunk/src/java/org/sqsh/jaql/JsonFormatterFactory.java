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
package org.sqsh.jaql;

import org.sqsh.Session;

public class JsonFormatterFactory {
    
    public static JaqlFormatter getFormatter (Session session, String name) {
        
        return getFormatter (session, name, -1);
    }
    
    public static JaqlFormatter getFormatter (Session session, String name, int indent) {
        
        /*
         * No name provided? Check the environment.
         */
        if (name == null) {
            
            name = session.getVariable("jaql_style");
        }
        
        /*
         * If "json" is asked for, then we need to process the indent.
         */
        if (name == null || name.equals("json")) {
        
            if (indent < 0) {
                
                String prop = session.getVariable("jaql_indent");
                if (prop == null) {
                    
                    indent = 3;
                }
                else {
                    
                    indent = Integer.parseInt(prop);
                }
            }
            
            return new JsonFormatter (session, indent);
        }
        else if (name.equals("lines")) {
            
            return new JsonLinesFormatter (session);
        }
        else if (name.equals("discard")) {
            
            return new DiscardFormatter (session);
        }
        else if (name.equals("csv")) {
            
            return new JsonCSVFormatter (session);
        }
        else {
            
            return null;
        }
    }
}

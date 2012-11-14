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
import org.sqsh.input.completion.Completer;

import com.ibm.jaql.json.type.JsonArray;
import com.ibm.jaql.json.type.JsonString;
import com.ibm.jaql.json.type.JsonValue;
import com.ibm.jaql.json.util.JsonIterator;
import com.ibm.jaql.json.util.JsonUtil;
import com.ibm.jaql.lang.JaqlQuery;

/**
 * Performs tab completion during editing of Jaql.
 */
public class JaqlCompleter
    extends Completer {
    
    private JsonIterator iter = null;
    
    public JaqlCompleter(JaqlQuery engine, Session session, String line, 
        int position, String word) {

        super(session, line, position, word);
        
        StringBuilder sb = new StringBuilder("listVariables()");
        if (word.length() > 0) {
            
            sb.append(" -> filter startsWith($.var, ")
                .append(JsonUtil.quote(word)).append(")");
        }
        sb.append(" -> distinct() -> transform $.var;");
        
        /*
         * I would like to have a cleaner way to do this, but....
         */
        engine.setQueryString(sb.toString());
        
        try {
        
            JsonValue v = engine.evaluate();
            if (!(v instanceof JsonArray)) {
                
                session.err.println("Expected JsonArray from listVariables()!!");
            }
            else {
                
                iter = ((JsonArray)v).iter();
            }
        }
        catch (Exception e) {
            
            session.err.println("[" + sb + "]: " + e.getMessage());
        }
    }

    @Override
    public String next() {
        
        String name = null;

        if (iter != null) {
            
            try {
                
                if (iter.moveNext() == false) {
                
                    iter.close();
                    iter = null;
                }
                else {
                    
                    JsonValue v = iter.current();
                    if (v != null && v instanceof JsonString) {
                    
                        name = v.toString();
                    }
                }
            }
            catch (Exception e) {
            
                session.err.println("Failed to iterator to next jaql object: "
                    + e.getMessage());
                iter = null;
            }
        }
        
        return name;
    }
}

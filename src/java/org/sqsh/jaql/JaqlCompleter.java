/*
 * Copyright (C) 2007 by Scott C. Gray
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, write to the Free Software Foundation, 675 Mass Ave,
 * Cambridge, MA 02139, USA.
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

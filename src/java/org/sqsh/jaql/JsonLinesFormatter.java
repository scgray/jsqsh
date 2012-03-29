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

import java.util.Iterator;
import java.util.Map.Entry;

import org.sqsh.Session;

import com.ibm.jaql.json.type.JsonArray;
import com.ibm.jaql.json.type.JsonNumber;
import com.ibm.jaql.json.type.JsonRecord;
import com.ibm.jaql.json.type.JsonString;
import com.ibm.jaql.json.type.JsonValue;
import com.ibm.jaql.json.util.JsonIterator;
import com.ibm.jaql.json.util.JsonUtil;

/**
 * Formatter used by jaql output to write JSON values out as one line
 * per record.
 */
public class JsonLinesFormatter 
    extends JaqlFormatter {
    
    public JsonLinesFormatter (Session session) {
        
        super (session);
    }
    
    @Override
    public String getName() {

        return "lines";
    }
    
    @Override
    public int write (JsonIterator iter)
        throws Exception {
        
        int nrows = 0;
        setScale();
        
        while (!isCanceled() && iter.moveNext()) {
            
            write(iter.current());
            ++nrows;
            session.out.println();
        }
        
        return nrows;
    }
    
    @Override
    public int write (JsonValue v)
        throws Exception {
        
        int nrows = 0;
        
        if (v == null) {
            
            session.out.print("null");
            nrows = 1;
        }
        else if (v instanceof JsonArray) {
            
            JsonIterator iter = ((JsonArray)v).iter();
            
            session.out.print('[');
            while (iter.moveNext()) {
                
                if (nrows > 0)
                    session.out.print(", ");
                
                write(iter.current());
                ++nrows;
            }
            session.out.print(']');
        }
        else if (v instanceof JsonRecord) {
            
            Iterator<Entry<JsonString, JsonValue>> iter = ((JsonRecord)v).iterator();
            int count = 0;
            session.out.print("{ ");
            while (iter.hasNext()) {
            
                Entry<JsonString, JsonValue> e = iter.next();
            
                String    name = JsonUtil.quote(e.getKey().toString());
                JsonValue val  = e.getValue();
            
                if (count > 0)
                    session.out.print(", ");
            
                session.out.print(name);
                session.out.print(": ");
                write(val);
            
                ++count;
            }
            session.out.print(" }");
            nrows = 1;
        }
        else {
            
            writeScalar(v);
            nrows = 1;
        }
        
        return nrows;
    }
}

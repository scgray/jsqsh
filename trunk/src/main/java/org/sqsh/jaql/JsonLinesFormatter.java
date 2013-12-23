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
import com.ibm.jaql.util.FastPrintStream;

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
    public int write (FastPrintStream out, JsonIterator iter)
        throws Exception {
        
        int nrows = 0;
        setScale();
        
        while (!isCanceled() && iter.moveNext()) {
            
            write(out, iter.current());
            ++nrows;
            out.println();
        }
        out.flush();
        
        return nrows;
    }
    
    @Override
    public int write (FastPrintStream out, JsonValue v)
        throws Exception {
        
        int nrows = 0;
        
        if (v == null) {
            
            out.print("null");
            nrows = 1;
        }
        else if (v instanceof JsonArray) {
            
            JsonIterator iter = ((JsonArray)v).iter();
            
            out.print('[');
            while (iter.moveNext()) {
                
                if (nrows > 0)
                    out.print(", ");
                
                write(out, iter.current());
                ++nrows;
            }
            out.print(']');
        }
        else if (v instanceof JsonRecord) {
            
            Iterator<Entry<JsonString, JsonValue>> iter = ((JsonRecord)v).iterator();
            int count = 0;
            out.print("{ ");
            while (iter.hasNext()) {
            
                Entry<JsonString, JsonValue> e = iter.next();
            
                String    name = JsonUtil.quote(e.getKey().toString());
                JsonValue val  = e.getValue();
            
                if (count > 0)
                    out.print(", ");
            
                out.print(name);
                out.print(": ");
                write(out, val);
            
                ++count;
            }
            out.print(" }");
            nrows = 1;
        }
        else {
            
            writeScalar(out, v, true);
            nrows = 1;
        }
        
        return nrows;
    }
}

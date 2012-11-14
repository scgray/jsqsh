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
 * Attempts to format Jaql/JSON output as CSV output.  The logic involved
 * here makes a lot of assumptions like that the input is an array of arrays
 * (of the same size) or an array of records (of the same structure) and any
 * attempt to pass anything else in will result in undefined output.
 * 
 * <p>If the input is an array of records, then if $header is true, the
 * name of the fields in the first record encountered is assumed to be the
 * column headers and are output accordingly. if there are nested complex
 * structures, like records or arrays, they are flattened into JSON strings
 * as a single comma delimited value, thus:
 * 
 *   [ {a: 10, b: { c: 20 }} ]
 *   
 * will become:
 * 
 *   a,b
 *   10,"{ ""c"": 20 }"
 *   
 * When formatting scalar values, some attempt is made to format the values
 * according to jsqsh configured rules. For example, to honor $scale when
 * printing doubles and to honor the datetime format when printing dates.
 * For other "odd" types that jsqsh isn't aware of the value is printed as
 * Jaql would normally print them, such as regex will be "regex('...')".
 */
public class JsonCSVFormatter
    extends JaqlFormatter {
    
    private boolean doneHeader = false;
    
    public JsonCSVFormatter(Session session) {
        super(session);
    }

    @Override
    public String getName() {

        return "csv";
    }

    @Override
    public int write(FastPrintStream out, JsonIterator iter) throws Exception {

        int nrows = 0;
        
        doneHeader = false;
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
    public int write(FastPrintStream out, JsonValue v) throws Exception {
        return write(out, v, 0);
    }
    
    public int write(FastPrintStream out, JsonValue v, int nesting) throws Exception {

        int nrows = 0;
        
        if (v == null) {
            
            out.print("");
            nrows = 1;
        }
        else if (v instanceof JsonArray) {
            
            JsonIterator iter = ((JsonArray)v).iter();
            
            if (nesting == 1)
                out.print("\"[");
            else if (nesting > 1)
                out.print('[');
                
            while (iter.moveNext()) {
                
                if (nrows > 0)
                    out.print(",");
                
                write(out, iter.current(), nesting+1);
                ++nrows;
            }
            
            if (nesting == 1)
                out.print("]\"");
            else if (nesting > 1)
                out.print(']');
        }
        else if (v instanceof JsonRecord) {
            
            
            if (nesting == 1)
                out.print("\"{");
            else if (nesting > 1)
                out.print('{');
            
            if (nesting == 0 && !doneHeader) {
                
                if (session.getRendererManager().isShowHeaders()) {
                    
                    Iterator<Entry<JsonString, JsonValue>> iter = ((JsonRecord)v).iterator();
                    int count = 0;
                    while (iter.hasNext()) {
                        Entry<JsonString, JsonValue> e = iter.next();
                        String name = e.getKey().toString();
                        
                        if (count > 0) {
                            
                            out.print(',');
                        }
                        out.print(quote(name, nesting));
                        ++count;
                    }
                    
                    out.println();
                }
                
                doneHeader = true;
            }
            
            Iterator<Entry<JsonString, JsonValue>> iter = ((JsonRecord)v).iterator();
            int count = 0;
            while (iter.hasNext()) {
            
                Entry<JsonString, JsonValue> e = iter.next();
                
                JsonValue val  = e.getValue();
            
                if (count > 0)
                    out.print(",");
                
                if (nesting > 0) {
                    
                    String  name = JsonUtil.quote(e.getKey().toString());
                    out.print(quote(name, nesting+1));
                    out.print(": ");
                }
                
                write(out, val, nesting + 1);
            
                ++count;
            }
            
            if (nesting == 1)
                out.print("}\"");
            else if (nesting > 1)
                out.print('}');
            
            nrows = 1;
        }
        else {
            
            String val;
            if (nesting > 1 && !(v instanceof JsonNumber)) {
                
                val = JsonUtil.quote(getScalar(v, true));
            }
            else {
                
                val = getScalar(v, false);
            }
            
            out.print(quote(val, nesting));
            nrows = 1;
        }
        
        return nrows;
    }
    
    public String quote(String str, int nesting) {
        
        boolean needsQuoting = 
            (str.length() == 0
                || str.indexOf('"') >= 0
                || str.indexOf('\n') >= 0
                || str.indexOf(',') >= 0
                || (str.length() > 0 
                        && (Character.isWhitespace(str.charAt(0))
                              || Character.isWhitespace(str.charAt(str.length() -1)))));
        
        if (!needsQuoting) {
            
            return str;
        }
        
        StringBuilder sb = new StringBuilder();
        
        if (nesting == 1)
            sb.append('"');
        
        for (int j = 0; j < str.length(); j++) {
            
            char ch = str.charAt(j);
            if (ch == '"') {
                
                sb.append('"');
            }
            sb.append(ch);
        }
        
        if (nesting == 1)
            sb.append('"');
        
        return sb.toString();
    }
}

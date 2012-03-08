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
 * Used to format Json output in the "traditional" style of:
 * <pre>
 * [
 *    { 
 *       name: "value",
 *       name2: 10,
 *       name3: {
 *          foo: "a"
 *       },
 *       name4: [
 *          1,
 *          2,
 *          3
 *       ]
 *    }
 *    ...
 * ]
 * </pre>
 */
public class JsonFormatter
    extends JaqlFormatter {
    
    private String defaultIndent;
    
    /**
     * Creates a new formatter
     * @param session The session to which the output is to be sent
     * @param indent The indent for the session
     */
    public JsonFormatter (Session session, int indent) {
        
        super(session);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            
            sb.append(' ');
        }
        defaultIndent = sb.toString(); 
    }
    
    @Override
    public String getName() {

        return "json";
    }

    @Override
    public int write (JsonIterator iter)
        throws Exception {
        
        int nrows = 0;
        
        if (!isCanceled() && iter.moveNext()) {
            
            /*
             * The getImmutableCopy() is because some functions, such
             * as strSplit(), re-use an internal buffer during each
             * call at a given call-site, so the current value is only
             * good "right now".  That is, the JsonValue I have in my 
             * hand will have its guts re-used on the next call to
             * moveNext(), which has icky side effects in terms of our
             * display.
             */
            JsonValue first = iter.current();
            if (first != null) {

                first = first.getImmutableCopy();
            }

            if (!isCanceled() && iter.moveNext()) {
            
                session.out.println('[');
                session.out.print(defaultIndent);
                write(first, defaultIndent);
                ++nrows;
                
                /*
                 * Just in case "first" was big, we are holding a copy
                 * right now, so free that up.
                 */
                first = null;
                
                do {
                    
                    session.out.println(",");
                    session.out.print(defaultIndent);
                    JsonValue next = iter.current();
                    write(next, defaultIndent);
                    ++nrows;
                }
                while (!isCanceled() && iter.moveNext());
                
                session.out.println();
                session.out.println(']');
            }
            else {
                
                write(first, "");
                ++nrows;
                session.out.println();
            }
        }
        
        return nrows;
    }
    
    @Override
    public int write (JsonValue v) 
        throws Exception {
        
        int nrows = write(v, "");
        session.out.println();
        return nrows;
    }
    
    private int write (JsonValue v, String indent)
        throws Exception {
        
        int nrows = 0;
        
        if (v == null) {
            
            session.out.print("null");
            nrows = 1;
        }
        else if (v instanceof JsonArray) {
            
            session.out.println('[');
            nrows = printArrayBody((JsonArray)v, indent + defaultIndent);
            session.out.println();
            session.out.print(indent);
            session.out.print(']');
        }
        else if (v instanceof JsonRecord) {
            
            session.out.println('{');
            printRecordBody((JsonRecord)v, indent + defaultIndent);
            session.out.println();
            session.out.print(indent);
            session.out.print('}');
            nrows = 1;
        }
        else if (v instanceof JsonNumber) {
            
            session.out.print(v.toString());
            nrows = 1;
        }
        else {
            
            session.out.print(JsonUtil.quote(v.toString()));
            nrows = 1;
        }
        
        /*
         * This is a super hack. If the indent is currently zero long
         * then we return the row count because we know that we just 
         * printed the outermost element.
         */
        if (indent.length() != 0)
            nrows = 0;
        
        return nrows;
    }
    
    private int printArrayBody (JsonArray a, String indent)
        throws Exception {
        
        JsonIterator iter = a.iter();
        int count = 0;
            
        while (iter.moveNext()) {
                
            if (count > 0)
                session.out.println(',');
            ++count;
                
            JsonValue av = iter.current();
            
            session.out.print(indent);
            write(av, indent);
        }
        
        return count;
    }
    
    private void printRecordBody (JsonRecord r, String indent)
        throws Exception {
        
        int idx = 0;
        
        Iterator<Entry<JsonString, JsonValue>> iter = r.iterator();
        while (iter.hasNext()) {
            
            Entry<JsonString, JsonValue> e = iter.next();
            
            String    name = JsonUtil.quote(e.getKey().toString());
            JsonValue val  = e.getValue();
            
            if (idx > 0)
                session.out.println(",");
            
            session.out.print(indent);
            session.out.print(name);
            session.out.print(": ");
            write(val, indent);
            
            ++idx;
        }
    }
}

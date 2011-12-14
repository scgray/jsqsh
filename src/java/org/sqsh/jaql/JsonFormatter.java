package org.sqsh.jaql;

import java.util.Iterator;
import java.util.Map.Entry;

import org.sqsh.Session;

import com.ibm.jaql.json.type.JsonArray;
import com.ibm.jaql.json.type.JsonError;
import com.ibm.jaql.json.type.JsonRecord;
import com.ibm.jaql.json.type.JsonString;
import com.ibm.jaql.json.type.JsonValue;
import com.ibm.jaql.json.util.JsonIterator;
import com.ibm.jaql.json.util.JsonUtil;

public class JsonFormatter
    extends JaqlFormatter {
    
    private String defaultIndent;
    
    public JsonFormatter (Session session, int indent) {
        
        super(session);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            
            sb.append(' ');
        }
        defaultIndent = sb.toString(); 
    }
    
    @Override
    public int write (JsonIterator iter)
        throws Exception {
        
        int nrows = 0;
        
        if (iter.moveNext()) {
            
            JsonValue v = iter.current();
            if (iter.moveNext()) {
                
                session.out.println('[');
                session.out.print(defaultIndent);
                write(v, defaultIndent);
                ++nrows;
                do {
                    
                    session.out.println(",");
                    session.out.print(defaultIndent);
                    v = iter.current();
                    write(v, defaultIndent);
                    ++nrows;
                }
                while (iter.moveNext());
                session.out.println();
                session.out.println(']');
            }
            else {
                
                write(v, "");
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
        /*
        else if (v instanceof JsonError) {
            
            JsonError e = (JsonError)v;
            
            session.err.println("["
               + e.getFilename()
               + ", line " + e.getLine()
               + " ]: "
               + e.getMessage());
            
            Exception ex = e.getException();
            if (ex != null) {
                
                session.err.println("Stack trace:");
                ex.printStackTrace(session.err);
            }
        }
        */
        else if (v instanceof JsonString) {
            
            session.out.print(JsonUtil.quote(v.toString()));
            nrows = 1;
        }
        else {
            
            session.out.print(v.toString());
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

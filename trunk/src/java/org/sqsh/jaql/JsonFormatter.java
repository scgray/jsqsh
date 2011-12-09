package org.sqsh.jaql;

import java.util.Iterator;
import java.util.Map.Entry;

import org.sqsh.Session;

import com.ibm.jaql.json.type.JsonArray;
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
    public void write (JsonIterator iter)
        throws Exception {
        
        if (iter.moveNext()) {
            
            JsonValue v = iter.current();
            if (iter.moveNext()) {
                
                session.out.println('[');
                session.out.print(defaultIndent);
                write(v, defaultIndent);
                do {
                    
                    session.out.println(",");
                    session.out.print(defaultIndent);
                    write(iter.current(), defaultIndent);
                }
                while (iter.moveNext());
                session.out.println();
                session.out.println(']');
            }
            else {
                
                write(v, "");
                session.out.println();
            }
        }
    }
    
    @Override
    public void write (JsonValue v) 
        throws Exception {
        
        write(v, "");
        session.out.println();
    }
    
    private void write (JsonValue v, String indent)
        throws Exception {
        
        if (v == null) {
            
            session.out.print("null");
        }
        else if (v instanceof JsonArray) {
            
            session.out.println('[');
            printArrayBody((JsonArray)v, indent + defaultIndent);
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
        }
        else if (v instanceof JsonString) {
            
            session.out.print(JsonUtil.quote(v.toString()));
        }
        else {
            
            session.out.print(v.toString());
        }
    }
    
    private void printArrayBody (JsonArray a, String indent)
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

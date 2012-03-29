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
    public int write(JsonIterator iter) throws Exception {

        int nrows = 0;
        
        doneHeader = false;
        setScale();
        
        while (!isCanceled() && iter.moveNext()) {
            
            write(iter.current());
            ++nrows;
            session.out.println();
        }
        
        return nrows;
    }

    @Override
    public int write(JsonValue v) throws Exception {
        return write(v, 0);
    }
    
    public int write(JsonValue v, int nesting) throws Exception {

        int nrows = 0;
        
        if (v == null) {
            
            session.out.print("null");
            nrows = 1;
        }
        else if (v instanceof JsonArray) {
            
            JsonIterator iter = ((JsonArray)v).iter();
            
            if (nesting == 1)
                session.out.print("\"[");
            else if (nesting > 1)
                session.out.print('[');
                
            while (iter.moveNext()) {
                
                if (nrows > 0)
                    session.out.print(",");
                
                write(iter.current(), nesting+1);
                ++nrows;
            }
            
            if (nesting == 1)
                session.out.print("]\"");
            else if (nesting > 1)
                session.out.print(']');
        }
        else if (v instanceof JsonRecord) {
            
            
            if (nesting == 1)
                session.out.print("\"{");
            else if (nesting > 1)
                session.out.print('{');
            
            if (nesting == 0 && !doneHeader) {
                
                if (session.getRendererManager().isShowHeaders()) {
                    
                    Iterator<Entry<JsonString, JsonValue>> iter = ((JsonRecord)v).iterator();
                    int count = 0;
                    while (iter.hasNext()) {
                        Entry<JsonString, JsonValue> e = iter.next();
                        String name = e.getKey().toString();
                        
                        if (count > 0) {
                            
                            session.out.print(',');
                        }
                        session.out.print(quote(name, nesting));
                        ++count;
                    }
                    
                    session.out.println();
                }
                
                doneHeader = true;
            }
            
            Iterator<Entry<JsonString, JsonValue>> iter = ((JsonRecord)v).iterator();
            int count = 0;
            while (iter.hasNext()) {
            
                Entry<JsonString, JsonValue> e = iter.next();
                
                JsonValue val  = e.getValue();
            
                if (count > 0)
                    session.out.print(",");
                
                if (nesting > 0) {
                    
                    String  name = JsonUtil.quote(e.getKey().toString());
                    session.out.print(quote(name, nesting+1));
                    session.out.print(": ");
                }
                
                write(val, nesting + 1);
            
                ++count;
            }
            
            if (nesting == 1)
                session.out.print("}\"");
            else if (nesting > 1)
                session.out.print('}');
            
            nrows = 1;
        }
        else {
            
            String val;
            if (nesting > 1 && !(v instanceof JsonNumber)) {
                
                val = JsonUtil.quote(getScalar(v));
            }
            else {
                
                val = getScalar(v);
            }
            
            session.out.print(quote(val, nesting));
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

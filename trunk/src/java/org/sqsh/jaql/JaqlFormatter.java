package org.sqsh.jaql;

import org.sqsh.Session;

import com.ibm.jaql.json.type.JsonValue;
import com.ibm.jaql.json.util.JsonIterator;

/**
 * Interface for a class capable of print JSON.
 */
public abstract class JaqlFormatter {
    
    protected Session session;
    
    public JaqlFormatter (Session session) {
        
        this.session = session;
    }
    
    public abstract void write (JsonIterator iter)
        throws Exception;
    
    public abstract void write (JsonValue v) 
        throws Exception;
}

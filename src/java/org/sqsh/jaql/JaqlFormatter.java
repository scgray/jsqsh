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
    
    /**
     * Write the results from a {@link JsonIterator}
     * @param iter The iterator
     * @return The number of "rows" returned, which typically corresponds
     *   to the number of elements in the outer most array.
     * @throws Exception
     */
    public abstract int write (JsonIterator iter)
        throws Exception;
    
    /**
     * Write the results from a {@link JsonValue}
     * @param v The value to write
     * @return The number of "rows" returned, which typically corresponds
     *   to the number of elements in the outer most array.
     * @throws Exception
     */
    public abstract int write (JsonValue v) 
        throws Exception;
}

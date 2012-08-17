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

import com.ibm.jaql.json.type.JsonValue;
import com.ibm.jaql.json.util.JsonIterator;
import com.ibm.jaql.util.FastPrintStream;

/**
 * Formatter that discards all values without displaying them. This
 * is primarily used for timing queries to avoid including the
 * decoding of the values to the screen.
 */
public class DiscardFormatter
   extends JaqlFormatter {
    
    public DiscardFormatter (Session session) {
        
        super(session);
    }

    @Override
    public String getName() {

        return "discard";
    }

    @Override
    public int write(FastPrintStream out, JsonIterator iter) throws Exception {
        
        int nrows = 0;
        
        if (!isCanceled() && iter.moveNext()) {
            
            ++nrows;
        }
        
        return nrows;
    }

    @Override
    public int write(FastPrintStream out, JsonValue v) throws Exception {

        return 1;
    }
}

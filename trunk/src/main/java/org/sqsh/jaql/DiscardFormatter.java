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
        
        while (!isCanceled() && iter.moveNext()) {
            
            ++nrows;
        }
        
        return nrows;
    }

    @Override
    public int write(FastPrintStream out, JsonValue v) throws Exception {

        return 1;
    }
}

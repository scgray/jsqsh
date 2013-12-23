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
package org.sqsh.input;

import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineLibrary;
import org.sqsh.SqshContext;

public class GetlineLineReader 
        extends ReadlineLineReader {
    
    public GetlineLineReader(SqshContext ctx)
        throws ConsoleException {
   
        try {
            
            Readline.load(ReadlineLibrary.Getline);
        }
        catch (Throwable e) {
            
            throw new ConsoleException(e.getMessage(), e);
        }
        
        init(ctx);
    }
    
    @Override
    public String getName() {
    
        return ConsoleLineReader.EDITLINE;
    }
}
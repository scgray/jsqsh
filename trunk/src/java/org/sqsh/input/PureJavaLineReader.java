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

import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.sqsh.Session;
import org.sqsh.SqshContext;

/**
 * The PureJavaLineReader is responsible for reading input using standard java
 * facilities, which means that it doesn't provide any command line editing or
 * history storage and recall.  This is used as either the last resort fall-back
 * when no other reader is available, or when reading input non-interactively.
 * 
 * @author gray
 */
public class PureJavaLineReader
        extends ConsoleLineReader {
    
    protected SqshContext ctx;
    protected StringBuilder sb = new StringBuilder();
    
    public PureJavaLineReader(SqshContext ctx) {
        
        this.ctx = ctx;
    }
    
    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public String readline(String prompt, boolean addToHistory)
            throws EOFException, IOException, UnsupportedEncodingException {
        
        Session session = ctx.getCurrentSession();
        
        session.out.print(prompt);
        
        sb.setLength(0);
        int ch = session.in.read();
        if (ch == -1) {
            
            return null;
        }
        
        while (ch != -1 && ch != '\n') {
            
            sb.append((char) ch);
            ch = session.in.read();
        }
        
        return sb.toString();
    }

    @Override
    public void readHistory(String filename) throws ConsoleException {
        /* NO-OP */
    }


    @Override
    public void addToHistory(String line) {
        /* NO-OP */
    }

    @Override
    public void writeHistory() throws ConsoleException {
        /* NO-OP */
    }

    @Override
    public String getName() {

        return "PureJava";
    }
}
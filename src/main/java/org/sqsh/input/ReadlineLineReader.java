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

import org.gnu.readline.Readline;
import org.sqsh.SqshContext;

/**
 * Abstract class from which all specific readline implementation reads 
 * will extend.
 * 
 * @author gray
 */
public abstract class ReadlineLineReader
        extends ConsoleLineReader {
    
    private String historyFile;
    
    protected void init(SqshContext ctx) {
        
        Readline.initReadline("JSqsh");
        
        /*
         * Install our tab completer. This could fail if the underlying
         * chosen implementation does not support completion.
         */
        try {
            
            Readline.setWordBreakCharacters(" \t,/.()<>=?");
            Readline.setCompleter(new ReadlineTabCompleter(ctx));
        }
        catch (Exception e) {
            
            /* IGNORED */
        }
    }

    @Override
    public void addToHistory(String line) {

        Readline.addToHistory(line);
    }


    @Override
    public boolean isTerminal() {

        return Readline.hasTerminal();
    }

    @Override
    public void readHistory(String filename)
        throws ConsoleException {

        try {
            
            Readline.readHistoryFile(filename);
            this.historyFile = filename;
        }
        catch (Throwable e) {
            
            throw new ConsoleException(e.getMessage(), e);
        }
    }

    @Override
    public String readline(String prompt, boolean addToHistory)
        throws ConsoleException {

        try {

            return Readline.readline(prompt, addToHistory);
        }
        catch (EOFException e) {
            
            throw new ConsoleEOFException();
        }
        catch (Throwable e) {
            
            throw new ConsoleException(e.getMessage(), e);
        }
    }
    
    @Override
    public void writeHistory()
        throws ConsoleException {

        try {
            
            if (historyFile != null)
            {
                Readline.writeHistoryFile(historyFile);
            }
        }
        catch (Throwable e) {
            
            throw new ConsoleException(e.getMessage(), e);
        }
    }
}

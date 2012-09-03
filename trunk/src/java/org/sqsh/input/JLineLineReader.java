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
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import jline.console.ConsoleReader;
import jline.console.history.FileHistory;

import org.sqsh.SqshContext;

public class JLineLineReader
        extends ConsoleLineReader {
    
    SqshContext   ctx;
    ConsoleReader reader;
    
    public JLineLineReader (SqshContext ctx)
        throws ConsoleException {
        
        this.ctx = ctx;
        
        try {
            
            reader = new ConsoleReader();
            
            /* 
             * Jsqsh interprets the "!" for itself, so we can't let jline
             * do that for us.
             */
            reader.setExpandEvents(false);
            
            reader.setHistoryEnabled(true);
            reader.addCompleter(new JLineTabCompleter(ctx));
        }
        catch (IOException e) {
            
            throw new ConsoleException(e.getMessage(), e);
        }
    }
    
    @Override
    public void setEditingMode(String name) {

        if (!reader.setKeyMap(name)) {
            throw new UnsupportedOperationException("Invalid keymap name '"
               + name + "'");
        }
    }

    @Override
    public String getEditingMode() {

        return reader.getKeyMap();
    }

    @Override
    public void addToHistory(String line) {

        reader.getHistory().add(line);
    }

    @Override
    public boolean isTerminal() {

        return reader.getTerminal().isSupported();
    }

    @Override
    public String getName() {

        return "JLine";
    }

    @Override
    public void readHistory(String filename)
        throws ConsoleException {

        try {
            
            FileHistory history = new FileHistory(new File(filename));
            reader.setHistory(history);
        }
        catch (IOException e) {
            
            throw new ConsoleException(e.getMessage(), e);
        }
    }

    @Override
    public String readline(String prompt, boolean addToHistory)
        throws EOFException, IOException, UnsupportedEncodingException {

        String str = reader.readLine(prompt);
        if (str == null) {
            
            throw new EOFException();
        }
        
        return str;
    }

    @Override
    public void writeHistory()
        throws ConsoleException {
        
        try {
            
            FileHistory hist = (FileHistory) reader.getHistory();
            hist.flush();
        }
        catch (IOException e) {
            
            throw new ConsoleException(e.getMessage(), e);
        }
    }
}

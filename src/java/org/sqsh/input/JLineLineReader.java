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
package org.sqsh.input;

import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import jline.console.ConsoleReader;
import jline.console.history.FileHistory;
import jline.console.history.History;

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
            reader.setBellEnabled(false);
            reader.setHistoryEnabled(true);
            reader.addCompleter(new JLineTabCompleter(ctx));
        }
        catch (IOException e) {
            
            throw new ConsoleException(e.getMessage(), e);
        }
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
            
            History history = new FileHistory(new File(filename));
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
    public void writeHistory(String filename)
        throws ConsoleException {
        
        try {
            
            FileHistory history = (FileHistory)reader.getHistory();
            history.flush();
        }
        catch (IOException e) {
            
            throw new ConsoleException(e.getMessage(), e);
        }
    }
}

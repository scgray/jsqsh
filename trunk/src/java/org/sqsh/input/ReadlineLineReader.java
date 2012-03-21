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
import java.io.IOException;
import java.io.UnsupportedEncodingException;

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
        throws UnsupportedEncodingException, IOException, EOFException {

        return Readline.readline(prompt, addToHistory);
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

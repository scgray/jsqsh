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

import org.sqsh.SqshContext;

/**
 * A ConsoleLineReader is an interface that allows you to read a line 
 * from the console (duh!). The purpose for this class to abstract 
 * away the actual implementation of the console interface and, most
 * importantly to try to give the user the handle to the most feature-rich
 * interface to the console to allow for line editing, history, completion,
 * etc.  Currently this supports the Java-Readline interface, which wraps
 * GNU Readline, Editline, and Getline as well as JLine (which is a pure
 * java interface for doing line editing).
 * 
 * @author gray
 */
public abstract class ConsoleLineReader {
    
    /** 
     * Attempts to determine which readline API you happen to have available
     * (and that really works) and configures itself to use that API. 
     * 
     * @param readerType If null, then the first available readline
     *   implementation will be used by checking in the following order
     *   (readline, editline, getline, jline, purejava). If non-null,
     *   on of the above names may be provided to for a particular 
     *   implementation to be used.
     */
    static public ConsoleLineReader getReader(
            SqshContext ctx, String readerType) {
        
        ConsoleLineReader reader = null;
        
        /*
         * Allow the JSQSH_READLINE environment variable to force
         * a particular input method.
         */
        if (readerType == null) {
            
            readerType = System.getenv("JSQSH_READLINE");
        }
        
        /*
         * This will be set to the reader that has been found during
         * our search for a suitable reader.
         */
        StringBuffer errors = new StringBuffer();
        
        /*
         * Readline initialization
         */
        if (readerType == null 
                || "readline".equalsIgnoreCase(readerType)) {
            
            try {
                
                reader = new GnuReadlineLineReader(ctx);
            }
            catch (Throwable e) {
                
                errors.append("[GNU Readline: ").append(
                        e.getMessage()).append("]");
            }
        }
        
        /*
         * Editline initialization
         */
        if (reader == null
                && (readerType == null  
                        || "editline".equalsIgnoreCase(readerType))) {
            
            try {
            
                reader = new EditlineLineReader(ctx);
            }
            catch (Throwable e) {
                
                errors.append("[Editline: ").append(e.getMessage()).append("]");
            }
        }
        
        if (reader == null
                && (readerType == null  
                        || "jline".equalsIgnoreCase(readerType))) {
            
            try {
            
                reader = new JLineLineReader(ctx);
            }
            catch (Throwable e) {
                
                errors.append("[JLine: ").append(e.getMessage()).append("]");
            }
        }
        
        /*
         * Getline initialization
         */
        if (reader == null
                && (readerType == null  
                        || "getline".equalsIgnoreCase(readerType))) {
            
            try {
            
                reader = new GetlineLineReader(ctx);
            }
            catch (Throwable e) {
                
                errors.append("[Getline: ").append(e.getMessage()).append("]");
            }
        }
        
        if (reader == null) {
            
            try {
                
                System.err.println("WARNING: Unable to load readline library. "
                    + "(Reasons: "
                    + errors.toString() 
                    + "). Run '\\help readline' for details.");
            
                reader = new PureJavaLineReader(ctx);
            }
            catch (Throwable e) {
                
                System.err.println("Couldn't load pure java readline library!: "
                    + e.getMessage());
            }
        }
        
        return reader;
    }
    
    /**
     * Returns the name of the reader.
     * 
     * @return the name of the reader.
     */
    public abstract String getName();
    
    /**
     * @return true if the input is from a terminal, false otherwise
     *   (for example, if the input is coming from a redirected stdin).
     */
    public abstract boolean isTerminal();
    
    /**
     * Reads a line of input.
     * 
     * @param prompt Prompt to present to the user.
     * @param addToHistory If true, then the line that is input is saved
     *    to the history.
     * @return The newly read line or NULL if there is no input.
     * @throws EOFException Thrown if the end of input is reached.
     */
    public abstract String readline(String prompt, boolean addToHistory)
        throws EOFException, IOException, UnsupportedEncodingException;
    
    /**
     * Read in the readline history file.
     * 
     * @param filename Filename to read in.
     * @throws ConsoleException If the file cannot be read (either doesn't
     *   exist, no permissions, badly formatted, etc.).
     */
    public abstract void readHistory (String filename)
        throws ConsoleException;
    
    /**
     * Adds a line to the readline history.
     * 
     * @param line The line to add.
     */
    public abstract void addToHistory(String line);
    
    /**
     * Write the readline history file.
     * 
     * @param filename Filename to write to.
     * @throws ConsoleException If the file cannot be written.
     */
    public abstract void writeHistory (String filename)
        throws ConsoleException;
}

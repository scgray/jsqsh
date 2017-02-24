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

import java.io.Console;

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
     * Name of the console reader that utilizes the JNI readline library.
     */
    public static final String READLINE = "Readline";
    /**
     * Name of the console reader that utilizes the JNI editline library.
     */
    public static final String EDITLINE = "Editline";
    /**
     * Name of the console reader that utilizes the JNI getline library.
     */
    public static final String GETLINE = "Getline";
    /**
     * Name of the console reader that utilizes the pure java jline library
     */
    public static final String JLINE    = "JLine";
    
    /**
     * A synonym for "none"
     */
    public static final String PUREJAVA = "PureJava";
    
    /**
     * A basic console reader that provides no line editing capabilities,
     * using direct java I/O.
     */
    public static final String NONE     = "None";
    
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
         * "PureJava" is just a synonym for "none". 
         */
        if (readerType != null 
            && readerType.equalsIgnoreCase(ConsoleLineReader.PUREJAVA)) {
            
            readerType = ConsoleLineReader.NONE;
        }
        
        /*
         * JLine initialization
         */
        if (readerType == null  || JLINE.equalsIgnoreCase(readerType)) {
            
            try {
            
                reader = new JLineLineReader(ctx);
            }
            catch (Throwable e) {
                
                errors.append("[JLine: ").append(e.getMessage()).append("]");
            }
        }
        
        /*
         * Readline initialization
         */
        if (reader == null
                && (readerType == null 
                        || READLINE.equalsIgnoreCase(readerType))) {
            
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
                        || EDITLINE.equalsIgnoreCase(readerType))) {
            
            try {
            
                reader = new EditlineLineReader(ctx);
            }
            catch (Throwable e) {
                
                errors.append("[Editline: ").append(e.getMessage()).append("]");
            }
        }
        
        
        /*
         * Getline initialization
         */
        if (reader == null
                && (readerType == null  
                        || GETLINE.equalsIgnoreCase(readerType))) {
            
            try {
            
                reader = new GetlineLineReader(ctx);
            }
            catch (Throwable e) {
                
                errors.append("[Getline: ").append(e.getMessage()).append("]");
            }
        }
        
        if (reader == null
                && (readerType == null  
                        || NONE.equalsIgnoreCase(readerType)
                        || PUREJAVA.equalsIgnoreCase(readerType))) {
            
            reader = new PureJavaLineReader(ctx);
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
     * Switches the key mapping for the editing to the specified map name.
     * If the name is not recognized then {@link UnsupportedOperationException}
     * is thrown.
     * 
     * @param name The name of the editing mode.
     */
    public void setEditingMode(String name) {
        throw new UnsupportedOperationException("Cannot change the editing "
           + "mode for " + getClass().getName());
    }
    
    /**
     * Retrieves the current key mapping for the reader.
     * @return The name of the key mapping or null if the mapping name
     *   cannot be determined or is not changeable for the implementation.
     */
    public String getEditingMode() {
        return null;
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
     * @throws ConsoleException Thrown if an issue is encountered while
     *   reading user input.  Common subclasses you should look out for are
     *   {@link ConsoleEOFException} and {@link ConsoleInterruptedException}
     */
    public abstract String readline(String prompt, boolean addToHistory)
        throws ConsoleException;
    
    /**
     * Reads a line of input, returning null if there is no input, the
     * end-of-input (EOF) is reached, the user is interrupted (^C) or any
     * other error is encountered.
     * 
     * @param prompt Prompt to present to the user.
     * @param addToHistory If true, then the line that is input is saved
     *    to the history.
     * @return The newly read line or NULL if there is no input or an
     *   error is encountered.
     */
    public String readlineSafe(String prompt, boolean addToHistory) {
        
        try {
            
            return readline(prompt, addToHistory);
        }
        catch (ConsoleException e) {
            
            return null;
        }
    }

    /**
     * Reads a line of input, returning null if there is no input, the
     * end-of-input (EOF) is reached, the user is interrupted (^C) or any
     * other error is encountered. If input is read, it is automatically
     * added to the history.
     * 
     * @param prompt Prompt to present to the user.
     * @return The newly read line or NULL if there is no input or an
     *   error is encountered.
     */
    public String readlineSafe(String prompt) {
        
        return readlineSafe(prompt, true);
    }
    
    /**
     * Reads a masked password (of up to 128 characters in length)
     * 
     * @param prompt Prompt to present to the user.
     * @return The password or null if there is no input
     * @throws ConsoleException Thrown if an issue is encountered while
     *   reading user input.  Common subclasses you should look out for are
     *   {@link ConsoleEOFException} and {@link ConsoleInterruptedException}
     */
    public String readPassword(String prompt)
        throws ConsoleException {
        
        try {

           Console console = System.console();
           char []chars = console.readPassword(prompt);
           if (chars.length == 0) {
            
               return null;
           }
        
           return new String(chars);
        }
        catch (Throwable e) {
            
            throw new ConsoleException(e.getMessage(), e);
        }
    }
    
    /**
     * Reads a masked password (of up to 128 characters in length)
     * 
     * @param prompt Prompt to present to the user.
     * @return The password or null if there is no input or an error is encountered.
     */
    public String readPasswordSafe(String prompt) {
        
        try {
            
            return readPassword(prompt);
        }
        catch (ConsoleException e) {
            
            return null;
        }
    }
    
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
     * @throws ConsoleException If the file cannot be written.
     */
    public abstract void writeHistory ()
        throws ConsoleException;
}

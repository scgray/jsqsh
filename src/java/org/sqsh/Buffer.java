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
package org.sqsh;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringReader;


/**
 * Represents SQL block that is being worked on.
 */
public class Buffer {
    
    /**
     * Every buffer is assigned a unique ID by the {@link BufferManager}
     * when it is created.
     */
    private int id = -1;
    
    /**
     * The actual buffer of SQL.
     */
    private StringBuilder buffer = null;
    
    /**
     * The number of lines of text that are contained in the buffer.
     */
    private int lineCount = 0;
    
    /**
     * The platform-specific line separator.
     */
    private static final String LINE_SEP = System.getProperty("line.separator");
    
    /**
     * Creates an empty buffer.
     * 
     * @param id The id to assign to the buffer.
     */
    public Buffer() {
        
        buffer = new StringBuilder();
    }
    
    /**
     * Creates an empty buffer.
     * 
     * @param id The id to assign to the buffer.
     */
    public Buffer(int id) {
        
        this();
        this.id = id;
    }
    
    /**
     * Creates a buffer that is pre-populated with SQL.
     * 
     * @param sql The SQL statement.
     */
    public Buffer(String sql) {
        
        set(sql);
    }
    
    /**
     * Creates a buffer that is pre-populated with SQL.
     * 
     * @param id The id assigned to the buffer.
     * @param sql The SQL statement.
     */
    public Buffer(int id, String sql) {
        
        set(sql);
        this.id = id;
    }
    
    /**
     * Checks whether or not a buffer is empty.
     * 
     * @param ignoreWhitespace If true, then a buffer that is full of nothing
     *    but whitespace (as determined by {@link Character#isWhitespace(char)})
     *    is considered empty.
     * @return true if the buffer is empty.
     */
    public boolean isEmpty (boolean ignoreWhitespace) {
        
        int len = buffer.length();
        boolean isEmpty = false;
        
        if (len == 0)
            isEmpty = true;
        else if (!ignoreWhitespace)
            isEmpty = false;
        else {
            
            int idx = 0;
            while (idx < len && Character.isWhitespace(buffer.charAt(idx))) {
                
                ++idx;
            }
            
            isEmpty = (idx == len);
        }
        
        return isEmpty;
    }
    
    /**
     * @return The id of the buffer. An id of -1 indicates that the
     * buffer has not yet been assigned an id.
     */
    public int getId () {
    
        return id;
    }
    
    /**
     * @param id Assigns an id to the buffer.
     */
    public void setId (int id) {
    
        this.id = id;
    }

    /**
     * Save the contents of the buffer to the specified file.
     * @param file The file to save to.
     */
    public void save (File file)
        throws IOException {
        
        PrintStream out = new PrintStream(file);
        out.println(buffer.toString());
        out.flush();
        out.close();
    }
    
    /**
     * Loads the buffer from a file.
     * @param file The file to read.
     */
    public void load (File file)
        throws IOException {
        
        BufferedReader in = new BufferedReader(
            new InputStreamReader(new FileInputStream(file)));
        
        clear();
        
        try {
            
            String line = in.readLine();
            while (line != null) {
                
                addLine(line);
                line = in.readLine();
            }
        }
        finally {
            
            try {
                
                in.close();
            }
            catch (IOException e) {
                
                /* IGNORED */
            }
        }
    }
    
    /**
     * Adds a single line of text to the current buffer. The line is
     * expected to not include any newlines.
     * 
     * @param line The line of text.
     */
    public void addLine(String line) {
        
        ++lineCount;
        buffer.append(line);
        buffer.append(LINE_SEP);
    }
    
    /**
     * Adds sql to the current buffer that may or may not contain
     * multiple lines of code.
     * 
     * @param sql The sql to be added.
     */
    public void add(String sql) {
        
        BufferedReader reader = new BufferedReader(
            new StringReader(sql));
        
        try {
            
            String line = reader.readLine();
            while (line != null) {
                
                ++lineCount;
                buffer.append(line);
                buffer.append(LINE_SEP);
                
                line = reader.readLine();
            }
        }
        catch (IOException e) {
            
            /* CAN'T HAPPEN */
        }
    }
    
    /**
     * Returns the current line number of the buffer.
     * 
     * @return current line number of the buffer.
     */
    public int getLineNumber() {
        
        return lineCount + 1;
    }
    
    /**
     * Sets the contents of the buffer to the specified SQL string.
     * 
     * @param sql String of SQL that is being set.
     */
    public void set(String sql) {
        
        buffer = new StringBuilder();
        add(sql);
    }
    
    /**
     * Clears the buffer.
     */
    public void clear() {
        
        lineCount = 0;
        buffer.setLength(0);
    }
    
    /**
     * Changes the length of the contents of the buffer. This
     * may only be used to shrink the buffer.
     * 
     * @param length The new length
     */
    public void setLength(int length) {
        
        if (length >= buffer.length()) {
            
            return;
        }
        
        for (int i = length; i < buffer.length(); i++) {
            
            if (buffer.charAt(i) == '\n') {
                
                --lineCount;
            }
        }
        
        buffer.setLength(length);
    }
    
    /**
     * Returns the buffer as a string.
     */
    public String toString() {
        
        return buffer.toString();
    }
}

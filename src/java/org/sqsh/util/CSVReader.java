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
package org.sqsh.util;

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class capable of processing a file full of comma separated values.
 */
public class CSVReader {
    
    private boolean hasHeaders = true;
    private String []headers = null;
    private InputStream in;
        
    /**
     * Creates a CSVReader.
     * 
     * @param in The input stream to read from.
     * @param headers If true, then the first line of the input file
     *    is assumed to contain column headers.
     *    
     * @throws IOException Thrown if the input stream could not be read.
     */
    public CSVReader (InputStream in, boolean headers) 
        throws IOException {
        
        this.hasHeaders = headers;
        this.in = in;
        
        if (hasHeaders) {
            
            this.headers = next();
        }
    }
    
    /**
     * If the CSV reader was told the file contains column headers, then
     * the headers are returned here (if available).
     * 
     * @return The set of headers for the file or null if no headers
     *   where available.
     */
    public String[] getHeaders() {
        
        return headers;
    }
    
    /**
     * Fetches the next row of CSV values.
     * 
     * @return The next set of CSV values or null if the EOF has been
     *   reached.
     * @throws IOException Thrown if things go bad.
     */
    public String[] next()
        throws IOException {
        
        List<String> words = new ArrayList<String>();
        boolean done = false;
        StringBuilder word = new StringBuilder();
            
        int ch = in.read();
        while (!done && ch >= 0) {
            
            if (ch == '\r') {
                
                ch = in.read();
            }
            else if (ch == '\n') {
                
                words.add(trimField(word));
                done = true;
            }
            else if (Character.isWhitespace(ch)
                    && word.length() == 0) {
                
                /*
                 * Discard leading white space.
                 */
                ch = in.read();
            }
            else if (ch == ',') {
                
                words.add(trimField(word));
            	word.setLength(0);
            	ch = in.read();
            }
            else if (ch == '"') {
                
                /*
                 * Double quote is only interpreted as "escaping" commas
                 * and other double quotes if it is the first character
                 * in the field.
                 */
                if (word.length() > 0) {
                    
                    word.append((char) ch);
                    ch = in.read();
                }
                else {
                    
                    ch = doQuotedField(word);
                    words.add(word.toString());
                    word.setLength(0);
                    
                    /*
                     * After the closing quote, we will suck forward
                     * to consume white space. We stop at a comma,
                     * or new line.
                     */
                    while (ch != '\n' && ch != ','
                        && Character.isWhitespace(ch)) {
                        
                        ch = in.read();
                    }
                    
                    if (ch == '\n' || ch == -1) {
                        
                        done = true;
                    }
                    else if (ch == ',') {
                        
                        ch = in.read();
                    }
                }
            }
            else {
                
                word.append((char) ch);
                ch = in.read();
            }
        }
        
        if (done == false && (words.size() > 0 || word.length() > 0)) {
            
            words.add(trimField(word));
        }
        
        if (ch == -1 && words.size() == 0) {
            
            return null;
        }
        
        return words.toArray(new String[0]);
    }
    
    /**
     * This method expects to be called after a " has been read as the
     * first character of a field from the input stream.  It processes
     * the field as if it was a quoted field, consuming all characters
     * in the field.
     * 
     * @param field A place to put the characters that are in the field.
     * @return Since this method needs to read forward after the last 
     *   double quote (to check to see if it is a doubled double quote
     *   ("") it returns the last character read.
     *   
     * @throws IOException Thrown if things go wrong.
     */
    private int doQuotedField(StringBuilder field)
        throws IOException {
        
        boolean done = false;
        int ch = in.read();
        while (!done && ch >= 0) {
            
            if (ch == '"') {
                
                ch = in.read();
                if (ch == '"') {
                    
                    field.append('"');
                    ch = in.read();
                }
                else {
                    
                    done = true;
                }
            }
            else {
                
                field.append((char) ch);
                ch = in.read();
            }
        }
        
        return ch;
    }
    
    /**
     * Trims the trailing spaces from a field.
     * 
     * @param field The field to be trimmed.
     * @return 
     */
    private String trimField(StringBuilder field) {
        
        int idx = field.length() - 1;
        while (idx >= 0 && Character.isWhitespace(field.charAt(idx))) {
            
            --idx;
        }
        
        if (idx >= 0) {
            
            field.setLength(idx+1);
        }
        else {
            
            field.setLength(0);
        }
        
        if (field.length() == 0) {
            
            return null;
        }
        
        return field.toString();
    }
}

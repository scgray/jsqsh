/*
 * Copyright 2007-2022 Scott C. Gray
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
package org.sqsh;

import java.util.logging.Logger;

/**
 * Used by {@link Tokenizer} to indicate that a file redirection
 * has been encountered. A file redirection is one of:
 * <pre>
 *      [m]&gt;[n]
 * </pre>
 * @author gray
 *
 */
public class RedirectOutToken
    extends Token {

    private static final Logger LOG = Logger
            .getLogger(RedirectOutToken.class.getName());
    
    /**
     * The filedescriptor that we are redirecting out (fd 1, the default, 
     * represents standard out).
     */
    private int fd = 1;
    
    /**
     * Where we are re-directing the output.
     */
    private String filename;
    
    /**
     * Indicates whether or not we are appending to the output sync or
     * overwriting it..
     */
    private boolean isAppend = false;
    
    /**
     * Creates a redirection token.
     * 
     * @param line The line from which the token came.
     * @param position The position on the line from which the token came.
     * @param fd The file descriptor that is to be redirected from.
     * @param filename The file that we are redirecting into.
     * @param isAppend Whether or not the output is going to be append
     *   or created new.
     */
    public RedirectOutToken(String line, int position, 
            int fd, String filename,  boolean isAppend) {
        
        super(line, position);
        
        this.fd = fd;
        this.filename = filename;
        this.isAppend = isAppend;
    }
    
    /**
     * Returns the filename that we are redirecting to.
     * @return The filename that we are redirecting to.
     */
    public String getFilename () {
    
        return filename;
    }
    
    /**
     * Sets the name of the file that we are going to redirect to
     * 
     * @param filename The name of the file we are going to redirect to.
     */
    public void setFilename (String filename) {
    
        this.filename = filename;
    }
    
    /**
     * Returns the number of the file descriptor to be redirected.
     * 
     * @return the number of the file descriptor to be redirected.
     */
    public int getFd () {
    
        return fd;
    }

    /**
     * Sets the file descriptor to be redirected.
     * 
     * @param fd The number of the file descriptor that is to be
     *    redirected.
     */
    public void setFd (int fd) {
    
        this.fd = fd;
    }

    /**
     * Returns whether or not the operation is going to append to the output
     * or create a fresh file (or whatever is being redirected to).
     * 
     * @return Whether or not we are going to append.
     */
    public boolean isAppend () {
    
        return isAppend;
    }

    /**
     * Determines whether or not the output is going to be appended.
     * 
     * @param isAppend Set to true if you want to append.
     */
    public void setAppend (boolean isAppend) {
    
        this.isAppend = isAppend;
    }
    
    /**
     * Returns the redirection as a string.
     */
    public String toString() {
        
        StringBuilder sb = new StringBuilder(10);
        sb.append(fd)
            .append(isAppend ? ">>" : ">")
            .append(filename);
        
        return sb.toString();
    }
}

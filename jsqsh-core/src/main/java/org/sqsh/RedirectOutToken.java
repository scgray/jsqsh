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

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Used by {@link Tokenizer} to indicate that a file redirection has been encountered. A file redirection is one of:
 * <pre>
 *      [m]&gt;[n]
 * </pre>
 *
 * @author gray
 */
public class RedirectOutToken extends Token {

    private static final Logger LOG = Logger.getLogger(RedirectOutToken.class.getName());

    /**
     * The filedescriptor that we are redirecting out (fd 1, the default, represents standard out).
     */
    private final int fd;

    /**
     * Where we are re-directing the output.
     */
    private final String filename;

    /**
     * Indicates if we are appending to the output sync or overwriting it..
     */
    private final boolean isAppend;

    /**
     * Creates a redirection token.
     *
     * @param line The line from which the token came.
     * @param position The position on the line from which the token came.
     * @param fd The file descriptor that is to be redirected from.
     * @param filename The file that we are redirecting into.
     * @param isAppend Whether or not the output is going to be append or created new.
     */
    public RedirectOutToken(String line, int position, int fd, String filename, boolean isAppend) {
        super(line, position);
        this.fd = fd;
        this.filename = filename;
        this.isAppend = isAppend;
    }

    /**
     * Returns the filename that we are redirecting to.
     *
     * @return The filename that we are redirecting to.
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Returns the number of the file descriptor to be redirected.
     *
     * @return the number of the file descriptor to be redirected.
     */
    public int getFd() {
        return fd;
    }

    /**
     * Returns whether or not the operation is going to append to the output or create a fresh file (or whatever is
     * being redirected to).
     *
     * @return Whether or not we are going to append.
     */
    public boolean isAppend() {
        return isAppend;
    }

    /**
     * Returns the redirection as a string.
     */
    @Override
    public String toString() {
        return fd + (isAppend ? ">>" : ">") + filename;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RedirectOutToken that = (RedirectOutToken) o;
        return fd == that.fd && isAppend == that.isAppend && Objects.equals(filename, that.filename);
    }
}

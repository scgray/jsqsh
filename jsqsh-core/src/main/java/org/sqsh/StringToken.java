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
 * Represents an arbitrary string that was parsed from the command line.
 */
public class StringToken extends Token {
    private static final Logger LOG = Logger.getLogger(RedirectOutToken.class.getName());

    private final String str;

    /**
     * Creates a string token.
     *
     * @param line The line from which the token came.
     * @param position The position on the line from which the token came.
     * @param str The string represented by the token.
     */
    public StringToken(String line, int position, String str) {
        super(line, position);
        this.str = str;
    }

    /**
     * Returns the string that was parsed.
     *
     * @return the string that was parsed.
     */
    public String getString() {
        return str;
    }

    @Override
    public String toString() {
        return str;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StringToken that = (StringToken) o;
        return Objects.equals(str, that.str);
    }
}

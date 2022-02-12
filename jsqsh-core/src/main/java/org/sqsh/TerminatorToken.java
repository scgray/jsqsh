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

/**
 * Command line token to represent the terminator character.
 */
public class TerminatorToken extends Token {
    private final String terminator;

    public TerminatorToken(String line, int pos, char terminator) {
        super(line, pos);
        this.terminator = Character.toString(terminator);
    }

    public String getTerminator() {
        return terminator;
    }

    @Override
    public String toString() {
        return terminator;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TerminatorToken that = (TerminatorToken) o;
        return Objects.equals(terminator, that.terminator);
    }
}

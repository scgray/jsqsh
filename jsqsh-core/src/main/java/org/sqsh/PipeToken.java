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
 * The PipeToken represents a pipline command following the building sqsh command. That is, given the string:
 * <pre>
 *    hello how are you | blah blah
 * </pre>
 *
 * <p>
 * The "blah blah" is the portion of the command line that is considered the pipe.
 */
public class PipeToken extends Token {
    private final String pipeCommand;

    /**
     * Creates a command line pipe token.
     *
     * @param line The line from which the token came.
     * @param position The position on the line from which the token came.
     * @param pipeCommand The command that is to be piped to.
     */
    public PipeToken(String line, int position, String pipeCommand) {
        super(line, position);
        this.pipeCommand = pipeCommand;
    }

    /**
     * Returns the command that is to be piped to.
     *
     * @return the command that is to be piped to.
     */
    public String getPipeCommand() {
        return pipeCommand;
    }

    @Override
    public String toString() {
        return pipeCommand;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PipeToken pipeToken = (PipeToken) o;
        return Objects.equals(pipeCommand, pipeToken.pipeCommand);
    }
}

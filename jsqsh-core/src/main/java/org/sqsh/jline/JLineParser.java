/*
 * Copyright 2007-2017 Scott C. Gray
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
package org.sqsh.jline;

import org.jline.reader.EOFError;
import org.jline.reader.ParsedLine;
import org.jline.reader.SyntaxError;
import org.jline.reader.impl.DefaultParser;
import org.sqsh.SqshContext;

/**
 * The <code>JLineParser</code> is called by Jline in various contexts to either determine when
 * the input from a user is "complete" (that is, they have finished their current statement, such
 * as by hitting a terminator or a command), or to determine the current "world" during a tab
 * completion, or when JLine wants to find out what character is missing in the secondary prompt
 * (see {@link ParseContext}
 */
public class JLineParser extends DefaultParser {

    private SqshContext context;
    private EOFError INPUT_NOT_COMPLETE = new EOFError(0, 0, "Foo");

    public JLineParser(SqshContext context) {

        this.context = context;
    }

    @Override
    public ParsedLine parse(String s, int i, ParseContext parseContext) throws SyntaxError {

        switch (parseContext) {

            case ACCEPT_LINE:
                if (! context.getCurrentSession().isInputComplete(s, i))
                    throw INPUT_NOT_COMPLETE;
                // FALL-THROUGH
            default:
                return super.parse(s, i, parseContext);
        }
    }
}

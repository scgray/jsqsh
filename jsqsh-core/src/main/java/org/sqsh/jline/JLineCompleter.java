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
package org.sqsh.jline;

import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.sqsh.ConnectionContext;
import org.sqsh.Session;
import org.sqsh.SqshContext;
import org.sqsh.completion.Completer;

import java.util.List;

public class JLineCompleter implements org.jline.reader.Completer {
    final SqshContext ctx;

    public JLineCompleter(SqshContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void complete(LineReader lineReader, ParsedLine parsedLine, List<Candidate> list) {
        Session session = ctx.getCurrentSession();
        ConnectionContext conn = session.getConnectionContext();
        if (conn != null) {
            Completer completer = conn.getTabCompleter(session, parsedLine.line(), parsedLine.cursor(), parsedLine.word());
            String name = completer.next();
            while (name != null) {
                list.add(new Candidate(name));
                name = completer.next();
            }
        }
    }
}

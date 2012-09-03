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

import java.util.List;

import org.sqsh.ConnectionContext;
import org.sqsh.Session;
import org.sqsh.SqshContext;
import org.sqsh.input.completion.Completer;

public class JLineTabCompleter 
    implements jline.console.completer.Completer {
    
    static final String WORD_BREAK_CHARS = " \t,/.()<>=?'\":;$%&+-*[]^{}|";
    SqshContext ctx;
    
    public JLineTabCompleter (SqshContext ctx) {
        
        this.ctx = ctx;
    }

    @Override
    public int complete(String buffer, int cursor, 
            List<CharSequence> candidates) {
        
        int pos     = cursor - 1;
        String word = "";
        
        /*
         * The cursor is the position that the tab was struck. Since
         * we are assuming the user was actively typing a word before
         * they hit tab, the word that they where typing is behind
         * the current location.
         */
        while (pos >= 0
                && WORD_BREAK_CHARS.indexOf(buffer.charAt(pos)) < 0)
        {
            --pos;
        }
        
        /*
         * We either ended the loop above because we fell off the beginning
         * of the line of input or we are sitting on a word-break character.
         * So, the actual start is one character forward.
         */
        ++pos;
        
        if (pos < cursor) {
            
            word = buffer.substring(pos, cursor);
        }
        
        Session session = ctx.getCurrentSession();
        ConnectionContext conn = session.getConnectionContext();
        if (conn != null) {
            
            Completer completer = conn.getTabCompleter(session,
                buffer, cursor, word);
            String name = completer.next();
            while (name != null) {
                
                candidates.add(name);
                name = completer.next();
            }
        }

        return pos;
    }
    
}

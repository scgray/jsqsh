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
package org.sqsh.input;

import java.util.List;

import org.sqsh.Session;
import org.sqsh.SqshContext;
import org.sqsh.input.completion.Completer;
import org.sqsh.input.completion.DatabaseObjectCompleter;

import jline.Completor;

public class JLineTabCompleter 
    implements Completor {
    
    static final String WORD_BREAK_CHARS = " \t,/.()<>=?'\":;$%&+-*[]^{}|";
    SqshContext ctx;
    
    public JLineTabCompleter (SqshContext ctx) {
        
        this.ctx = ctx;
    }

    @Override
    public int complete(String buffer, int cursor, List candidates) {
        
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
        if (session.getConnection() != null) {
            
            Completer completer = new DatabaseObjectCompleter(session,
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

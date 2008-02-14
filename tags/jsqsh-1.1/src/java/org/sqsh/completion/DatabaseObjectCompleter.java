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
package org.sqsh.completion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sqsh.Session;
import org.sqsh.parser.SQLParser;
import org.sqsh.parser.DatabaseObject;

/**
 * This class implements tab completion of database objects based upon 
 * a rudamentary parsing of the current SQL buffer.  One of the main
 * limitations that it has is that it cannot <b>exactly</b> determine
 * where the cursor is sitting inside of the SQL buffer because readline
 * doesn't quite give us the ability to determine (or, more exactly, the
 * JNI interface to readline doesn't expose it).  The logic this implements
 * tries really really hard to determine where you are in the SQL but
 * may it get it wrong from time to time.
 */
public class DatabaseObjectCompleter
    extends Completer {
    
    private static final Logger LOG = 
        Logger.getLogger("org.sqsh.completion.DatabaseObjectCompleter");
    
    /**
     * This is a list of available completers. Each completer can identify
     * a statement and (optionally) a clause for which it applies. The first
     * matching completer based upon where the user is in the query will be
     * used.
     */
    private static final List<SQLStatementCompleter>
        STATEMENT_COMPLETERS = new ArrayList<SQLStatementCompleter>();
    
    static {
        
        STATEMENT_COMPLETERS.add(new QueryStatementCompleter("SELECT"));
        STATEMENT_COMPLETERS.add(new QueryStatementCompleter("INSERT"));
        STATEMENT_COMPLETERS.add(new QueryStatementCompleter("UPDATE"));
        STATEMENT_COMPLETERS.add(new QueryStatementCompleter("DELETE"));
        STATEMENT_COMPLETERS.add(
            new GenericStatementCompleter("USE", null, 
                GenericStatementCompleter.CATALOGS));
        
        STATEMENT_COMPLETERS.add(new ExecProcedureStatementCompleter());
    }
    
    /**
     * Used internally to keep track of what sort of quoting was taking
     * place on the object name that the user was completing.
     */
    private enum QuoteType { NONE, BRACKET, QUOTES };
    
    /**
     * The SQL statement that the user was building.
     */
    private String sql;
    private QuoteType quote = QuoteType.NONE;
    private Iterator<String> iter = null;
    
    public DatabaseObjectCompleter(Session session,
            String line, int position, String word) {
        
        super(session, line, position, word);
        
        if (LOG.isLoggable(Level.FINE)) {

            LOG.fine("Completing '"
                + line + "' at position "
                + position + ", current word '" + word + "'");
        }
        
        /*
         * Take a look at the first character in the word that the user is
         * sitting on. If it is a SQL object quote, then we will retain that
         * information to ensure that all completions are completed with
         * the quotes left in place.
         */
        char ch = (word.length() == 0 ? (char) 0 : word.charAt(0));
        if (ch == '[') {
            
            quote = QuoteType.BRACKET;
        }
        else if (ch == '"') {
            
            quote = QuoteType.QUOTES;
        }
        else {
            
            quote = QuoteType.NONE;
        }
        
        /*
         * First we will blast through the current line of input up to
         * the point at which the user hit the completion key and look for
         * the name of the object that the user was sitting on. This
         * name will be returned as its component parts.
         */
        String nameParts[] = getObjectName(line.substring(0, position));
        
        if (LOG.isLoggable(Level.FINE)) {

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < nameParts.length; i++) {
                
                if (i > 0) {
                    
                    sb.append('.');
                }
                sb.append('[');
                sb.append(nameParts[i]);
                sb.append(']');
            }
            
            LOG.fine("   Parsed object name is: " + sb.toString());
        }
        
        /*
         * Our total SQL statement is what the user has entered to date
         * plus the line of input that the user is working on.
         */
        sql = session.getBufferManager().getCurrent().toString() + line;
        
        /*
         * Now, we will parse the SQL and accumulate information about the
         * statement that the user is currently working on.
         */
        SQLParseState info = new SQLParseState();
        SQLParser parser = new SQLParser(info);
        parser.parse(sql);
        
        if (LOG.isLoggable(Level.FINE)) {

            LOG.fine("   Parsing state:");
            LOG.fine("      Statement  = " + info.getStatement());
            LOG.fine("      Clause     = " + info.getCurrentClause());
            
            StringBuilder sb = new StringBuilder();
            int idx = 0;
            for (DatabaseObject o : info.getObjectReferences()) {
                
                if (idx > 0) {
                    
                    sb.append(", ");
                }
                ++idx;
                
                sb.append(o.toString());
            }
            LOG.fine("      References = " + sb.toString());
        }
        
        /*
         * Grab ahold of the tables that are being referenced by the
         * current SQL statement.
         */
        DatabaseObject []tableRefs = info.getObjectReferences();
        Set<String> completions = new TreeSet<String>();
        
        /*
         * Now that we have everything we need we will begin searching
         * for which completer to utilize.
         */
        if (info.getStatement() != null) {
            
            for (SQLStatementCompleter completer : STATEMENT_COMPLETERS) {
                
                if (completer.getStatement().equals(info.getStatement())
                    && (completer.getClause() == null
                       || completer.getClause().equals(info.getCurrentClause()))) {
                    
                    completer.getCompletions(completions, 
                        session.getConnection(), nameParts, info);
                    break;
                }
            }
        }
        
        
        if (completions.size() > 0) {
            
            iter = completions.iterator();
        }
        else {
            
            iter = null;
        }
    }
    
    /**
     * Returns the next string available in the completion list.
     * @return The next string in the completion list or null if no
     *   more strings are available.
     */
    public String next() {
        
        if (iter == null || iter.hasNext() == false) {
                
            return null;
        }
            
        String s = iter.next();
        if (quote == QuoteType.BRACKET) {
                
            return "[" + s + "]";
        }
        else if (quote == QuoteType.QUOTES) {
                
            return '"' + s + '"';
        }
            
        return s;
    }
    
    /**
     * This method walks a line of input from the user up to the point
     * where their cursor is sitting and tries to figure out if the cursor
     * is sitting on an object name. If the cursor is on an object name
     * then the name of the object will be returned.
     * 
     * @param line The line to be processed.
     * @return The object name broken into its component parts. If there
     * was no object found an empty list will be returned.
     */
    protected String[] getObjectName(String line) {
        
        int idx = 0;
        int len = line.length();
        List<String> parts = new ArrayList<String>();
        
        /*
         * Ok, here's what we are doing. When we hit a 
         * word, "quoted word", or [bracketed word] we take that
         * word and append it to our list of object name parts and
         * we continue to do so as long as there is a '.' between
         * those word. As soon as we hit something that is not a '.'
         * we clear out our current list and keep moving on.
         */
        while (idx < len) {
            
            char ch = line.charAt(idx);
            if (Character.isLetter(ch)
                    || ch == '_'
                    || ch == '@') {
                
                idx = doWord(parts, line, idx);
            }
            else if (ch == '"') {
                
                idx = doQuotedWord(parts, line, idx);
            }
            else if (ch == '[') {
                
                idx = doBracketedWord(parts, line, idx);
            }
            else if (ch == '.') {
                
                ++idx;
                if (idx == len || line.charAt(idx) == '.') {
                    
                    parts.add("");
                }
            }
            else {
                
                if (parts.size() > 0) {
                    
                    parts.clear();
                }
                
                ++idx;
            }
        }
        
        /*
         * Before we return our list of name parts, I want to convert
         * all of the empty parts to nulls. I do this because I perfer
         * code that does 
         *    if (nameParts[x] == null)
         * over 
         *    if (nameParts[x].equals(""))
         * but thats just me. 
         */
        String []nameParts = parts.toArray(new String[0]);
        for (int i = 0; i < nameParts.length; i++) {
            
            if (nameParts[i].length() == 0) {
                
                nameParts[i] = null;
            }
        }
        
        return nameParts;
    }
    
    /**
     * Called by doFindObject() when it has landed on the beginning of 
     * a "word" in a line.
     * 
     * @param parts The list of object parts that we have found thus far.
     *   The newly parsed word will be appended to this list.
     * @param line The line being parsed.
     * @param idx The index of the first character of the word.
     * @return The index after parsing.
     */
    private int doWord(List<String> parts, String line, int idx) {
        
        StringBuilder word = new StringBuilder();
        int len = line.length();
        word.append(line.charAt(idx));
                    
        /*
         * Skip forward until we hit the likely end.
         */
        ++idx;
        boolean done = false;
        while (!done) {
            
            if (idx >= len) {
                
                done = true;
            }
            else {
                
                char ch = line.charAt(idx);
                if (Character.isLetter(ch)
                            || Character.isDigit(ch)
                            || ch == '_') {
                    
                    word.append(ch);
                    ++idx;
                }
                else {
                    
                    done = true;
                }
            }
        }
        
        parts.add(word.toString());
        return idx;
    }
    
    /**
     * Called by doFindObject() when it has landed on the beginning of 
     * a quoted word.
     * 
     * @param parts The list of object parts that we have found thus far.
     *   The newly parsed word will be appended to this list.
     * @param line The line being parsed.
     * @param idx The index of the first character of the word.
     * @return The index after parsing.
     */
    private int doQuotedWord(List<String> parts, String line, int idx) {
        
        StringBuilder word = new StringBuilder();
        int len = line.length();
                    
        /*
         * Skip forward until we hit the likely end.
         */
        ++idx;
        boolean done = (idx >= len);
        while (idx < len && !done) {
                        
            char ch = line.charAt(idx);
            if (ch == '"') {
                
                ++idx;
                if (idx < (len - 1) && line.charAt(idx+1) == '"') {
                    
                    word.append('"');
                }
                else {
                    
                    done = true;
                }
            }
            else {
                
                word.append(ch);
                ++idx;
            }
        }
        
        parts.add(word.toString());
        return idx;
    }
    
    /**
     * Called by doBracketedWord() when it has landed on the beginning of 
     * a bracketed word.
     * 
     * @param parts The list of object parts that we have found thus far.
     *   The newly parsed word will be appended to this list.
     * @param line The line being parsed.
     * @param idx The index of the first character of the word.
     * @return The index after parsing.
     */
    private int doBracketedWord(List<String> parts, String line, int idx) {
        
        StringBuilder word = new StringBuilder();
        int len = line.length();
                    
        /*
         * Skip forward until we hit the likely end.
         */
        ++idx;
        boolean done = (idx >= len);
        while (idx < len && !done) {
                        
            char ch = line.charAt(idx);
            if (ch == ']') {
                
                ++idx;
                done = true;
            }
            else {
                
                ++idx;
                word.append(ch);
            }
        }
        
        parts.add(word.toString());
        return idx;
    }
}

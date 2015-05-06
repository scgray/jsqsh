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
package org.sqsh.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sqsh.SQLIdentifierNormlizer;
import org.sqsh.SQLTools;

import static org.sqsh.input.completion.CompletionCandidate.*;


/**
 * The SQLParser is a very very rudamentary parser for traversing a SQL
 * statement or batch of SQL statements looking for "interesting" items
 * that may be found in the SQL. The parser does not fully understand
 * any SQL dialect, but recognizes many keywords and aspects of queries.
 */
public class SQLParser {
    
    /*
     * NOTES ON HOW THINGS WORK
     * 
     * The code in here isn't necessarily that easy to follow, so here are 
     * a few notes about how things are implemented that may help.
     * 
     * First, this isn't a "real" parser, so there is no such thing as a 
     * syntax error and I need to be able to deal with stuff like:
     * 
     *      select * from (select count(*) from), t1 ..
     * 
     * I want to be able to pick up on the fact that "t1" is a referenced
     * table in the query despite the fact that the derived table is 
     * syntactically incorrect, so you'll see a creative use of exceptions
     * to try to get around this.
     * 
     * Just about any time an open parenthesis is read, I ask the tokenizer
     * what the current nesting level is for parenthesis, and call
     * 
     *    triggerOnParenLevel(<level>);
     *    
     * this tells the method nextToken() that should it read a closing paren
     * and should that paren close the registered nesting level, it should
     * throw the exception HitClosingParen. Thus the open paren handling code
     * can catch that exception, and the underlying parsing code for the
     * contents of the parens can just silently go along parsing, not worrying
     * about the issue.
     * 
     * A similar trick is done for SQL dialects that utilize a semicolon. A 
     * semicolon is assumed to be a statement terminator, so whenever nextToken()
     * is called and it gets a semicolon, it throws a HitSemiColon which 
     * throws control directly back up to the logic that is parsing individual
     * statements.
     */
    
    private static final Logger LOG = Logger.getLogger(SQLParser.class.getName());
    
    /**
     * The listener for parse events.
     */
    private SQLParserListener listener = null;
    private SQLIdentifierNormlizer normalizer = null;
    private SimpleSQLTokenizer tokenizer = null;
    
    private Stack<Integer> parenMarkers = new Stack<Integer>();
    private int triggerParenLevel = -1;
    
    /**
     * Creates a new parser.
     * 
     * @param ctx The connection being used. This is used to determine things
     *   like platform normalization rules.
     * @param listener The object to be notified when the parser hits
     *   "interesting" points in the SQL.
     */
    public SQLParser (SQLIdentifierNormlizer normlizer, SQLParserListener listener) {
        
        this.normalizer = normlizer;
        this.listener = listener;
    }
    
    /**
     * Method made available to SQLParserListener's to request a handle
     * to the tokenizer used for parsing. It is important to note that 
     * any tokens consumed by this tokenizer will no longer be available
     * to the SQLParser unless you unget them.
     * 
     * @return The tokenizer used for parsing.
     */
    public SimpleSQLTokenizer getTokenizer() {
        
        return tokenizer;
    }
    
    /**
     * Performs a rudimentary parsing of a supplied block of SQL.
     * 
     * @param sql The sql being parsed.
     */
    public void parse (String sql) {
        
        tokenizer = new SimpleSQLTokenizer(sql);
        String token = tokenizer.next();
        
        try {
        
            /*
             * This loop looks over statements at the top level.
             */
            while (token != null) {
                
                try {
                    
                    /*
                     * Safety net. Ensure that we think we are at the top level of parsing
                     * in case something went wrong.
                     */
                    if (triggerParenLevel >= 0) {
                        
                        LOG.fine("Parenthesis trigger level should have been -1, current is "
                                + triggerParenLevel);
                        triggerParenLevel = -1;
                        parenMarkers.clear();
                    }
                    
                    int len = token.length();
                    
                    /*
                     * Could be a subquery. 
                     */
                    if (len == 1 && token.charAt(0) == '(') {
                        
                        doParen(null);
                    }
                    else if (len == 4) {
                        
                        char ch = token.charAt(0);
                        if ((ch == 'e' || ch == 'E') && "EXEC".equalsIgnoreCase(token)) {
                            
                            listener.foundStatement(this, "EXECUTE");
                            doExecuteProcedure();
                        }
                        else if ((ch == 'c' || ch == 'C') && "CALL".equalsIgnoreCase(token)) {
                            
                            listener.foundStatement(this, "CALL");
                            doCall();
                        }
                    }
                    else if (len == 6) {
                        
                        // Make it upper case
                        char ch = token.charAt(0);
                        if (ch >= 'a' && ch <= 'z') {
                
                            ch = (char) (((int) ch) - 32);
                        }
                        
                        if (ch == 'S' && "SELECT".equalsIgnoreCase(token)) {
                        
                            listener.foundStatement(this, "SELECT");
                            doSelect();
                        }
                        else if (ch == 'D' && "DELETE".equalsIgnoreCase(token)) {
                        
                            listener.foundStatement(this, "DELETE");
                            doDelete();
                        }
                        else if (ch == 'U' && "UPDATE".equalsIgnoreCase(token)) {
                        
                            listener.foundStatement(this, "UPDATE");
                            doUpdate();
                        }
                        else if (ch == 'I' && "INSERT".equalsIgnoreCase(token)) {
                        
                            listener.foundStatement(this, "INSERT");
                            doInsert();
                        }
                    }
                    else if (len == 7) {
                        
                        char ch = token.charAt(0);
                        if ((ch == 'e' || ch == 'E') && "EXECUTE".equalsIgnoreCase(token)) {
                            
                            listener.foundStatement(this, "EXECUTE");
                            doExecuteProcedure();
                        }
                    }
                }
                catch (HitSemiColon e) {
                    
                    // Ignored
                }
                
                token = tokenizer.next();
            }
        }
        catch (Exception e) {
            
            /*
             * Safety net. Don't puke back to the user if we have a bug below.
             */
            LOG.log(Level.FINE, "Exception during parse: " + e.getMessage(), e);
        }
        
        tokenizer = null;
    }
    
    /**
     * Called when an outer-most SELECT statement has been hit (not a subquery)
     */
    private void doSelect () {
        
        // Parse out the FROM clause
        if (seekToKeyword("FROM")) {
            
            doFrom();
        }
    }
    
    /**
     * Called after the tokenizer hits an INSERT statement and attempts
     * to determine the table name that is being updated or inserted into.
     * Note that we treat INSERT...SELECT basically as two separate statements.
     * 
     * @param tableRefs The list of references found thus far.
     * @param tokenizer The current parsing state.
     */
    private void doInsert() {
        
        String token = nextToken();
        if ("INTO".equalsIgnoreCase(token)) {
            
            token = nextToken();
        }
        
        if (token != null) {
            
            listener.foundTableReference(this, doObjectReference(token));
        }
    }
    
    /**
     * Called after the tokenizer hits an UPDATE statement and attempts
     * to determine the table name that is being updated.
     * Note that we treat INSERT...SELECT basically as two separate statements.
     * 
     * @param tableRefs The list of references found thus far.
     * @param tokenizer The current parsing state.
     */
    private void doUpdate() {
        
        String token = nextToken();
        
        /*
         * T-SQL extension: UPDATE TOP N...
         */
        if ("TOP".equalsIgnoreCase(token)) {
            
            // Skip N
            token = nextToken();
            // Grab the next keyword
            token = nextToken();
        }
        
        /*
         * If we are updating a derived table like:
         *    UPDATE (SELECT ..)
         * then I cheat and use doFrom() read the query
         */
        if ("(".equals(token)) {
            
            tokenizer.unget(token);;
            doFrom(Boolean.FALSE, false);
        }
        else {
            
            listener.foundTableReference(this, doObjectReference(token));
        }
        
        /*
         * Allow for the T-SQL version that also allows a FROM clause
         */
        if (seekToKeyword("FROM")) {
            
            doFrom(Boolean.FALSE, true);
        }
    }
    
    /**
     * Called after the tokenizer hits a DELETE statement and attempts
     * to determine the table name that is being deleted.
     * 
     * @param tableRefs The list of references found thus far.
     * @param tokenizer The current parsing state.
     */
    private void doDelete() {
        
        String token = nextToken();
        if (token == null) {
            
            return;
        }
        
        /*
         * T-SQL allows for:
         *   DELETE TOP N [FROM] ...
         * if we see TOP N, then skip it.
         */
        if ("TOP".equalsIgnoreCase(token)) {
            
            // Skip the N
            token = nextToken();
            
            // Grab the following token
            token = nextToken();
        }
        
        /*
         * In T-SQL the FROM is optional, so we could have just
         *   DELETE T1 [, T2, ...]
         */
        if (! "FROM".equalsIgnoreCase(token)) {
            
            tokenizer.unget(token);
        }
        
        /*
         * Ok, here are the forms of DELETE we should be prepared for:
         *    DELETE FROM <table>            -- Standard SQL
         *    DELETE FROM (SELECT ..)        -- Oracle
         *    DELETE [FROM] <table>, <table> -- T-SQL
         * At this point we have just processed the "FROM" or recognized
         * that it is missing.  So, now we just treat it like a 
         * regular FROM clause (without ANSI join facilities). The
         * doFrom will automatically stop at the WHERE clause.
         */
        doFrom(Boolean.FALSE, true);
    }
    
    
    /**
     * Called to process a FROM clause
     */
    private final void doFrom() {
        
        doFrom(null, true);
    }
    
    /**
     * This is called either just after a FROM has been encountered or after 
     * consuming a join operator in a FROM clause.  For example, it would be
     * invoked here:
     * <pre>
     *    FROM &lt;here&gt;
     * </pre>
     * or
     * <pre>
     *    FROM T1, &lt;here&gt;
     * </pre>
     * or
     * <pre>
     *    FROM T1 LEFT OUTER JOIN &lt;here&gt;
     * </pre>
     * 
     */
    private void doFrom(Boolean isAnsiJoin, boolean allowsMultipleTables) {
        
        String token = nextToken();
        
        boolean done = false;
        
        listener.setEditingTableReference(true);
        
        /*
         * Consume tokens until we hit a clause or the next statement
         */
        while (token != null && ! done) {
                
            DatabaseObject obj = null;
            
            /*
             * We could have either a 
             *    (SELECT ..)
             * or we could have
             *    ((T1 JOIN T2) JOIN ...)
             * or we can have
             *    TABLE(...)
             * or we can have
             *    T1
             */
            if ("(".equals(token)) {
                    
                /*
                 * We have a table expression, like: (SELECT ..)
                 */
                String nextToken = tokenizer.peek();
                if (isQuery(nextToken)) {
                        
                    /*
                     * Process the body of the paren
                     */
                    listener.setEditingTableReference(false);
                    
                    List<String> projection = new ArrayList<String>();
                    
                    /*
                     * We only resume editing table references if we found the 
                     * closing parenthesis.
                     */
                    if (doParen(projection)) {
                    
                        listener.setEditingTableReference(true);
                    }
                        
                    /*
                     * We can only turn the select into an object if there is a
                     * name following the query.
                     */
                    if (tokenizer.peek() != null && projection.size() > 0) {
                            
                        obj = new DatabaseObject("", projection);
                    }
                }
                else {
                    
                    /*
                     * Anything else following the open paren indicates an ANSI
                     * JOIN, so just enter that code path and be done with it.
                     */
                    triggerOnParenLevel(tokenizer.getParenCount()-1);
                    try {
                            
                        /*
                         * Ok, we could have:
                         *   ((T1 LEFT JOIN
                         * or 
                         *   (T1 LEFT JOIN
                         * or something like that, so we'll just recurse in, but now
                         * I know that I have an outer join for sure.
                         */
                        doFrom(true, allowsMultipleTables);
                        isAnsiJoin = true;
                            
                        /*
                         * We shouldn't hit this point, however if we do, then we just
                         * keep absorbing tokens until we hit the end or trigger the
                         * closing parens;
                         */
                        seekToTriggeringParen();
                    }
                    catch (HitClosingParen e) {
                            
                        // Ignored 
                    }
                }
            }
            else if ("TABLE".equalsIgnoreCase(token) && "(".equals(tokenizer.peek())) {
                    
                /*
                 * We have a TABLE function call, like:
                 *    FROM TABLE(FUNC_CALL());
                 * so, read the next parenthesis we just peeked at, then consume
                 * the contents of the parenthesis.
                 */
                nextToken();
                listener.setEditingTableReference(false);
                
                /*
                 * We only resume editing table references if we found the 
                 * closing parenthesis.
                 */
                if (doParen(null)) {
                    
                    listener.setEditingTableReference(true);
                }
            }
            else {
                    
                /*
                 * We have a basic object name
                 */
                obj = doObjectReference(token);
            }
            
            /*
             * Ok, now we have to delicate. We want to walk ahead to see if we have
             * a table alias, which means we could have:
             * 
             *   T1 A
             * 
             * or 
             * 
             *   T1 AS A
             *   
             * or 
             *  
             *   (SELECT ..) AS A
             *
             * But, I have to be careful because I could have
             * 
             *   T1 LEFT JOIN 
             *   
             * and I don't want T1 to be aliased as "LEFT". In addition if I had:
             * 
             *   T1)
             * 
             * I don't want the close paren to cause me to discard the fact that
             * I just read T1.
             */
            try {
                    
                boolean hasAs = false;
                token = nextToken();
                if ("AS".equalsIgnoreCase(token)) {
                        
                    hasAs = true;
                    token = nextToken();
                }
                    
                Boolean isJoin;
                    
                /*
                 * Have we hit the next join?
                 */
                if (hasAs == false 
                    && allowsMultipleTables 
                    && (isJoin = isNextJoin(token, isAnsiJoin)) != null) {
                        
                    isAnsiJoin = isJoin;
                    token = nextToken();
                }
                else if (hasAs == false 
                    && (isSelectClause(token) || isStatement(token))) {
                        
                    listener.setEditingTableReference(false);
                    done = true;
                }
                else {
                        
                    /*
                     * We aren't at the next join, if the token is an identifier then
                     * we have hit an alias for this object.
                     */
                    String alias = null;
                    List<String> columns = null;
                    
                    if (SQLTools.isIdentifier(token) 
                        && (hasAs || ! "ON".equalsIgnoreCase(token))) {
                            
                        alias = token;
                        token = nextToken();
                            
                        /*
                         * Do we have a projection list for our alias?
                         */
                        if ("(".equals(token)) {
                                
                            columns = new ArrayList<String>();
                            token = nextToken();
                            while (token != null && ! ")".equals(token)) {
                                    
                                if (SQLTools.isIdentifier(token)) {
                                        
                                    columns.add(token);
                                }
                                    
                                token = nextToken();
                            }
                                
                            /*
                             * Grab the token after the close paren.
                             */
                            token = nextToken();
                        }
                            
                        /*
                         * We could have had (SELECT ..) AS T1 (A, B, C), so we
                         * need to fabricate an object for that.
                         */
                        if (obj == null) {
                                
                            obj = new DatabaseObject(normalizer.normalize(alias), columns);
                        }
                        else {
                                
                            if (alias != null) {
                                    
                                obj.setAlias(normalizer.normalize(alias));
                            }
                            if (columns != null) {
                                    
                                obj.setColumnList(columns);
                            }
                        }
                    }
                        
                    /*
                     * At this point I'll just seek forward to the next join, a select
                     * clause or a new statement.
                     */
                    if (allowsMultipleTables) {
                            
                        Boolean join;
                        while (token != null) {
                            
                            /*
                             * While we are in an ON clause expand the completions to allow
                             * for column references. Also we do not count this as currently
                             * editing the FROM clause either, so the user's completions are
                             * narrowed down to only objects in scope.
                             */
                            if (isAnsiJoin == Boolean.TRUE 
                                && "ON".equalsIgnoreCase(token)) {
                                
                                /*
                                 * Inside of the ON clause we treat it like a WHERE
                                 * clause where you can see only referenced tables.
                                 */
                                listener.setEditingTableReference(false);
                                token = nextToken();
                            }
                            else {
                            
                                join = isNextJoin(token, isAnsiJoin);
                                if (join != null) {
                                    
                                    /*
                                     * In case we were in an ON clause, we record that
                                     * we are no longer in it...
                                     */
                                    listener.setEditingTableReference(true);
                                    isAnsiJoin = join;
                                    token = nextToken();
                                    break;
                                }
                                else if (isStatement(token) || isSelectClause(token)) {
                                    
                                    /*
                                     * If we finished parsing because we hit the next
                                     * clause or statement, then we are no longer
                                     * editing table references.
                                     */
                                    listener.setEditingTableReference(false);
                                    done = true;
                                    break;
                                }
                                
                                token = nextToken();
                            }
                        }
                    }
                    else {
                            
                        done = true;
                    }
                }
            }
            finally {
                    
                if (obj != null) {
                        
                    listener.foundTableReference(this, obj);
                }
            }
        }
            
        /*
         * Make sure we don't leave the stopping token consumed.
         */
        tokenizer.unget(token);
    }
    
    /**
     * Called at any point that we have just hit an open parenthesis to parse
     * the contents. If the parenthesis consist of a SELECT statement, then it
     * is parsed as a subquery, any other contents are silently ignored, and
     * any nested parenthesis are handled in the same fashion.
     * 
     * @param projection If not null, this will be populated with the list of
     *   projection columns that could be computed from the select list.
     * 
     * @return true if the paren block was properly closed, false if EOF was hit
     *   before it could be reached.
     */
    private boolean doParen (List<String> projection) {
        
        boolean isClosed = false;
        
        // Paren was was already read.
        int parenLevel = tokenizer.getParenCount() - 1;
        boolean isSubquery = false;
        
        triggerOnParenLevel(parenLevel);
        try {
            
            String token = nextToken();
            if ("SELECT".equalsIgnoreCase(token)) {
                
                isSubquery = true;
                listener.enteredSubquery(this);
                
                // Attempt to consume the select list.
                doSelectList(projection);
                
                if (seekToKeyword("FROM")) {
            
                    doFrom();
                }
            }
            else if (token.length() > 0 && token.charAt(0) == '(') {
                
                doParen(null);
            }
            
            // If we reached this point, we either didn't have a SELECT or
            // the subquery was not fully processed (like it never hit the
            // closing parenthesis. So, we just keep sucking tokens until 
            // we hit EOF or get a HitClosingParen error.
            seekToTriggeringParen();
        }
        catch (HitClosingParen e) {
            
            /*
             * Only exit the subquery if we actually hit the closing paren.
             * If we don't this this point, then presumably it is because we
             * are at the end of input.
             */
            if (isSubquery) {
            
                listener.exitedSubquery(this);
            }
            
            isClosed = true;
        }
        
        return isClosed;
    }
    
    /**
     * Called immediately after hitting the SELECT keyword, this attempts to 
     * determine the project from the select list. The logic is a bit hairy
     * an error prone.
     * 
     * @param projection Where to place columns found
     * @return The list of column names that could be gleened from the SQL.
     */
    private void doSelectList(List<String> projection) {
        
        String token = nextToken();
        
        /*
         * SELECT A, A as B, A+B, A + B as D
         */
        
        /*
         * Keep moving until we hit a clause (preferably a FROM clause).
         * Note that we will trip up on tables that have identifiers that
         * are the same as a select clause
         * 
         * The logic here is awfully crude and error prone, but it does like
         * this:  I look at the last token I received from before a comma and
         * assume that is the column name.  This works fine for:
         * 
         *     A
         *     A AS B
         *     (A * 2) + C B
         *     
         * But for the case of:
         *    
         *     A + B
         * 
         * I'll think the column is called "B".  Oh well.
         */
        String prevToken = null;
        
        try {
            
            while (token != null && ! isSelectClause(token)) {
                
                if (",".equals(token)) {
                    
                    if (projection != null 
                        && prevToken != null 
                        && SQLTools.isIdentifier(prevToken)) {
                        
                        projection.add(normalizer.normalize(prevToken));
                    }
                    prevToken = null;
                }
                else if ("(".equals(token)) {
                    
                    doParen(null);
                    prevToken = null;
                }
                else {
                    
                    prevToken = token;
                }
                
                token = nextToken();
            }
        }
        finally {
        
            if (projection != null
                && prevToken != null && SQLTools.isIdentifier(prevToken)) {
                    
                projection.add(normalizer.normalize(prevToken));
            }
        }
        
        /*
         * Leave the clause token in tact.
         */
        tokenizer.unget(token);
    }
    
    /**
     * Seeks forward, looking for a specific keyword. 
     * @param keyword The keyword to seek to
     * @return true if the keyword was found, false if it wasn't found and
     *   we are currently at EOF
     */
    private boolean seekToKeyword (String keyword) {
        
        String token = nextToken();
        while (token != null && ! keyword.equalsIgnoreCase(token)) {
            
            /*
             * Stop searching if we hit the next statement
             */
            if (isStatement(token)) {
                
                return false;
            }
            
            /*
             * If we hit an open paren, check to see if it was a subquery that
             * needs to be processed.
             */
            if ("(".equals(token)) {
                
                doParen(null);
            }
            
            token = nextToken();
        }
        
        if (token == null) {
            
            return false;
        }
        
        return true;
    }
    
    /**
     * Removes a previously recorded parenthesis level.
     * @param parenLevel The level to remove
     */
    private void removeTriggerOnParenLevel(int parenLevel) {
        
        if (triggerParenLevel != parenLevel) {
            
            LOG.fine("Attempting remove incorrect parenthesis level: " + parenLevel);
            return;
        }
        
        if (parenMarkers.size() == 0) {
            
            triggerParenLevel = -1;
        }
        else {
            
            triggerParenLevel = parenMarkers.pop();
        }
    }
    
    /**
     * Registers a parenthesis nesting level which, when nextToken() hits
     * upon reading a closing parenthesis, will cause a HitClosingParen
     * exception to be thrown
     * 
     * @param parenLevel The level to trigger.
     */
    private void triggerOnParenLevel(int parenLevel) {
        
        if (LOG.isLoggable(Level.FINE)) {
            
            LOG.fine("Registering notification upon leaving paren level " + parenLevel);
        }
        
        if (triggerParenLevel >= 0) {
            
            parenMarkers.push(triggerParenLevel);
        }
        
        triggerParenLevel = parenLevel;
    }
    
    /**
     * This is called when inside a set of parenthesis that has been set to 
     * trigger a HitClosingParen, it seeks forward until the closing paren
     * is hit (triggering the exception) or until the end of input is reached.
     * When it returns the triggering parenthesis is removed.
     * 
     * @param parentLevel
     */
    private void seekToTriggeringParen() {
        
        int triggeringLevel = this.triggerParenLevel;
        
        String token;
        while ((token = nextToken()) != null) {
            
            if (token.length() > 0 && token.charAt(0) == '(') {
                
                doParen(null);
            }
        }
        
        /*
         * If we hit end of input, then we'll reach this line of code, so we
         * just remove the triggering paren level and return.
         */
        removeTriggerOnParenLevel(triggeringLevel);
    }
    
    /**
     * Retrieves a token. If that token is a closing parenthesis and the 
     * paren count matches of registered parenthesis level, then HitClosingParen
     * is thrown
     * 
     * @return The next token
     */
    private String nextToken () {
        
        String token = tokenizer.next();
        if (token == null) {
            
            return null;
        }
        
        int len = token.length();
        if (len == 1) {
            
            char ch = token.charAt(0);
            if (ch == ')') {
            
                if (tokenizer.getParenCount() == triggerParenLevel) {
                
                    if (LOG.isLoggable(Level.FINE)) {
                        
                        LOG.fine("Notifying of exit from paren level " + triggerParenLevel);
                    }
                
                    if (parenMarkers.size() > 0) {
                    
                        triggerParenLevel = parenMarkers.pop();
                    }
                    else {
                    
                        triggerParenLevel = -1;
                    }
                
                    throw CLOSING_PAREN_ERROR;
                }
            }
            else if (ch == ';') {
                
                throw SEMICOLON_ERROR;
            }
        }
        
        return token;
    }
    
    
    /**
     * Checks the current token position to see if we are at the 
     * next JOIN in a FROM clause. 
     * 
     * @param token The token just read
     * @param isAnsiJoin If true, then we know this is an ANSI join
     *    if false, then we know it is an old-style join. If null then
     *    the type of join is not yet known.
     * @return null if it is not the next join clause, otherwise TRUE
     *    is returned for an ANSI join and FALSE for an old style join.
     */
    private Boolean isNextJoin(String token, Boolean isAnsiJoin) {
        
        if (isAnsiJoin == null) {
            
            if (isAnsiJoin(token, tokenizer)) {
                
                return Boolean.TRUE;
            }
            else if (",".equals(token)) {
                
                return Boolean.FALSE;
            }
        }
        else if (isAnsiJoin == Boolean.TRUE && isAnsiJoin(token, tokenizer)) {
            
            return Boolean.TRUE;
        }
        else if (",".equals(token)) {
            
            return Boolean.FALSE;
        }
        
        return null;
    }
    
    private boolean isQuery(String token) {
        
        // Need to process a WITH common table expression
        return "SELECT".equalsIgnoreCase(token);
    }
    
    /**
     * Checks to see if we have hit a keyword in our parsing that indicates that
     * we are no longer parsing the FROM clause.  The recognized clauses are:
     * <pre>
     *   FROM
     *   WHERE
     *   GROUP BY
     *   ORDER BY
     *   HAVING
     *   FETCH FIRST
     *   UNION
     *   INTERSECT
     *   MINUS
     *   COMPUTE
     *   AT ISOLATION
     *   FOR READ
     *   FOR UPDATE
     *   FOR BROWSE
     * </pre>
     * 
     * This method does not adjust the current position in the tokenizer.
     * 
     * @param token The current token just read
     * @param tokenizer Used to peek forward if we need to
     * @return true if we have hit a keyword that follows the FROM clause, false otherwise.
     */
    private boolean isSelectClause(String token) {
        
        if (token == null) {
            
            return false;
        }
        
        int len = token.length();
        if (len == 2) {
            
            char ch = token.charAt(0);
            if (ch == 'a' || ch == 'A') {
                
                ch = token.charAt(1);
                if (ch == 't' || ch == 'T') {
                    
                    String next = tokenizer.peek();
                    if (next != null && next.length() == 9
                        && "ISOLATION".equalsIgnoreCase(next)) {
                        
                        return true;
                    }
                }
            }
        }
        if (len == 3) {
            
            char ch = token.charAt(0);
            if (ch == 'f' || ch == 'F') {
                
                ch = token.charAt(1);
                if (ch == 'o' || ch == 'O') {
                    
                    ch = token.charAt(2);
                    if (ch == 'r' || ch == 'R') {
                        
                        String next = tokenizer.peek();
                        
                        if (next != null) {
                            
                            int nextLen = next.length();
                            if ((nextLen == 4 && "READ".equalsIgnoreCase(next))
                               || (nextLen == 6 && 
                                    ("UPDATE".equalsIgnoreCase(next)
                                       || "BROWSE".equalsIgnoreCase(next)))) {
                                
                                return true;
                            }
                        }
                    }
                }
            }
        }
        else if (len == 4) {
            
            char ch = token.charAt(0);
            if ((ch == 'f' || ch == 'F') && "FROM".equalsIgnoreCase(token)) {
                
                return true;
            }
        }
        else if (len == 5) {
            
            char ch = token.charAt(0);
            
            // Make it upper case
            if (ch >= 'a' && ch <= 'z') {
                
                ch = (char) (((int) ch) - 32);
            }
            
            if (ch == 'W' && "WHERE".equalsIgnoreCase(token)) {
                
                return true;
            }
            else if (ch == 'F' && "FETCH".equalsIgnoreCase(token)) {
                
                if ("FIRST".equalsIgnoreCase(tokenizer.peek())) {
                    
                    return true;
                }
            }
            else if (ch == 'U' && "UNION".equalsIgnoreCase(token)) {
                
                return true;
            }
            else if (ch == 'M' && "MINUS".equalsIgnoreCase(token)) {
                
                return true;
            }
            else if ((ch == 'O' && "ORDER".equalsIgnoreCase(token))
                  || (ch == 'G' && "GROUP".equalsIgnoreCase(token))) {
                
                if ("BY".equalsIgnoreCase(tokenizer.peek())) {
                    
                    return true;
                }
            }
        }
        else if (len == 6) {
            
            char ch = token.charAt(0);
            if ((ch == 'h' || ch == 'H') && "HAVING".equalsIgnoreCase(token)) {
                
                return true;
            }
        }
        else if (len == 7) {
            
            char ch = token.charAt(0);
            if ((ch == 'c' || ch == 'C') && "COMPUTE".equalsIgnoreCase(token)) {
                
                return true;
            }
        }
        else if (len == 9) {
            
            char ch = token.charAt(0);
            if ((ch == 'i' || ch == 'I') && "INTERSECT".equalsIgnoreCase(token)) {
                    
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks to see if we are currently sitting on a new statement (that this
     * parser recognizes).  The set of recognized statements are:
     * <pre>
     *    SELECT
     *    INSERT
     *    UPDATE
     *    DELETE
     *    EXEC
     *    EXECUTE
     *    CALL
     * </pre>
     * 
     * This method does not change the position in the tokenizer stream
     * 
     * @param token The current token
     * @return true if the token is sitting on a statement;
     */
    private boolean isStatement (String token) {
        
        if (token == null) {
            
            return false;
        }
        
        final int len = token.length();
        if (len == 4) {
            
            char ch = token.charAt(0);
            if ((ch == 'c' || ch == 'C') && "CALL".equals(token)) {
                
                return true;
            }
            else if ((ch == 'e' || ch == 'E') && "EXEC".equals(token)) {
                
                return true;
            }
        }
        else if (len == 6) {
            
            char ch = token.charAt(0);
            if ((ch == 's' || ch == 'S') && "SELECT".equals(token)) {
                
                return true;
            }
            else if ((ch == 'i' || ch == 'I') && "INSERT".equals(token)) {
                
                return true;
            }
            else if ((ch == 'u' || ch == 'U') && "UPDATE".equals(token)) {
                
                return true;
            }
            else if ((ch == 'u' || ch == 'U') && "DELETE".equals(token)) {
                
                return true;
            }
        }
        else if (len == 7) {
            
            char ch = token.charAt(0);
            if ((ch == 'e' || ch == 'E') && "EXECUTE".equals(token)) {
                
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks to see if the parse state is currently sitting on an ANSI join clause.
     * Recognized clauses are
     * <pre>
     *    JOIN
     *    INNER JOIN
     *    CROSS JOIN
     *    NATURAL [INNER] JOIN
     *    LEFT [OUTER] JOIN
     *    RIGHT [OUTER] JOIN
     *    FULL [OUTER] JOIN
     * </pre>
     * 
     * @param token The current parse token
     * @param tokenizer The tokenizer 
     * @return true is returned if it was an ANSI join clause, in which case the
     *    tokenizer will be sitting immediately following the clause itself. If
     *    false is returned, then tokenizer state is untouched.
     */
    private boolean isAnsiJoin(String token, SimpleSQLTokenizer tokenizer) {
        
        if (token == null) {
            
            return false;
        }
        
        int len = token.length();
        boolean isOuterJoin = false;
        boolean isInnerJoin = false;
        if (len == 4) {
            
            char ch = token.charAt(0);
            if ((ch == 'j' || ch == 'J') && "JOIN".equalsIgnoreCase(token)) {
            
                return true;
            }
            else if ((ch == 'l' || ch == 'L') && "LEFT".equalsIgnoreCase(token)) {
                
                isOuterJoin = true;
            }
            else if ((ch == 'f' || ch == 'F') && "FULL".equalsIgnoreCase(token)) {
                
                isOuterJoin = true;
            }
        }
        else if (len == 5) {
            
            char ch = token.charAt(0);
            if ((ch == 'r' || ch == 'R') && "RIGHT".equalsIgnoreCase(token)) {
                
                isOuterJoin = true;
            }
            else if ((ch == 'i' || ch == 'I') && "INNER".equalsIgnoreCase(token)) {
                
                isInnerJoin = true;
            }
            else if ((ch == 'c' || ch == 'C') && "CROSS".equalsIgnoreCase(token)) {
                
                isInnerJoin = true;
            }
        }
        else if (len == 7) {
            
            char ch = token.charAt(0);
            if ((ch == 'n' || ch == 'N') && "NATURAL".equalsIgnoreCase(token)) {
                
                String next = tokenizer.next();
                String inner = null;
                if ("INNER".equalsIgnoreCase(next)) {
                    
                    inner = next;
                    next = tokenizer.next();
                }
                
                if ("JOIN".equalsIgnoreCase(next)) {
                    
                    return true;
                }
                
                if (inner != null) {
                    
                    tokenizer.unget(inner);
                }
                
                tokenizer.unget(next);
            }
        }
        
        if (isOuterJoin) {
            
            String next = tokenizer.next();
            String outer = null;
            if (next != null) {
                
                char ch = next.charAt(0);
                if ((ch == 'o' || ch == 'O') && "OUTER".equalsIgnoreCase(next)) {
                    
                    outer = token;
                    next = tokenizer.next();
                }
            }
            
            if (next != null) {
                
                char ch = next.charAt(0);
                if ((ch == 'j' || ch == 'J') && "JOIN".equalsIgnoreCase(next)) {
                    
                    return true;
                }
            }
            
            if (outer != null) {
                
                tokenizer.unget(outer);
            }
            
            tokenizer.unget(next);
        }
        else if (isInnerJoin) {
            
            String next = tokenizer.next();
            if ("JOIN".equalsIgnoreCase(next)) {
                
                return true;
            }
            
            tokenizer.unget(next);
        }
        
        return false;
    }
    
    /**
     * Called after the tokenizer hits a EXEC[UTE] statement and attempts
     * to determine the procedure name that is being deleted.
     * 
     * <p>A procedure execution looks like:
     * <pre>
     *    EXEC[UTE] [@return_code = ] [procedure_name | @variable]
     * </pre>
     */
    private void doExecuteProcedure() {
        
        String token = nextToken();
        
        /*
         * If there is an '@' sign in front of the next token, it is
         * either the @return_code or the procedure's name in a 
         * @variable.
         */
        if (token != null && token.charAt(0) == '@') {
            
            String n = nextToken();
            if (n != null && n.equals("=")) {
                
                token = nextToken();
            }
            else {
                
                token = n;
            }
        }
        
        if (token != null && token.charAt(0) != '@') {
            
            listener.foundProcedureExecution(this,
                doObjectReference(token));
        }
        else {
            
            if (token != null) {
                
                tokenizer.unget(token);
            }
        }
    }
    
    /**
     * Called after the tokenizer hits a CALL statement and attempts
     * to determine the procedure name that is being deleted.
     */
    private void doCall() {
        
        String token = nextToken();
        if (token != null) {
            
            listener.foundProcedureExecution(this, doObjectReference(token));
        }
    }
    
    /**
     * Called when the parser has hit an object it thinks is the name of
     * a database object. This purpose of this method is to try to see
     * if the object is a multi.part.name and will take care of sucking
     * in the rest of the name.
     * 
     * @param tokenizer The tokenizer
     * @param name The object name (or at least the first part)
     * @return The full object.name.
     */
    private DatabaseObject doObjectReference(String name) {
        
        List<String> parts = new ArrayList<String>();
        parts.add(name);
        
        String token = tokenizer.next();
        while (".".equals(token)) {
            
            token = tokenizer.next();
            if (SQLTools.isIdentifier(token)) {
                
                parts.add(token);
                token = tokenizer.next();
            }
            else if (".".equals(token)) {
                
                parts.add("");
            }
            else {
                
                break;
            }
        }
        
        tokenizer.unget(token);;
        
        String catalog = null;
        String schema = null;
        String table = null;
        if (parts.size() == 1) {
            
            table = parts.get(0);
        }
        else if (parts.size() == 2) {
            
            schema = parts.get(0);
            table = parts.get(1);
        }
        else if (parts.size() == 3) {
            
            catalog = parts.get(0);
            schema = parts.get(1);
            table = parts.get(2);
        }
        
        return new DatabaseObject(
            normalizer.normalize(catalog), 
            normalizer.normalize(schema), 
            normalizer.normalize(table));
    }
    
    /**
     * Exception that is thrown internally when the closing parenthesis is hit
     * that matches a given open paren
     */
    @SuppressWarnings("serial")
    private static class HitClosingParen extends Error  {
        
        public HitClosingParen() {
            
        }
    }
    private static final HitClosingParen CLOSING_PAREN_ERROR = new HitClosingParen();
    
    /**
     * Exception that is thrown when a semicolon is encountered, indicating that the
     * current statement is completed
     */
    @SuppressWarnings("serial")
    private static class HitSemiColon extends Error  {
        
        public HitSemiColon() {
            
        }
    }
    private static final HitSemiColon SEMICOLON_ERROR = new HitSemiColon();
}

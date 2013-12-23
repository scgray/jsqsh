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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;


/**
 * The SQLParser is a very very rudamentary parser for traversing a SQL
 * statement or batch of SQL statements looking for "interesting" items
 * that may be found in the SQL. The parser does not fully understand
 * any SQL dialect, but recognizes many keywords and aspects of queries.
 */
public class SQLParser {
    
    /**
     * This is a list of keywords that we treat as the beginning of a 
     * statement.
     */
    private static SQLStatementSet STATEMENTS = new SQLStatementSet();
    static {
        
        STATEMENTS.add("ALTER");
        STATEMENTS.add("BEGIN");
        STATEMENTS.add("BREAK");
        STATEMENTS.add("COMMIT");
        STATEMENTS.add("CREATE");
        STATEMENTS.add("DBCC");
        STATEMENTS.add("DECLARE");
        STATEMENTS.add("DELETE");
        STATEMENTS.add("DROP");
        STATEMENTS.add("DUMP");
        STATEMENTS.add("END");
        STATEMENTS.add("EXEC");
        STATEMENTS.add("EXECUTE");
        STATEMENTS.add("FETCH");
        STATEMENTS.add("GOTO");
        STATEMENTS.add("GRANT");
        STATEMENTS.add("IF");
        STATEMENTS.add("INSERT");
        STATEMENTS.add("LOAD");
        STATEMENTS.add("ROLLBACK");
        STATEMENTS.add("SELECT");
        STATEMENTS.add("UPDATE");
        STATEMENTS.add("WHILE");
        STATEMENTS.add("USE");
    }
    
    /**
     * This is a list of keywords that we treat as clauses for SQL statements.
     */
    private static HashSet<String> CLAUSES = new HashSet<String>();
    static {
        
        CLAUSES.add("FROM");
        CLAUSES.add("WHERE");
        CLAUSES.add("SET");
        CLAUSES.add("ORDER");
        CLAUSES.add("GROUP");
        CLAUSES.add("COMPUTE");
    }
    
    
    /**
     * The listener for parse events.
     */
    private SQLParserListener listener = null;
    private SimpleSQLTokenizer tokenizer = null;
    
    /**
     * Creates a new parser.
     * 
     * @param listener The object to be notified when the parser hits
     *   "interesting" points in the SQL.
     */
    public SQLParser (SQLParserListener listener) {
        
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
        
        String keyword = tokenizer.next();
        boolean inDML = false;
        
        while (keyword != null) {
            
            /*
             * Handle the case of entering a new statement.
             */
            SQLStatement statement = STATEMENTS.find(keyword, tokenizer);
            if (statement != null) {
                
                if (statement.equals("DELETE")) {
                    
                    inDML = true;
                    listener.foundStatement(this, statement.toString());
                    doDelete();
                }
                else if (statement.equals("UPDATE")) {
                    
                    inDML = true;
                    listener.foundStatement(this, statement.toString());
                    doInsertUpdate();
                }
                else if (statement.equals("INSERT")) {
                    
                    inDML = true;
                    listener.foundStatement(this, statement.toString());
                    doInsertUpdate();
                }
                else if (statement.equals("SELECT")) {
                    
                    if (tokenizer.getParenCount() > 0) {
                        
                        doSubquery();
                    }
                    else {
                        
                        inDML = true;
                        listener.foundStatement(this, statement.toString());
                    }
                }
                else if (statement.equals("EXEC")
                        || statement.equals("EXECUTE")) {
                    
                    listener.foundStatement(this, "EXECUTE");
                    doExecuteProcedure();
                }
                else {
                    
                    inDML = false;
                    listener.foundStatement(this, statement.toString());
                }
            }
            else {
                
                keyword = keyword.toUpperCase();
                if (CLAUSES.contains(keyword) && inDML) {
                
                    listener.foundClause(this, keyword);
                
                    /*
                     * To prevent false positives on clauses, we only recognize
                     * them if they appear as part of a DML statement. For example
                     * a FROM may also be part of some other part of the SQL
                     * dialect, so we will only pay attention if it comes after 
                     * a SELECT/INSERT/UPDATE/DELETE.
                     */
                    if ("FROM".equals(keyword)) {
                        
                        doFrom();
                    }
                }
            }
                
            keyword = tokenizer.next();
        }
        
        tokenizer = null;
    }
    
    /**
     * Called after the tokenizer hits a DELETE statement and attempts
     * to determine the table name that is being deleted.
     * 
     * @param tableRefs The list of references found thus far.
     * @param tokenizer The current parsing state.
     */
    private void doDelete() {
        
        String token = tokenizer.next();
        if (token != null && "FROM".compareToIgnoreCase(token) == 0) {
            
            token = tokenizer.next();
        }
        
        if (token != null) {
            
            listener.foundTableReference(this,
                doObjectReference(tokenizer, token));
        }
    }
    
    /**
     * Called to process the parts of a subquery.
     */
    private void doSubquery() {
        
        /*
         * Keep track of how deeply nested we were when we entered
         * the subquery.
         */
        int parenDepth = tokenizer.getParenCount() - 1;
        
        /*
         * Let someone know we have entered.
         */
        listener.enteredSubquery(this);
        
        String token = tokenizer.next();
        while (token != null) {
            
            /*
             * If we hit a close paren and it knocks us back to the depth
             * at which the subquery started, then we are finished.
             */
            if (token.equals(")") && tokenizer.getParenCount() <= parenDepth) {
                
                listener.exitedSubquery(this);
                return;
            }
            
            String keyword = token.toUpperCase();
            
            if (CLAUSES.contains(keyword)) {
                
                listener.foundClause(this, keyword);
            }
            
            /*
             * Again, we could hit yet another sub-select.
             */
            if ("SELECT".equals(keyword)) {
                
                doSubquery();
            }
            else if ("FROM".equals(keyword)) {
                
                doFrom();
            }
            
            token = tokenizer.next();
        }
    }
    
    /**
     * Called when a FROM clause has been encountered.
     */
    private void doFrom() {
        
        int parenDepth = tokenizer.getParenCount();
        
        String token = tokenizer.next();
        boolean done = false;
        while (token != null && !done) {
            
            DatabaseObject table = null;
            
            /*
             * The starting token is expected to be the name of an
             * object that we are selecting from.
             */
            char ch = token.charAt(0);
            if (Character.isLetter(ch) || ch == '_') {
                
                table = doObjectReference(tokenizer, token);
                
                /*
                 * Next we could have the keyword 'as', if so, ignore it.
                 */
                token = tokenizer.next();
                if (token != null
                        && "AS".compareToIgnoreCase(token) == 0) {
                    
                    token = tokenizer.next();
                }
                
                /*
                 * Next we could have the alias for the table.
                 */
                if (token != null
                        && "WHERE".compareToIgnoreCase(token) != 0
                        && (token.charAt(0) == '_'
                            || Character.isLetter(token.charAt(0)))) {
                    
                    table.setAlias(token);
                    token = tokenizer.next();
                }
                
                /*
                 * At this point we have parsed:
                 *     FROM table [AS alias]
                 * At this point I am going to skip forward until I hit 
                 * a comma or a WHERE. I do this because some databases
                 * allow for hints and various other alterations following
                 * the table name and I don't want to worry about the 
                 * specifics of that syntax.
                 * 
                 * Basically, this allows for logic that handles:
                 * 
                 *    FROM table [AS ALIAS] [STUFF], table ...
                 *    
                 * I also keep track of whether or not the parenthes depth
                 * fall below that of where the FROM statement began. That is,
                 * I want to pick up on :
                 * 
                 *    FROM table as x)
                 *    
                 * which meant that we hit the end of a FROM that was part
                 * of a sub-select.
                 */
                while (token != null 
                        && "WHERE".compareToIgnoreCase(token) != 0
                        && token.equals(",") == false
                        && tokenizer.getParenCount() >= parenDepth) {
                            
                    token = tokenizer.next();
                }
                
                if (token != null
                        && token.equals(",")) {
                    
                    token = tokenizer.next();
                }
                else {
                    
                    done = true;
                }
                
                listener.foundTableReference(this, table);
            }
            else {
                
                done = true;
            }
        }
        
        if (token != null) {
            
            tokenizer.unget(token);
        }
    }
    
    
    /**
     * Called after the tokenizer hits an INSERT/UPDATE statement and attempts
     * to determine the table name that is being updated or inserted into.
     * 
     * @param tableRefs The list of references found thus far.
     * @param tokenizer The current parsing state.
     */
    private void doInsertUpdate() {
        
        String token = tokenizer.next();
        if (token != null) {
            
            listener.foundTableReference(this,
                doObjectReference(tokenizer, token));
        }
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
        
        String token = tokenizer.next();
        
        /*
         * If there is an '@' sign in front of the next token, it is
         * either the @return_code or the procedure's name in a 
         * @variable.
         */
        if (token != null && token.charAt(0) == '@') {
            
            String n = tokenizer.next();
            if (n != null && n.equals("=")) {
                
                token = tokenizer.next();
            }
            else {
                
                token = n;
            }
        }
        
        if (token != null && token.charAt(0) != '@') {
            
            listener.foundProcedureExecution(this,
                doObjectReference(tokenizer, token));
        }
        else {
            
            if (token != null) {
                
                tokenizer.unget(token);
            }
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
    private static DatabaseObject doObjectReference(
            SimpleSQLTokenizer tokenizer, String name) {
        
        List<String> parts = new ArrayList<String>();
        parts.add(name);
        
        String token = tokenizer.next();
        if (token != null && token.equals(".")) {
            
            while (token != null && token.equals(".")) {
                
                token = tokenizer.next();
                if (token != null) {
                    
                    if (token.equals(".")) {
                        
                        parts.add("");
                        tokenizer.unget(token);
                    }
                    else {
                        
                        parts.add(token);
                    }
                    
                    token = tokenizer.next();
                }
            }
        }
        
        if (token != null) {
            
            tokenizer.unget(token);
        }
        
        String catalog = null;
        String owner = null;
        String table = null;
        if (parts.size() == 1) {
            
            table = parts.get(0);
        }
        else if (parts.size() == 2) {
            
            owner = parts.get(0);
            table = parts.get(1);
        }
        else if (parts.size() == 3) {
            
            catalog = parts.get(0);
            owner = parts.get(1);
            table = parts.get(2);
        }
        
        return new DatabaseObject(catalog, owner, table);
    }
    
    /**
     * A SQLStatement is a sequence of keywords (tokens) that, put
     * together, represent a logical SQL statement.
     */
    private static class SQLStatement {
        
        private String statement;
        private String keyword;
        private SQLStatement nextKeyword;
        
        private SQLStatement () {
            
            /* NOTHING */
        }
        
        /**
         * Creates a new SQLStatement.
         * 
         * @param keywords This is a white-space delimited sequence of
         *     keywords that, together, make up the SQL Statement.
         */
        public SQLStatement (String keywords) {
            
            this.statement = keywords;
            String []words = keywords.split("\\s");
            this.keyword = words[0];
            
            SQLStatement prevWord = this;
            for (int i = 1; i < words.length; i++) {
                
                prevWord.nextKeyword = new SQLStatement();
                prevWord.nextKeyword.keyword = words[i];
                prevWord = prevWord.nextKeyword;
            }
        }
        
        /**
         * Compares a provided keyword against the statement definition.
         * In the case where the SQLSTatement involves more than one
         * keyword, the provided {@link SimpleSQLTokenizer} will be polled
         * to fetch the next available keyword (multiple times if necessary)
         * to perform a full comparison.  If the match is successful then
         * the tokenizer will be left pointing at the next token immediately
         * following the SQL statement. If the match is unsuccessful then 
         * the state of the tokenizer will be unaltered.
         * 
         * @param word The word to compare.
         * @param tokenizer If this SQLStatement comprises more than one
         *    keyword then the tokenizer will be used to pull the next
         *    available keyword.
         *    
         * @return true if they match.
         */
        public int compare(String word,
                SimpleSQLTokenizer tokenizer) {
            
            if (word == null) {
                
                return -1;
            }
            
            int rc = keyword.compareToIgnoreCase(word);
            if (rc == 0) {
                
                if (nextKeyword != null) {
                    
                    String token = tokenizer.next();
                    rc = nextKeyword.compare(token, tokenizer);
                    if (rc == 0) {
                        
                        return 0;
                    }
                    else {
                        
                        if (token != null) {
                            
                            tokenizer.unget(token);
                        }
                        
                        return rc;
                    }
                }
                else {
                    
                    return 0;
                }
            }
            
            return rc;
        }
        
        public boolean equals(Object o) {
            
            if (o instanceof String) {
                
                return statement.equalsIgnoreCase((String) o);
            }
            
            return false;
        }
        
        public String toString() {
            
            return statement;
        }
    }
    
    /**
     * A cheesy "bag" used to hold a set of SQL statements.
     */
    private static class SQLStatementSet {
        
        private List<SQLStatement> statements
            = new ArrayList<SQLStatement>();
        boolean isSorted = false;
        
        public void add(String statement) {
            
            isSorted = false;
            statements.add(new SQLStatement(statement));
        }
        
        public void add(SQLStatement statement) {
            
            isSorted = false;
            statements.add(statement);
        }
        
        public SQLStatement find (String word, SimpleSQLTokenizer tokenizer) {
            
            if (!isSorted) {
                
                Collections.sort(statements, new Comparator<SQLStatement>() {
                    
                    public int compare (SQLStatement s1, SQLStatement s2) {
                        
                        return s1.toString().compareTo(s2.toString());
                    }
                });
                
                isSorted = true;
            }
            
            int low = 0;
            int high = statements.size() - 1;
            
            while (low < high) {
                
                int mid = (low + high) / 2;
                SQLStatement s = statements.get(mid);
                int rc = s.compare(word, tokenizer);
                
                if (rc > 0) {
                    
                    high = mid - 1;
                }
                else if (rc < 0) {
                    
                    low = mid + 1;
                }
                else {
                    
                    return s;
                }
            }
            
            return null;
        }
    }
}

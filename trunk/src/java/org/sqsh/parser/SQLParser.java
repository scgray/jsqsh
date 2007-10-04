package org.sqsh.parser;

import java.util.ArrayList;
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
    private static HashSet<String> STATEMENTS = new HashSet<String>();
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
        
        String token = tokenizer.next();
        boolean inDML = false;
        
        while (token != null) {
            
            String keyword = token.toUpperCase();
            
            /*
             * Handle the case of entering a new statement.
             */
            if (STATEMENTS.contains(keyword)) {
                
                if ("DELETE".equals(keyword)) {
                    
                    inDML = true;
                    listener.foundStatement(this, keyword);
                    doDelete();
                }
                else if ("UPDATE".equals(keyword)) {
                    
                    inDML = true;
                    listener.foundStatement(this, keyword);
                    doInsertUpdate();
                }
                else if ("INSERT".equals(keyword)) {
                    
                    inDML = true;
                    listener.foundStatement(this, keyword);
                    doInsertUpdate();
                }
                else if ("SELECT".equals(keyword)) {
                    
                    if (tokenizer.getParenCount() > 0) {
                        
                        doSubquery();
                    }
                    else {
                        
                        inDML = true;
                        listener.foundStatement(this, keyword);
                    }
                }
                else {
                    
                    inDML = false;
                    listener.foundStatement(this, keyword);
                }
            }
            else if (CLAUSES.contains(keyword) && inDML) {
                
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
                
            token = tokenizer.next();
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
                doTableReference(tokenizer, token));
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
            
            TableReference table = null;
            
            /*
             * The starting token is expected to be the name of an
             * object that we are selecting from.
             */
            char ch = token.charAt(0);
            if (Character.isLetter(ch) || ch == '_') {
                
                table = doTableReference(tokenizer, token);
                
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
                doTableReference(tokenizer, token));
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
    private static TableReference doTableReference(
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
        
        return new TableReference(catalog, owner, table);
    }
}
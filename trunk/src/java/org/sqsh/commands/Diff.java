package org.sqsh.commands;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.sqsh.Command;
import org.sqsh.SQLTools;
import org.sqsh.Session;
import org.sqsh.SqshContext;
import org.sqsh.format.BlobFormatter;
import org.sqsh.format.ByteFormatter;
import org.sqsh.format.ClobFormatter;

/**
 * Implements the \diff command.
 */
public class Diff
    extends Command {
    
    private static class Options {
        
         @Argument
             public List<String> arguments = new ArrayList<String>();
     }

    @Override
    public int execute (Session session, String[] argv)
        throws Exception {
        
        ArrayList<Session> sessions = new ArrayList<Session>();
        SqshContext ctx = session.getContext();
        Options options = new Options();
        int rc = parseOptions(session, argv, options);
        if (rc != 0) {
            
            return rc;
        } 
        
        /*
         * The current session is always part of the "diff".
         */
        sessions.add(session);
        
        /*
         * Iterate through the remaining arguments. These should all be 
         * session numbers that are to be used for the comparison.
         */
        for (int i = 0; i < options.arguments.size(); i++) {
            
            boolean ok = true;
            try {
                
                int id = Integer.parseInt(options.arguments.get(i));
                Session nextSession = ctx.getSession(id);
                if (nextSession == null) {
                    
                    ok = false;
                }
                else {
                    
                    if (sessions.contains(nextSession) == false) {
                    
                    	sessions.add(nextSession);
                	}
                }
            }
            catch (NumberFormatException e) {
                
                ok = false;
            }
            
            if (!ok) {
                
                session.err.println("Session '" + options.arguments.get(i)
                    + "' is not a valid session identifier. Use \\session to "
                    + "view a list of active sessions.");
                return 1;
            }
        }
        
        /*
         * Now, verify that each session is connected to a server.
         */
        for (Session s : sessions) {
            
            if (s.getConnection() == null) {
                
                session.err.println("Session #"
                    + s.getId() + " is not currently connected to a database "
                    + "server.");
                return 1;
            }
        }
        
        /*
         * Grab ahold of the SQL to be tested.
         */
        String sql = session.getBufferManager().getCurrent().toString();
        
        if (session.isInteractive()) {
            
            session.getBufferManager().newBuffer();
        }
        else {
            
            session.getBufferManager().getCurrent().clear();
        }
        
        if (compare(sessions.toArray(new Session[0]), sql)) {
            
            session.out.println("Results are identical.");
            return 0;
        }
        
        return 1;
    }
    
    private boolean compare (Session []sessions, String sql) {
        
        Statement []statements = new Statement[sessions.length];
        ResultSet []results = new ResultSet[sessions.length];
        SQLException []exceptions = new SQLException[sessions.length];
        SQLWarning []warnings = new SQLWarning[sessions.length];
        boolean gotException = false;
        boolean ok =  true;
        
        /*
         * First, create our statements.
         */
        for (int i = 0; i < sessions.length; i++) {
            
            try {
                
                statements[i] =
                    sessions[i].getConnection().createStatement();
            }
            catch (SQLException e)  {
                
                gotException = true;
                exceptions[i] = e;
            }
        }
        
        /*
         * Next, if everything went ok and/or everything failed
         * in the same way, then execute the SQL against all of 
         * the servers.
         */
        ok = compareExceptions(sessions, exceptions);
        if (ok && !gotException) {
            
            for (int i = 0; i < statements.length; i++) {
                
                try {
                    
                    statements[i].execute(sql);
                }
                catch (SQLException e) {
                    
                    gotException = true;
                    exceptions[i] = e;
                }
            }
        }
        
        /*
         * Check to see if they all succeeded or failed in the same
         * way. If they did, then process the result set(s) and compare
         * to see if they are the same.
         */
        ok = compareExceptions(sessions, exceptions);
        if (ok && !gotException) {
            
            for (int i = 0; i < statements.length; i++) {
                
                try {
                    
                    statements[i].execute(sql);
                }
                catch (SQLException e) {
                    
                    gotException = true;
                    exceptions[i] = e;
                }
            }
        }
        
        /*
         * We are done if a comparison failed or we had an 
         * exception previously.
         */
        boolean done = (ok == false || gotException);
        while (ok && !done) {
            
            boolean haveResults = false;
            
            /*
             * Fetch the next result set.
             */
            for (int i = 0; i < statements.length; i++) {
                
                try {
                    
                    results[i] = statements[i].getResultSet();
                    if (results[i] != null) {
                        
                        haveResults = true;
                    }
                }
                catch (SQLException e) {
                    
                    gotException = true;
                    exceptions[i] = e;
                }
            }
            
            ok = compareExceptions(sessions, exceptions);
            
            /*
             * If the everything checked out thus far and we have
             * a result set, then compare them.
             */
            if (ok && haveResults) {
                
                ok = compareResults(sessions, results);
                close(results);
            }
            
            int nRows = -1;
            boolean isMore = false;
            
            if (ok) {
                
                nRows = compareUpdateCount(sessions, statements);
                if (nRows == -999) {
                    
                    ok = false;
                }
                else {
                    
                    int m = compareMoreResults(sessions, statements);
                    if (m == -1) {
                        
                        ok = false;
                    }
                    else {
                        
                        isMore = (m == 1);
                    }
                }
            }
            
            if (ok && isMore == false && nRows < 0) {
                
                done = true;
            }
        }
        
        close(results);
        close(statements);
        
        return ok;
    }
    
    /**
     * Compares result sets from multiple sessions.
     * 
     * @param sessions The sessions to compare
     * @param results The results
     * @return true if they match, false if they don't
     */
    private boolean compareResults(Session []sessions,
            ResultSet []results) {
        
        SQLException []exceptions = new SQLException[sessions.length];
        
        /*
         * First, make sure that the metadata is the same across all
         * of our result sets. No bother comparing data if they don't
         * match.
         */
        if (!compareMetadata(sessions, results)) {
            
            return false;
        }
        
        ResultSetMetaData meta;
        int ncols;
        
        boolean ok = true;
        boolean done = false;
        boolean gotException = false;
        int rowCount = 0;
        
        while (ok && !done) {
            
            /*
             * Call next() on each result.
             */
            boolean []isNext = new boolean[results.length];
            for (int i = 0; i < results.length; i++) {
                
                try {
                    
                    isNext[i] = results[i].next();
                    if (i > 0 && isNext[i] != isNext[0]) {
                        
                        ok = false;
                    }
                }
                catch (SQLException e) {
                    
                    exceptions[i] = e;
                    gotException = true;
                }
            }
            
            if (!ok) {
                
                System.err.println("Number of available rows differs:");
                for (int i = 0; i < sessions.length; i++) {
                    
                	System.err.println("   Session #"
                    	+ sessions[i].getId() + ": " 
                    	+ (isNext[i] ? "More rows" : "No more rows"));
                }
            }
            
            /*
             * If we got an exception, stop but compare that we got the
             * same exception.
             */
            if (ok) {
                
                if (gotException) {
	                
                	ok = compareExceptions(sessions, exceptions);
                	done = true;
            	}
                else {
                    
                    if (isNext[0]) {
                
                        ++rowCount;
                        ok = compareRow(sessions, results, rowCount);
                    }
                    else {
                
                        done = true;
                    }
                }
            }
        }
        
        return ok;
    }
    
    /**
     * Compares the contents of a row.
     * 
     * @param sessions The sessions
     * @param results The results
     * @param rowCount The current rowcount
     * @return true if the row matches
     */
    private boolean compareRow(Session []sessions,
            ResultSet []results, int rowCount) {
        
        SQLException []exceptions = new SQLException[sessions.length];
        boolean gotException = false;
        boolean ok = true;
        int badColumn = -1;
        Object [][]values = new Object[results.length][];
        
        /*
         * Suck in the values for each row of each session.
         */
        for (int i = 0; i < results.length; i++) {
            
            try {
                
                values[i] = getRow(results[i]);
            }
            catch (SQLException e) {
                
                gotException = true;
                exceptions[i] = e;
            }
        }
        
        /*
         * If they got an exception we are finished.
         */
        if (gotException) {
            
            return (compareExceptions(sessions, exceptions));
        }
        
        /*
         * Now, compare our results.
         */
        for (int i = 1; ok && i < results.length; i++) {
            
            for (int c = 0; ok && c < values[i].length; c++) {
                
                if (!values[i][c].equals(values[0][c])) {
                    
                    ok = false;
                    badColumn = c;
                }
            }
        }
        
        if (!ok) {
            
            System.err.println("Value contained in row #"
                + rowCount + ", column #" 
                + (badColumn + 1) + " differs:");
            for (int i = 0; i < results.length; i++) {
                
                System.err.println("   Session #"
                    	+ sessions[i].getId() + ": " 
                    	+ values[i][badColumn].toString());
            }
        }
        
        return ok;
    }
    
    private Object[] getRow(ResultSet set)
        throws SQLException {
        
        ResultSetMetaData meta = set.getMetaData();
        int ncols = meta.getColumnCount();
        Object []values = new Object[ncols];
        for (int c = 1; c <= ncols; c++) {
            
            int type = meta.getColumnType(c);
            switch (type) {
                
                case Types.NUMERIC:
                case Types.DECIMAL:
                    values[c - 1] = set.getBigDecimal(c);
                    break;
                    
                case Types.BINARY:
                case Types.VARBINARY:
                case Types.LONGVARBINARY:
                    ByteFormatter f = new ByteFormatter(
                        meta.getColumnDisplaySize(c));
                    values[c - 1] = f.format(set.getObject(c));
                    break;
                    
                case Types.BLOB:
                    BlobFormatter b = new BlobFormatter();
                    values[c - 1] = b.format(set.getObject(c));
                    break;
                    
                case Types.CLOB:
                    ClobFormatter cf= new ClobFormatter();
                    values[c - 1] = cf.format(set.getObject(c));
                    break;
                    
                case Types.BOOLEAN:
                    values[c - 1] = set.getBoolean(c);
                    break;
                    
                case Types.CHAR:
                case Types.VARCHAR:
                /* case Types.NVARCHAR:*/
                /* case Types.LONGNVARCHAR: */
                case Types.LONGVARCHAR:
                /* case Types.NCHAR: */
                    values[c - 1] = set.getString(c);
                    break;
                    
                case Types.DATE:
                case Types.TIME:
                case Types.TIMESTAMP:
                    values[c - 1] = set.getObject(c);
                    break;
                    
                case Types.DOUBLE:
                case Types.FLOAT:
                case Types.REAL:
                    values[c - 1] = set.getDouble(c);
                    break;
                   
                case Types.BIT:
                case Types.TINYINT:
                case Types.SMALLINT:
                case Types.INTEGER:
                case Types.BIGINT:
                    values[c - 1] = new Long(set.getLong(c));
                    break;
                    
                default:
                    System.err.println("WARNING: I do not understand "
                        + "datatype #" + type + " ("
                        + meta.getColumnTypeName(c) + ") in column #"
                        + c + ". No comparison will be performed.");
                    values[c - 1] = set.getObject(c);
                    break;
            }
            
            if (set.wasNull()) {
                
                values[c - 1] = "NULL";
            }
        }
        
        return values;
    }
    
    
    /**
     * Compares metadata from result sets of multiple sessions.
     * 
     * @param sessions The sessions to compare
     * @param results The results
     * @return true if the metadata matches, false otherwise.
     */
    private boolean compareMetadata(Session []sessions,
            ResultSet []results) {
        
        boolean ok = true;
        
        try {
            
            ResultSetMetaData meta = results[0].getMetaData();
            
            /*
             * First check the column count.
             */
            for (int i = 1; ok && i < results.length; i++) {
                
                if (meta.getColumnCount() 
                        != results[i].getMetaData().getColumnCount()) {
                    
                    ok = false;
                }
            }
            
            if (!ok) {
                
                System.err.println("Column count differs in results");
                for (int i = 0; i < results.length; i++) {
                
                	System.err.println("   Session #"
                    	+ sessions[i].getId() + ": " 
                    	+ results[i].getMetaData().getColumnCount()
                    	+ " column(s)");
            	}
                
                return false;
            }
            
            /*
             * Now that we can be confident the column counts match
             * we'll check that other metadata matches. I cheat a bit
             * here and build a string to describe the metadata and
             * then compare the strings. This allows me to easily add
             * or remote items that I want to compare.
             */
            meta = results[0].getMetaData();
            int cols = meta.getColumnCount();
            
            String []descriptions = new String[results.length];
            int badColumn = -1;
            for (int c = 1; ok && c <= cols; c++) {
                
                for (int i = 0; i < results.length; i++) {
                    
                    meta = results[i].getMetaData();
                    StringBuilder sb = new StringBuilder();
                    
                	sb.append("Type #")
                	    .append(meta.getColumnType(c))
                	    .append(" (")
                	    .append(SQLTools.getTypeName(meta.getColumnType(c)))
                	    .append(")");
                	
                	if (meta.getColumnType(c) == Types.NUMERIC
                	   || meta.getColumnType(c) == Types.DECIMAL) {
                	    
                	    sb.append(", Precision=")
                	        .append(meta.getPrecision(c))
                	        .append(", Scale=")
                	        .append(meta.getScale(c));
                	}
                	
                	descriptions[i] = sb.toString();
                	
                	if (ok && i > 0) {
                	    
                	    if (!(descriptions[0].equals(descriptions[i]))) {
                	        
                	        ok = false;
                	        badColumn = c;
                	    }
                	}
                }
            }
                    
            if (!ok) {
                
                System.err.println("Datatype differs in column #"
                    + badColumn);
                
                for (int i = 0; i < sessions.length; i++) {
                    
                	System.err.println("   Session #"
                	    + sessions[i].getId()
                	    + ": " + descriptions[i]);
            	}
                
                return false;
            }
        }
        catch (SQLException e) {
            
            System.err.println("WARNING: Exception while retrieving "
                + "result set metadata. I cannot do a propper diff if this "
                + "occurs: " + e.getMessage());
        }
        
        return true;
    }
    
    /**
     * Compares the update counts of a set of statements.
     * 
     * @param sessions The sessions
     * @param statements The statements to check
     * @return The actual update count(s) or -999 if they differ.
     */
    private int compareUpdateCount(Session []sessions,
            Statement []statements) {
        
        boolean ok = true;
        int c = -1;
        try {
            
            c = statements[0].getUpdateCount();
            for (int i = 1; ok && i < sessions.length; i++) {
                
                if (statements[i].getUpdateCount() != c) {
                    
                    ok = false;
                }
            }
            
            if (!ok) {
                
                System.err.println("Update count differs:");
                for (int i = 0; i < statements.length; i++) {
                
                	System.err.println("   Session #"
                    	+ sessions[i].getId() + ": " 
                    	+ statements[i].getUpdateCount()
                    	+ " row(s)");
            	}
                
                return -999;
            }
        }
        catch (SQLException e) {
            
            /* IGNORED */
        }
        
        return c;
    }
    
    /**
     * Checks to see if there are more results.
     * 
     * @param sessions The sessions
     * @param statements The statements to check
     * @return 1 if there are more, 0 if there aren't, and -1
     *   if the statements differ about ti.
     */
    private int compareMoreResults(Session []sessions,
            Statement []statements) {
        
        boolean ok = true;
        boolean isMore = false;
        
        try {
            
            isMore = statements[0].getMoreResults();
            for (int i = 1; ok && i < sessions.length; i++) {
                
                if (statements[i].getMoreResults() != isMore) {
                    
                    ok = false;
                }
            }
            
            if (!ok) {
                
                System.err.println("Number of result sets differ:");
                for (int i = 0; i < statements.length; i++) {
                
                	System.err.println("   Session #"
                    	+ sessions[i].getId() + ": " 
                    	+ (statements[i].getMoreResults()
                    	        ? "Results pending"
                    	        : "No more results"));
            	}
                
                return -1;
            }
        }
        catch (SQLException e) {
            
            /* IGNORED */
        }
        
        return (isMore ? 1 : 0);
    }
    
    /**
     * Compares the exceptions returned by our sessions.
     * @param sessions The sessions.
     * @param exceptions The exceptions
     * @return true if they are the same.
     */
    private boolean compareExceptions (Session []sessions, 
            SQLException []exceptions) {
        
        boolean ok = true;
        SQLException e = exceptions[0];
        
        for (int i = 1; i < exceptions.length; i++) {
            
            if ((e != null || exceptions[i] != null)
               && ((e == null && exceptions[i] != null)
                  || (e != null && exceptions[i] == null)
                  || !(e.getMessage().equals(exceptions[i].getMessage())))) {
                
                ok = false;
                break;
            }
        }
        
        if (!ok) {
            
            System.err.println("SQL exceptions differ:");
            for (int i = 0; i < exceptions.length; i++) {
                
                e = exceptions[i];
                System.err.println("   Session #"
                    + sessions[i].getId() + ": " 
                    + (e == null ? "<none>" : e.getMessage()));
            }
        }
        
        return ok;
    }
    
    private void close (Statement []statements) {
        
        for (int i = 0; i < statements.length; i++) {
            
            SQLTools.close(statements[i]);
            statements[i] = null;
        }
    }
    
    private void close (ResultSet []results) {
        
        for (int i = 0; i < results.length; i++) {
            
            SQLTools.close(results[i]);
            results[i] = null;
        }
    }
}

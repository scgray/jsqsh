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
package org.sqsh.commands;

import org.sqsh.Command;
import org.sqsh.SQLTools;
import org.sqsh.Session;
import org.sqsh.SqshContext;
import org.sqsh.SqshOptions;
import org.sqsh.format.BlobFormatter;
import org.sqsh.format.ByteFormatter;
import org.sqsh.format.ClobFormatter;
import org.sqsh.options.Argv;
import org.sqsh.options.OptionProperty;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import static org.sqsh.options.ArgumentRequired.REQUIRED;

/**
 * Implements the \diff command.
 */
public class Diff extends Command {

    private static class Options extends SqshOptions {

        // 0  - Don't check update counts
        // 1  - Only check non-zero update counts
        // >  - Check and compare update counts
        @OptionProperty(option = 'u', longOption = "update-stringency", arg = REQUIRED,
                description = "Stringency for update count checks")
        public int updateStringency = 2;

        // 0  - Check that an exception was thrown
        // 1  - Check that the text of the exception is the same
        // 2  - Check the SQLStatus of the exception
        // >  - Check the error code
        @OptionProperty(option = 'e', longOption = "exception-stringency", arg = REQUIRED,
                description = "Stringency for exception checking")
        public int exceptionStringency = 1;

        @Argv(program = "\\diff", min = 0, usage = "[-u update-stringency] [-e exception-stringency] " + "[session [session ...]]")
        public List<String> arguments = new ArrayList<String>();
    }

    @Override
    public SqshOptions getOptions() {
        return new Options();
    }

    /**
     * The currently elected set of options. This is local to this instance so we don't need to pass it around.
     */
    private Options options = null;

    @Override
    public int execute(Session session, SqshOptions opts) throws Exception {
        options = (Options) opts;
        ArrayList<Session> sessions = new ArrayList<Session>();
        SqshContext ctx = session.getContext();

        // The current session is always part of the "diff".
        sessions.add(session);

        // If no arguments were received, then we are comparing all sessions.
        if (options.arguments.size() < 1) {
            for (Session s : ctx.getSessions()) {
                options.arguments.add(Integer.toString(s.getId()));
            }
        }

        // Iterate through the remaining arguments. These should all be session numbers that are to be used for
        // the comparison.
        for (int i = 0; i < options.arguments.size(); i++) {
            boolean ok = true;
            try {
                int id = Integer.parseInt(options.arguments.get(i));
                Session nextSession = ctx.getSession(id);
                if (nextSession == null) {
                    ok = false;
                } else {
                    if (!sessions.contains(nextSession)) {
                        sessions.add(nextSession);
                    }
                }
            } catch (NumberFormatException e) {
                ok = false;
            }
            if (!ok) {
                session.err.println("Session '" + options.arguments.get(i) + "' is not a valid session identifier. "
                        + "Use \\session to view a list of active sessions.");
                return 1;
            }
        }

        // Now, verify that each session is connected to a server.
        for (Session s : sessions) {
            if (s.getConnection() == null) {
                session.err.println("Session #" + s.getId() + " is not currently connected to a database server.");
                return 1;
            }
        }

        // Grab a hold of the SQL to be tested.
        String sql = session.getBufferManager().getCurrent().toString();
        if (session.isInteractive()) {
            session.getBufferManager().newBuffer();
        } else {
            session.getBufferManager().getCurrent().clear();
        }
        if (compare(sessions.toArray(new Session[0]), sql)) {
            session.out.println("Results are identical.");
            return 0;
        }
        return 1;
    }

    private boolean compare(Session[] sessions, String sql) {
        Statement[] statements = new Statement[sessions.length];
        ResultSet[] results = new ResultSet[sessions.length];
        int[] updateCounts = new int[sessions.length];
        boolean[] moreResults = new boolean[sessions.length];
        SQLException[] exceptions = new SQLException[sessions.length];
        String[] resultState = new String[sessions.length];
        int[] resultCount = new int[sessions.length];
        boolean gotException = false;
        boolean ok = true;

        // First, create our statements.
        for (int i = 0; i < sessions.length; i++) {
            resultCount[i] = 0;
            try {
                statements[i] = sessions[i].getConnection().createStatement();
            } catch (SQLException e) {
                gotException = true;
                exceptions[i] = e;
            }
        }

        // Next, if everything went ok and/or everything failed in the same way, then execute the SQL against all servers
        ok = compareExceptions(sessions, exceptions);
        if (ok && !gotException) {
            for (int i = 0; i < statements.length; i++) {
                try {
                    statements[i].execute(sql);
                } catch (SQLException e) {
                    gotException = true;
                    exceptions[i] = e;
                }
            }
        }

        // Check to see if they all succeeded or failed in the same way. If they did, then process the result
        // set(s) and compare to see if they are the same.
        ok = compareExceptions(sessions, exceptions);
        if (ok && !gotException) {
            for (int i = 0; i < statements.length; i++) {
                try {
                    statements[i].execute(sql);
                } catch (SQLException e) {
                    gotException = true;
                    exceptions[i] = e;
                }
            }
        }

        // We are done if a comparison failed or we had an exception previously.
        boolean done = (!ok || gotException);
        while (ok && !done) {

            // Fetch the next result set.
            for (int i = 0; i < statements.length; i++) {
                exceptions[i] = null;
                try {
                    // This loop will attempt to "seek" forward to the next relevant piece of information we want from
                    // the result set, because upon the updateStringency value.
                    //   0  - Will seek until it finds row results
                    //   1  - Will seek until it hits a > 0 update count
                    //   >1 - Will stop at rows or update counts.
                    do {

                        // If we have already grabbed a result set, then check to see if there are any more results
                        // available.
                        moreResults[i] = (resultCount[i] <= 0 || statements[i].getMoreResults());
                        results[i] = null;
                        updateCounts[i] = -999;


                        // If there are more results, then deal with them.
                        if (moreResults[i]) {
                            results[i] = statements[i].getResultSet();
                            ++resultCount[i];
                            if (results[i] == null) {

                                // If we have no result sets, then fetch the update count.
                                results[i] = null;
                                updateCounts[i] = statements[i].getUpdateCount();
                            }
                        }
                    } while (results[i] == null && moreResults[i]
                            && (options.updateStringency == 0 || (options.updateStringency == 1 && updateCounts[i] <= 0)));
                } catch (SQLException e) {
                    gotException = true;
                    exceptions[i] = e;
                }
            }

            // Ok. At this point, we have either
            //   1. An exception
            //   2. A result set set
            //   3. An update count
            //   4. End of results
            //
            // The first step it to determine that each session is at the
            // same point.
            if (ok) {
                for (int i = 0; i < sessions.length; i++) {
                    if (exceptions[i] != null) {
                        resultState[i] = "Exception";
                    } else if (results[i] != null) {
                        resultState[i] = "Row results";
                    } else if (!moreResults[i]) {
                        resultState[i] = "Query complete";
                    } else if (updateCounts[i] != -999) {
                        resultState[i] = "Update count";
                    } else {

                        // Unless the logic above is fubar, then this should never be hit.
                        resultState[i] = "Unknown";
                    }
                    if (i > 0 && !resultState[i].equals(resultState[0])) {
                        ok = false;
                    }
                }
                if (!ok) {
                    System.err.println("Query state differs:");
                    for (int i = 0; i < sessions.length; i++) {
                        System.err.println("   Session #" + sessions[i].getId() + ": " + resultState[i]);
                    }
                }
            }

            // If every session is in the same state, then check the contents of whatever that state is.
            if (ok) {
                ok = compareExceptions(sessions, exceptions);
            }
            if (ok && !gotException) {
                ok = compareUpdateCount(sessions, updateCounts);
            }
            if (ok && !gotException) {
                ok = compareMoreResults(sessions, moreResults);
            }
            if (ok && !gotException && results[0] != null) {
                ok = compareResults(sessions, results);
                close(results);
            }
            if (ok && !gotException && moreResults[0] == false) {
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
    private boolean compareResults(Session[] sessions, ResultSet[] results) {
        SQLException[] exceptions = new SQLException[sessions.length];
        boolean ok = true;
        boolean done = false;
        boolean gotException = false;
        int rowCount = 0;

        // First, make sure that the metadata is the same across all  of our result sets. No bother comparing data
        // if they don't match.
        if (!compareMetadata(sessions, results)) {
            return false;
        }
        while (ok && !done) {

            // Call next() on each result.
            boolean[] isNext = new boolean[results.length];
            for (int i = 0; i < results.length; i++) {
                try {
                    isNext[i] = results[i].next();
                    if (i > 0 && isNext[i] != isNext[0]) {
                        ok = false;
                    }
                } catch (SQLException e) {
                    exceptions[i] = e;
                    gotException = true;
                }
            }
            if (!ok) {
                System.err.println("Number of available rows differs:");
                for (int i = 0; i < sessions.length; i++) {
                    System.err.println("   Session #" + sessions[i].getId() + ": "
                            + (isNext[i] ? "More rows" : "No more rows"));
                }
            }

            // If we got an exception, stop but compare that we got the same exception.
            if (ok) {
                if (gotException) {
                    ok = compareExceptions(sessions, exceptions);
                    done = true;
                } else {
                    if (isNext[0]) {
                        ++rowCount;
                        ok = compareRow(sessions, results, rowCount);
                    } else {
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
    private boolean compareRow(Session[] sessions, ResultSet[] results, int rowCount) {
        SQLException[] exceptions = new SQLException[sessions.length];
        boolean gotException = false;
        boolean ok = true;
        int badColumn = -1;
        Object[][] values = new Object[results.length][];

        // Suck in the values for each row of each session.
        for (int i = 0; i < results.length; i++) {
            try {
                values[i] = getRow(results[i]);
            } catch (SQLException e) {
                gotException = true;
                exceptions[i] = e;
            }
        }

        // If they got an exception we are finished.
        if (gotException) {
            return (compareExceptions(sessions, exceptions));
        }

        // Now, compare our results.
        for (int i = 1; ok && i < results.length; i++) {
            for (int c = 0; ok && c < values[i].length; c++) {
                if (!values[i][c].equals(values[0][c])) {
                    ok = false;
                    badColumn = c;
                }
            }
        }
        if (!ok) {
            System.err.println("Value contained in row #" + rowCount + ", column #" + (badColumn + 1) + " differs:");
            for (int i = 0; i < results.length; i++) {
                System.err.println("   Session #" + sessions[i].getId() + ": " + values[i][badColumn].toString());
            }
        }
        return ok;
    }

    private Object[] getRow(ResultSet set) throws SQLException {
        ResultSetMetaData meta = set.getMetaData();
        int ncols = meta.getColumnCount();
        Object[] values = new Object[ncols];
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
                    ByteFormatter f = new ByteFormatter(meta.getColumnDisplaySize(c));
                    values[c - 1] = f.format(set.getObject(c));
                    break;
                case Types.BLOB:
                    BlobFormatter b = new BlobFormatter();
                    values[c - 1] = b.format(set.getObject(c));
                    break;
                case Types.CLOB:
                    ClobFormatter cf = new ClobFormatter();
                    values[c - 1] = cf.format(set.getObject(c));
                    break;
                case Types.BOOLEAN:
                    values[c - 1] = set.getBoolean(c);
                    break;
                case Types.CHAR:
                case Types.VARCHAR:
                    // case Types.NVARCHAR
                    // case Types.LONGNVARCHAR
                case Types.LONGVARCHAR:
                    // case Types.NCHAR
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
                    values[c - 1] = set.getLong(c);
                    break;
                default:
                    System.err.println("WARNING: I do not understand " + "datatype #" + type
                            + " (" + meta.getColumnTypeName(c) + ") in column #" + c + ". No comparison will be performed.");
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
    private boolean compareMetadata(Session[] sessions, ResultSet[] results) {
        boolean ok = true;
        try {
            ResultSetMetaData meta = results[0].getMetaData();

            // First check the column count.
            for (int i = 1; ok && i < results.length; i++) {
                if (meta.getColumnCount() != results[i].getMetaData().getColumnCount()) {
                    ok = false;
                }
            }
            if (!ok) {
                System.err.println("Column count differs in results");
                for (int i = 0; i < results.length; i++) {
                    System.err.println("   Session #" + sessions[i].getId() + ": " + results[i].getMetaData().getColumnCount() + " column(s)");
                }
                return false;
            }

            // Now that we can be confident the column counts match we'll check that other metadata matches. I cheat a bit
            // here and build a string to describe the metadata and then compare the strings. This allows me to easily add
            // or remote items that I want to compare.
            meta = results[0].getMetaData();
            int cols = meta.getColumnCount();
            String[] descriptions = new String[results.length];
            int badColumn = -1;
            for (int c = 1; ok && c <= cols; c++) {
                for (int i = 0; i < results.length; i++) {
                    meta = results[i].getMetaData();
                    StringBuilder sb = new StringBuilder();
                    sb.append("Type #").append(meta.getColumnType(c))
                            .append(" (").append(SQLTools.getTypeName(meta.getColumnType(c))).append(")");
                    if (meta.getColumnType(c) == Types.NUMERIC || meta.getColumnType(c) == Types.DECIMAL) {
                        sb.append(", Precision=").append(meta.getPrecision(c)).append(", Scale=").append(meta.getScale(c));
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
                System.err.println("Datatype differs in column #" + badColumn);
                for (int i = 0; i < sessions.length; i++) {
                    System.err.println("   Session #" + sessions[i].getId() + ": " + descriptions[i]);
                }
                return false;
            }
        } catch (SQLException e) {
            System.err.println("WARNING: Exception while retrieving result set metadata. I cannot do a proper diff "
                + "if this " + "occurs: " + e.getMessage());
        }
        return true;
    }

    /**
     * Compares the update counts of a set of statements.
     *
     * @param sessions The sessions
     * @param updateCounts The update counts for the sessions.
     * @return True if they are the same
     */
    private boolean compareUpdateCount(Session[] sessions, int[] updateCounts) {
        boolean ok = true;
        for (int i = 1; ok && i < sessions.length; i++) {
            if (updateCounts[i] != updateCounts[0]) {
                ok = false;
            }
        }
        if (!ok) {
            System.err.println("Update count differs:");
            for (int i = 0; i < updateCounts.length; i++) {
                System.err.println("   Session #" + sessions[i].getId() + ": " + updateCounts[i] + " row(s)");
            }
        }
        return ok;
    }

    /**
     * Checks to see if there are more results.
     *
     * @param sessions The sessions
     * @param moreResults The more results flags for the sessions.
     * @return true if they match, false if they don't if the statements differ about ti.
     */
    private boolean compareMoreResults(Session[] sessions, boolean[] moreResults) {
        boolean ok = true;
        for (int i = 1; ok && i < sessions.length; i++) {
            if (moreResults[i] != moreResults[0]) {
                ok = false;
            }
        }
        if (!ok) {
            System.err.println("Number of result sets differ:");
            for (int i = 0; i < sessions.length; i++) {
                System.err.println("   Session #" + sessions[i].getId() + ": " + (moreResults[i] ? "Results pending" : "No more results"));
            }
        }
        return ok;
    }

    /**
     * Compares the exceptions returned by our sessions.
     *
     * @param sessions The sessions.
     * @param exceptions The exceptions
     * @return true if they are the same.
     */
    private boolean compareExceptions(Session[] sessions, SQLException[] exceptions) {
        boolean ok = true;
        String[] descriptions = new String[exceptions.length];
        for (int i = 0; i < exceptions.length; i++) {
            if (exceptions[i] == null) {
                descriptions[i] = "null";
            } else {
                StringBuilder sb = new StringBuilder();
                if (options.exceptionStringency == 0) {
                    sb.append("exception");
                } else if (options.exceptionStringency > 1) {
                    sb.append("[State ").append(exceptions[i].getSQLState()).append("]: ");
                } else if (options.exceptionStringency > 2) {
                    sb.append("[Code ").append(exceptions[i].getErrorCode()).append("]: ");
                }
                if (options.exceptionStringency > 0) {
                    sb.append(exceptions[i].getMessage());
                }
                descriptions[i] = sb.toString();
            }
            if (i > 0 && !descriptions[0].equals(descriptions[i])) {
                ok = false;
            }
        }
        if (!ok) {
            System.err.println("SQL exceptions differ:");
            for (int i = 0; i < exceptions.length; i++) {
                System.err.println("   Session #" + sessions[i].getId() + ": " + descriptions[i]);
            }
        }
        return ok;
    }

    private void close(Statement[] statements) {
        for (int i = 0; i < statements.length; i++) {
            SQLTools.close(statements[i]);
            statements[i] = null;
        }
    }

    private void close(ResultSet[] results) {
        for (int i = 0; i < results.length; i++) {
            SQLTools.close(results[i]);
            results[i] = null;
        }
    }
}

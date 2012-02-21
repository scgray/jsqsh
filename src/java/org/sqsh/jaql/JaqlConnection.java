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
package org.sqsh.jaql;

import java.io.IOException;

import org.sqsh.ConnectionContext;
import org.sqsh.SQLRenderer;
import org.sqsh.Session;
import org.sqsh.input.completion.Completer;
import org.sqsh.util.TimeUtils;

import com.ibm.jaql.io.converter.JsonToStream;
import com.ibm.jaql.json.type.JsonArray;
import com.ibm.jaql.json.type.JsonRecord;
import com.ibm.jaql.json.type.JsonValue;
import com.ibm.jaql.json.util.JsonIterator;
import com.ibm.jaql.lang.JaqlQuery;
import com.ibm.jaql.predict.JobInfo;
import com.ibm.jaql.predict.ProgressEstimation;

/**
 * Used to allow a Jaql instance to be used like a JDBC connection (well,
 * sort of).
 */
public class JaqlConnection
    extends ConnectionContext {
    
    private Session session;
    private JaqlQuery engine;
    private JaqlFormatter formatter;
    private String oldPrompt = null;
    private long sleepTime = 10000;
    
    public JaqlConnection (Session session, JaqlQuery engine, 
        JaqlFormatter formatter) {
 
        this.session     = session;
        this.engine      = engine;
        this.formatter   = formatter;
        
        String jaqlPrompt = session.getVariable("jaql_prompt");
        if (jaqlPrompt == null) {
            
            jaqlPrompt = "jaql [$lineno]>";
        }
        oldPrompt = session.getVariable("prompt");
        session.setVariable("prompt", jaqlPrompt);
        
        String str = session.getVariable("jaql_sleeptime");
        if (str != null) {
            
            sleepTime = Long.parseLong(str);
        }
    }
    
    /**
     * @return The Jaql execution engine.
     */
    public JaqlQuery getEngine() {
        
        return engine;
    }
    

    /**
     * Performs very very rudamentary parsing of Jaql code looking to see
     * that a terminator is at the end of the current line and isn't contained
     * in a quoted string or comment.
     * 
     * @param batch The current batch string.
     * @param terminator The terminator character
     */
    @Override
    public boolean isTerminated(String batch, char terminator) {
        
        int len = batch.length();
        int idx = 0;
        
        while (idx < len) {
            
            char ch = batch.charAt(idx);
            if (ch == '"' || ch == '\'') {
                
                idx = skipString(batch, len, idx+1, ch);
            }
            else if (ch == '/') {
                
                ++idx;
                if (idx < len) {
                    
                    if (batch.charAt(idx) == '/') {
                    
                        for (++idx; idx < len 
                            && batch.charAt(idx) != '\n'; ++idx) {
                            /* EMPTY */
                        }
                            
                        ++idx;
                    }
                    else if (batch.charAt(idx) == '*') {
                        
                        ++idx;
                        idx = skipComment(batch, len, idx);
                    }
                }
            }
            else if (ch == terminator) {
                
                for (++idx; idx < len 
                    && Character.isWhitespace(batch.charAt(idx)); ++idx);
                
                if (idx >= len)
                    return true;
            }
            else {
                
                ++idx;
            }
        }
        
        return false;
    }
    
    
    /**
     * Returns true if the terminator is not the Jaql terminator.
     */
    @Override
    public boolean isTerminatorRemoved(char terminator) {

        return terminator != ';';
    }
    
    /**
     * Returns a Jaql tab completer. This completer attempts to complete
     * the current word from the set of Jaql global variables.
     */
    @Override
    public Completer getTabCompleter(Session session, String line,
                    int position, String word) {

        return new JaqlCompleter(engine, session, line, position, word);
    }

    /**
     * Given a quoted string, skip it.
     * @param s The string being parsed
     * @param len The length of the string being parsed
     * @param idx The current location. This should be just after the 
     *   initial quote
     * @param quoteType Type of quote
     * @return The location immediately after the closing quote
     */
    private int skipString(String s, int len, int idx, char quoteType) {
        
        while (idx < len) {
            
            char ch = s.charAt(idx);
            if (ch == quoteType) {
                
                ++idx;
                break;
            }
            else if (ch == '\\') {
                
                idx += 2;
            }
            else {
                
                ++idx;
            }
        }
        
        return idx;
    }
    
    /**
     * Skip to the end of a C-style comment 
     * @param s The string being parsed.
     * @param len The length of the string.
     * @param idx The current index into the string, just after the
     *   opening comment location
     * @return The end of the comment (after the close)
     */
    private int skipComment(String s, int len, int idx) {
        
        while (idx < len) {
            
            char ch = s.charAt(idx);
            if (ch == '*') {
                
                ++idx;
                ch = s.charAt(idx);
                if (ch == '/') {
                    
                    ++idx;
                    break;
                }
            }
            else {
                
                ++idx;
            }
        }
        
        return idx;
    }
    
    @Override
    public void eval(String batch, Session session, SQLRenderer renderer)
        throws Exception {
        
        long start = System.currentTimeMillis();
        long stop;
        engine.setQueryString(batch);
        
        int totalRows = 0;
        
        long currentStart = start;
        int nResults = 0;
        try {
            
            while (engine.moveNextQuery()) {
                
                /*
                 * Check to see if we have been requested to track progress.
                 */
                ProgressEstimation progress = engine.getProgressEstimation();
                if (progress != null && progress.getWorkLeft() < 1.0) {
                    
                    double oldWorkLeft = Double.NaN;
                    int    oldJobsLeft = -1;
                    while (progress.getMRJobsLeft() > 0) {
                        
                        try {
                            
                            Thread.sleep(sleepTime);
                        }
                        catch (InterruptedException e) {
                            
                            break;
                        }
                    
                        double workLeft = progress.getWorkLeft();
                        int jobsLeft = progress.getMRJobsLeft();
                        if (workLeft != oldWorkLeft || jobsLeft != oldJobsLeft) {
                            
                            session.out.printf(
                               "[%1$d job%2$s, %3$.1f%% complete]\n",
                               jobsLeft,
                               (jobsLeft != 1 ? "s" : ""),
                               workLeft * 100.0);
                            
                            oldWorkLeft = workLeft;
                            oldJobsLeft = jobsLeft;
                        }
                    }
                }
                
                int nrows = formatter.write(engine.currentQuery());
                
                long currentStop = System.currentTimeMillis();
                
                if (session.isInteractive()
                    && renderer.isShowTimings()
                    && (nrows > 0 || (currentStop - currentStart) > 10L)) {
                    
                    session.out.println("("
                       + nrows
                       + " row"
                       + ((nrows != 1) ? "s in " : " in ")
                       + TimeUtils.millisToDurationString(currentStop - currentStart)
                       + ")");
                }
                
                currentStart = System.currentTimeMillis();
                totalRows += nrows;
                ++nResults;
            }
        }
        catch (Throwable e) {
            
            session.printException(e);
        }
        
        stop = System.currentTimeMillis();
        
        /*
         * Only display counts in interactive mode and if show timings
         * is enabled, if rowcounts are zero we'll also supress timings 
         * that are very low, because the assumption is this is something
         * like a simple assignment.
         */
        if (nResults > 1
           && session.isInteractive() && renderer.isShowTimings()
           && (totalRows > 0 || (stop - start) > 10L)) {
            
            session.out.println(nResults + " results ("
               + totalRows
               + " total row"
               + ((totalRows != 1) ? "s in " : " in ")
               + TimeUtils.millisToDurationString(stop - start)
               + ")");
        }
    }
    
    @Override
    public String toString() {

        return "Jaql";
    }

    @Override
    public void close() {
        
        if (oldPrompt != null) {
            
            session.setVariable("prompt", oldPrompt);
        }

        try {
            
            engine.close();
            engine = null;
        }
        catch (IOException e)
        {
            /* IGNORED */
        }
    }
}

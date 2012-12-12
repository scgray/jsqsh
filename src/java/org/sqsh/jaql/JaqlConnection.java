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
package org.sqsh.jaql;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sqsh.CannotSetValueError;
import org.sqsh.ConnectionContext;
import org.sqsh.SQLRenderer;
import org.sqsh.Session;
import org.sqsh.Style;
import org.sqsh.input.completion.Completer;
import org.sqsh.signals.SignalManager;
import org.sqsh.util.TimeUtils;

import com.ibm.jaql.json.type.BufferedJsonRecord;
import com.ibm.jaql.json.type.JsonArray;
import com.ibm.jaql.json.type.JsonString;
import com.ibm.jaql.json.type.JsonValue;
import com.ibm.jaql.json.type.MutableJsonString;
import com.ibm.jaql.json.util.JsonIterator;
import com.ibm.jaql.lang.ExceptionHandler;
import com.ibm.jaql.lang.JaqlQuery;
import com.ibm.jaql.lang.core.Context;
// import com.ibm.jaql.predict.ProgressEstimation;
import com.ibm.jaql.util.FastPrintStream;

/**
 * Used to allow a Jaql instance to be used like a JDBC connection (well,
 * sort of).
 */
public class JaqlConnection
    extends ConnectionContext {
    
    private static final Logger LOG =
            Logger.getLogger(JaqlConnection.class.getName());
    
    /**
     * Used to capture and process exceptions from a Jaql statement.
     */
    private static class JaqlExceptionHandler
        extends ExceptionHandler {
        
        private boolean handledException = false;
        private Session session;
        
        public JaqlExceptionHandler (Session session) {

            this.session = session;
        }
        
        public void clear() {

            this.handledException = false;
        }
        
        public boolean handledException() {

            return handledException;
        }

        @Override
        public void handleException(Throwable e, JsonValue ctx) throws Exception {

            session.printException(e);
            if (ctx != null) {
                
                session.err.println("Current value: " + ctx.toString());
            }
            handledException = true;
        }
    }
    
    private JaqlQuery engine;
    private JaqlFormatter formatter;
    private String oldPrompt = null;
    private long sleepTime = 10000;
    
    /*
     * Record that is used to configure job properties.
     */
    private BufferedJsonRecord confRecord  = new BufferedJsonRecord(1);
    private BufferedJsonRecord confValues  = new BufferedJsonRecord(2);
    private MutableJsonString  jobName     = new MutableJsonString("");
    private JaqlExceptionHandler exceptionHandler;
    
    private static class JaqlStyle
        extends Style {
        
        JaqlFormatter formatter;
        
        public JaqlStyle (JaqlFormatter formatter) {
            
            super(formatter.getName());
            this.formatter = formatter;
        }
        
        public JaqlFormatter getFormatter() {
            
            return formatter;
        }
    }
    
    public JaqlConnection (Session session, JaqlQuery engine, 
        JaqlFormatter formatter) {
 
        super(session);
        
        this.engine           = engine;
        this.formatter        = formatter;
        this.exceptionHandler = new JaqlExceptionHandler(session);
        
        setExceptionHandler();
        
        String jaqlPrompt = session.getVariable("jaql_prompt");
        if (jaqlPrompt == null) {
            
            jaqlPrompt = "jaql [$histid:$lineno]>";
        }
        oldPrompt = session.getVariable("prompt");
        session.setVariable("prompt", jaqlPrompt);
        
        String str = session.getVariable("jaql_sleeptime");
        if (str != null) {
            
            sleepTime = Long.parseLong(str);
        }
        
        /*
         * Create the job property configuration record.
         */
        confRecord.add(new JsonString("conf"), confValues);
        confValues.add(new JsonString("mapred.job.name"), jobName);
    }
    
    private void setExceptionHandler() {
        
        /*
         * There was a problem where the setExceptionHandler() method was
         * not public in one version of Jaql, so I test for it here.
         */
        try {
            
            Method m = JaqlQuery.class.getMethod("setExceptionHandler", 
                            ExceptionHandler.class);
            if (m.isAccessible()) {
                
                engine.setExceptionHandler(exceptionHandler);
            }
            else {
                
                m.setAccessible(true);
                m.invoke(engine, exceptionHandler);
            }
        }
        catch (Throwable e) {
            
            /*
             * If the method doesn't exist at all, then our handler will never
             * get called and we will handle it locally here in jsqsh.
             */
        }
    }
    
    public Style getStyle() {

        return new JaqlStyle(formatter);
    }

    @Override
    public void setStyle(Style style) {

        this.formatter = ((JaqlStyle)style).getFormatter();
    }
    
    @Override
    public void setStyle(String name) {

        JaqlFormatter formatter = JsonFormatterFactory.getFormatter(session, name);
        if (formatter == null) {
            
            throw new CannotSetValueError("Display style '" + name
                 + "' is not valid for Jaql sessions. See \"help \\style\"");
        }
        
        this.formatter = formatter;
    }

    public JaqlFormatter getFormatter() {
        
        return formatter;
    }
    
    public void setFormatter (JaqlFormatter formatter) {
        
        this.formatter = formatter;
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
     * Returns all of the currently defined global variables.
     */
    @Override
    public List<String> getGlobals() {
        
        ArrayList<String> vars = new ArrayList<String>();

        /*
         * Yuck. The distinct() is because of a bug in listVariables()
         */
        engine.setQueryString("distinct(listVariables().var)");
        try {
        
            JsonValue v = engine.evaluate();
            if (!(v instanceof JsonArray)) {
                
                session.err.println("Expected JsonArray from listVariables()!!");
            }
            else {
                
                JsonIterator iter = ((JsonArray)v).iter();
                while (iter.moveNext()) {
                    
                    vars.add(iter.current().toString());
                }
            }
        }
        catch (Exception e) {
            
            session.setException(e);
            session.err.println(e.getMessage());
        }
        
        return vars;
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
    public boolean supportsQueryTimeout() {

        return false;
    }

    @Override
    public void cancel() throws Exception {

        Context.current().interrupt();
    }

    @Override
    public void evalImpl(String batch, Session session, SQLRenderer renderer)
        throws Exception {
        
        long start = System.currentTimeMillis();
        long stop;
        boolean showFooters = session.getRendererManager().isShowFooters();
        FastPrintStream out = new FastPrintStream(session.out);
        
        /*
         * Install our signal handler.
         */
        SignalManager sigMan = SignalManager.getInstance();
        JaqlSignalHandler sigHandler = 
            new JaqlSignalHandler(Context.current());
        sigMan.push(sigHandler);
        
        /*
         * The formatter polls the signal handler to find out if it 
         * should stop writing.
         */
        formatter.setSignalHandler(sigHandler);
        
        if (LOG.isLoggable(Level.FINE)) {
            
            LOG.fine("Executing: [" + batch + "]");
        }
        
        setJobConf(session);
        
        exceptionHandler.clear();
        
        engine.setQueryString(batch);
        
        int totalRows = 0;
        
        long currentStart = start;
        int nResults = 0;
        try {
            
            /*
             * Start the visual timer.
             */
            session.startVisualTimer();
            
            while (!sigHandler.isTriggered() && engine.moveNextQuery()) {
                
                LOG.fine("==== JAQL RESULT =====");
                
                /*
                 * Check to see if we have been requested to track progress.
                 */
                /*
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
                */
                
                JsonIterator iter = new JsonIteratorWrapper(session, engine.currentQuery());
                
                int nrows = formatter.write(out, iter);
                
                long currentStop = System.currentTimeMillis();
                
                if (session.isInteractive()
                    && renderer.isShowTimings()
                    && showFooters
                    && (nrows > 0 || (currentStop - currentStart) > 10L)) {
                    
                    session.err.println("("
                       + (sigHandler.isTriggered() ? "Canceled at " : "")
                       + nrows
                       + " row"
                       + ((nrows != 1) ? "s in " : " in ")
                       + TimeUtils.millisToDurationString(currentStop - currentStart)
                       + ")");
                }
                
                currentStart = System.currentTimeMillis();
                totalRows += nrows;
                ++nResults;
                
                /*
                 * There may be another query, so crank the timer back up.
                 */
                session.startVisualTimer();
            }
        }
        catch (Throwable e) {
            
            if (!exceptionHandler.handledException()) {
                
                session.printException(e);
            }
        }
        finally {
            
            session.stopVisualTimer();
        }
        
        /*
         * Make sure we uninstall our signal handler.
         */
        sigMan.pop();
        
        stop = System.currentTimeMillis();
        
        /*
         * Only display counts in interactive mode and if show timings
         * is enabled, if rowcounts are zero we'll also supress timings 
         * that are very low, because the assumption is this is something
         * like a simple assignment.
         */
        if (!sigHandler.isTriggered()
           && nResults > 1
           && session.isInteractive() && renderer.isShowTimings()
           && showFooters
           && (totalRows > 0 || (stop - start) > 10L)) {
            
            session.err.println(nResults + " results ("
               + totalRows
               + " total row"
               + ((totalRows != 1) ? "s in " : " in ")
               + TimeUtils.millisToDurationString(stop - start)
               + ")");
        }
    }
    
    /**
     * Attempts to configure the Jaql jobname.
     * @param session The name of the session.
     */
    private void setJobConf(Session session) {
        
        String name = session.getVariable("jaql_jobname");
        if (name != null) {
            
            jobName.setCopy(session.expand(name));
            
            Context context = Context.current();
            try {
                
                context.setOptions(confRecord);
            }
            catch (Exception e) {
                
                session.printException(e);
            }
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
    
    /**
     * Simple wrapper around the data iterator that will automatically shut
     * off the visual timer as soon as the first item has been fetched from
     * the actual iterator.
     */
    private static class JsonIteratorWrapper
        extends JsonIterator
    {
        JsonIterator iter;
        Session session;
        boolean stoppedTimer = false;
        
        public JsonIteratorWrapper(Session session, JsonIterator iter) {
            
            this.session = session;
            this.iter = iter;
        }

        @Override
        public boolean moveNext() throws Exception {
            
            boolean hasNext = iter.moveNext();
            
            if (!stoppedTimer) {
                
                session.stopVisualTimer();
                stoppedTimer = true;
            }
            
            if (!hasNext)
                return false;
            
            currentValue = iter.current();
            return true;
        }
    }
}

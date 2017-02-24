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
package org.sqsh;

import java.util.ArrayList;
import java.util.List;

import org.sqsh.input.completion.Completer;
import org.sqsh.input.completion.NullCompleter;

/**
 * "Connections" are theoretically abstract things, and this is the base
 * representation of a connection.  For most of jsqsh, a connection is a 
 * {@link SQLConnectionContext}, however it is possible that you could have
 * other types of "connections"--to wrap jsqsh around a scripting language
 * where the current batch is "evaled" in the language, then an
 * implementation of this object would contain a handle to the scripting
 * engine.
 */
public abstract class ConnectionContext {
    
    /**
     * Parent session.
     */
    protected Session session;
    
    /**
     * If set to a value &gt; 0, then the eval() method should time out after
     * the specified number of seconds.  Not call connections need support
     * this. If the context does not support it 
     */
    protected int timeout = 0;
    
    /**
     * If a timeout has been scheduled, this is the thread waiting to do the
     * dirty work and cancel the query when requested.
     */
    private TimeoutThread timeoutThread = null;
    
    /**
     * If true, then JSqsh will take care of doing the timeout via the timeout
     * thread whether or not the connection is capable of doing it itself.
     */
    private boolean forceAssistedTimeout = false;
    
    /**
     * Create a context
     * @param session The session that created us
     */
    public ConnectionContext (Session session) {
        
        this.session = session;
        
        /*
         * Transfer default context settings to this connection.
         */
        this.timeout = session.getContext().getQueryTimeout();
    }
    
    /**
     * @return The session to which this connection belongs.
     */
    public Session getSession() {
        
        return session;
    }
    
    /**
     * @return The number of seconds before a query should timeout. A 
     *   value &lt;= 0 indicates that the query should run indefinitely
     */
    public int getQueryTimeout() {
    
        return timeout;
    }
    
    /**
     * @param truefalse If true, then jsqsh will launch a thread to cancel
     *   the current query when the timeout period has been reached (see
     *   {@link #setQueryTimeout(int)}) rather than asking the underlying
     *   connection implementation to do the cancel.
     */
    public void setForceAssistedTimeout(boolean truefalse) {
        
        this.forceAssistedTimeout = truefalse;
    }
    
    /**
     * @return true if timeouts will be assisted
     */
    public boolean isForceAssistedTimeout() {
        
        return this.forceAssistedTimeout;
    }

    /**
     * Sets the query timeout period (in seconds). 
     * @param timeout The number of seconds to wait before timeout.
     *   A value &lt;= 0 indicates to wait indefinitely.
     */
    public void setQueryTimeout(int timeout) {
    
        this.timeout = timeout;
    }
    
    /**
     * Must be implemented to indicate whether or not a connection type is
     * capable of implementing the query timeout facility.  If the answer is
     * no ("false") then a thread will be automatically started to attempt to
     * {@link #cancel()} the query at the requested timeout period.
     * 
     * @return true if it is supported, false otherwise.
     */
    public abstract boolean supportsQueryTimeout();
    
    /**
     * This method can be called during {@link #evalImpl(String, Session, SQLRenderer)}
     * of connections that declare that they support query timeout (i.e.
     * {@link #supportsQueryTimeout()} returns true), but they discover that
     * they cannot support it after all. This will cause a {@link #cancel()}
     * to be scheduled at the current query timeout period. The query timeout
     * will automatically be aborted when <code>evalImpl</code> returns.
     */
    protected final void startQueryTimeout() {
        
        if (timeoutThread == null) {
            
            timeoutThread = new TimeoutThread();
            timeoutThread.start();
        }
    }
    
    /**
     * Evaluates a batch of SQL (or something else) on the connection.
     * 
     * @param batch The batch of text to be evaluated.
     * @param session A handle to the executing session.
     * @param renderer How the results should be rendered (if applicable to
     *   this type of connection).
     */
    public final void eval (String batch, Session session, SQLRenderer renderer)
        throws Exception {
        
        if (timeout > 0 
            && (!supportsQueryTimeout() || this.forceAssistedTimeout)) {
            
            timeoutThread = new TimeoutThread();
            timeoutThread.start();
        }
        
        try {
            
            evalImpl(batch, session, renderer);
        }
        finally {
        
            /*
             * Timeout thread was started...
             */
            if (timeoutThread != null) {
                
                /*
                 * If it is still running, our query finished before it did
                 * which is good!
                 */
                if (timeoutThread.isAlive()) {
                    
                    /*
                     * Ask it to shut down
                     */
                    timeoutThread.interrupt();
                    try {
                        
                        /*
                         * Wait for it to finish
                         */
                        timeoutThread.join();
                    }
                    catch (InterruptedException e) {
                        
                        /* IGNORED */
                    }
                }
                
                if (timeoutThread.isTimedOut()) {
                    
                    session.err.println("Query canceled due to timeout (" 
                    + timeout + " ms.)");
                }
                
                timeoutThread = null;
            }
        }
    }

    /**
     * Evaluates a batch of SQL (or something else) on the connection.
     * 
     * @param batch The batch of text to be evaluated.
     * @param session A handle to the executing session.
     * @param renderer How the results should be rendered (if applicable to
     *   this type of connection).
     */
    public abstract void evalImpl (String batch, Session session, SQLRenderer renderer)
        throws Exception;
    
    /**
     * Cancels the currently executing statement. This is the only method that
     * can be called by another thread.
     * 
     * @throws Exception Thrown if the cancel cannot be performed.
     */
    public abstract void cancel()
        throws Exception;
    
    /**
     * Retrieve the current display style for the connection. This is an
     * abstract representation of the style. Under the hood is carries enough
     * information to restore the style to its original state with 
     * {@link #setStyle(Style)}.
     * 
     * @return The current display style.
     */
    public abstract Style getStyle();
    
    /**
     * Sets the display style. This *must* be a style that is returned from
     * {@link #getStyle()} from this connection.
     * 
     * @param style The style to set.
     */
    public abstract void setStyle(Style style);
    
    /**
     * Attempts to set the display style for the connection by name. 
     * @param name The name of the style
     */
    public abstract void setStyle(String name);
    
    /**
     * Returns whether or not the connection wants the terminator character
     * removed from the buffer before executing it.
     * 
     * @return true or false
     */
    public boolean isTerminatorRemoved(char terminator) {
        
        return true;
    }

    /**
     * Analyzes a block of text to see if it is terminated with the provided
     * terminator character (presumably the character is a semicolon most of
     * the time). 
     * 
     * @param batch The batch to analyze
     * @param terminator The terminator character
     * @return True if the batch is terminated.
     */
    public abstract boolean isTerminated(CharSequence batch, char terminator);
    
    /**
     * Returns a tab word completer for the current connection type.
     * The default implementation returns a NullTabCompleter which
     * effectively disables tab completion altogether.
     * 
     * @param session The current session
     * @param line The line being typed into when the tab was struck
     * @param position The current cursor position in the line
     * @param word The current word at the point the tab was struct
     * @return The completer
     */
    public Completer getTabCompleter (Session session, String line, 
        int position, String word) {
        
        return new NullCompleter(session, line, position, word);
    }
    
    /**
     * Returns global variables/objects. This is primarily intended for
     * use by scripting-language "connections" and is used by the \globals
     * command.
     * @return The list of globals. The default implementation will return
     *   an empty list.
     */
    public List<String> getGlobals() {
        
        return new ArrayList<String>(0);
    }
    
    /**
     * Every sub-class of ConnectionContext needs to override this
     * method to return a simple, single line string describing what the
     * session is connected to.
     */
    @Override
    public String toString() {
        
        return "IMPLEMENT ME";
    }
    
    /**
     * This method will be called when the connection is to be closed.
     * Implementations should override this method if necessary.
     */
    public void close() {
        
        /* EMPTY */
    }
    
    /**
     * Simple internal class to schedule a timeout (cancel) of a query
     */
    private class TimeoutThread
        extends Thread
    {
        private boolean didTimeout = false;
        
        public TimeoutThread() {
            
            super();
            setDaemon(true);
            setName("JSqsh Query Timeout");
        }
        
        @Override
        public void run() {
            
            try {
                
                Thread.sleep(timeout * 1000L);
                didTimeout = true;
                
                try {
                    
                    cancel();
                }
                catch (Exception e) {
                    
                    /* IGNORED */
                }
            }
            catch (InterruptedException e) {
                
                /* We were asked to abort */
            }
        }
        
        public boolean isTimedOut() {
            
            return didTimeout;
        }
    }
}

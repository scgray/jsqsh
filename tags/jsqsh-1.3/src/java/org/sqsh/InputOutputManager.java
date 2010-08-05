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
package org.sqsh;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The InputOutputManager provides a mechanism by which the state of
 * a sessions input, output, and error handles can be saved and restored
 * as necessary. This is useful for cases where a jsqsh command may 
 * need to redirect the I/O handles for the duration of the command
 * but wishes to conveniently restore them when complete.
 * 
 * <p>Each time a new I/O handle is assigned, it can be handed a flag
 * that indicates if the handle should be closed when it is no longer
 * referenced by the manager.  A handle is no longer referenced when
 * it is replaced by another call to setOut(), setErr() or setIn() 
 * or if a call to restore() is called and the handle is no longer 
 * in use.
 * 
 * <p>It is up to the user to avoid doing stupid things like calling
 * setOut(System.out, true)--that is, asking for a system handle to
 * be closed when it is finished.
 */
public class InputOutputManager {
    
    private static final Logger LOG = 
        Logger.getLogger(InputOutputManager.class.getName());
    
    private Stack<InputOutputState> ioStack =
        new Stack<InputOutputState>();
    
    public InputOutputManager() {
        
        ioStack.push(new InputOutputState());
    }
    
    /**
     * Returns the number of saved I/O states in the manager.
     * 
     * @return The number of saved I/O states in the manager.
     */
    public int getSaveCount() {
        
        return ioStack.size();
    }
    
    /**
     * Marks whether or not the current input stream should be
     * considered an interactive input stream (i.e. belongs to
     * a terminal).
     * 
     * @param isInteractive True if the stream is interactive.
     */
    public void setInteractive(boolean isInteractive) {
        
        if (LOG.isLoggable(Level.FINE)) {

            LOG.fine("Setting interactive to " + isInteractive);
        }
        
        ioStack.peek().isInteractive = isInteractive;
    }
    
    /**
     * Returns whether or not the current input stream is interactive.
     * 
     * @return true if the session is interactive.
     */
    public boolean isInteractive() {
        
        return ioStack.peek().isInteractive;
    }
    
    /**
     * Assigns a new output stream for the manager. The prior
     * output stream is closed if necessary.
     * 
     * @param out The new output stream
     * @param autoClose A flag to indicate if this output stream
     *   should be closed when it is no longer being referenced.
     *   It is up to the caller to not do stupid things like 
     *   passing System.out in here and marking it as autoclose.
     */
    public void setOut(PrintStream out, boolean autoClose) {
        
        if (LOG.isLoggable(Level.FINE)) {

            LOG.fine("Replacing (autoClose=" + autoClose + ")");
        }
        
        /*
         * Grab the current state of affairs.
         */
        InputOutputState curState = ioStack.peek();
        
        /*
         * Save away the handle we may need to close.
         */
        PrintStream oldOut = curState.out;
        boolean oldAutoClose = curState.autoCloseOut;
        
        /*
         * And replace it with the new handle.
         */
        curState.out = out;
        curState.autoCloseOut = autoClose;
        
        if (oldAutoClose) {
            
            close(oldOut, "output");
        }
    }
    
    /**
     * Returns the current output stream.
     * 
     * @return The current output stream.
     */
    public PrintStream getOut() {
        
        return ioStack.peek().out;
    }
    
    
    /**
     * Assigns a new error stream for the manager. The prior
     * error stream is closed if necessary.
     * 
     * @param err The new error stream
     * @param autoClose A flag to indicate if this error stream
     *   should be closed when it is no longer being referenced.
     */
    public void setErr(PrintStream err, boolean autoClose) {
        
        if (LOG.isLoggable(Level.FINE)) {

            LOG.fine("Replacing (autoClose=" + autoClose + ")");
        }
        
        InputOutputState curState = ioStack.peek();
        
        PrintStream oldErr = curState.err;
        boolean oldAutoClose = curState.autoCloseErr;
        
        curState.err = err;
        curState.autoCloseErr = autoClose;
        
        if (oldAutoClose) {
            
            close(oldErr, "error");
        }
    }
        
    /**
     * Returns the current error stream.
     * 
     * @return The current error stream.
     */
    public PrintStream getErr() {
        
        return ioStack.peek().err;
    }
    
    /**
     * Assigns a new error stream for the manager. The prior
     * error stream is closed if necessary.
     * 
     * @param err The new error stream
     * @param autoClose A flag to indicate if this error stream
     *   should be closed when it is no longer being referenced.
     * @param isInteractive If true, then the input stream is assumed
     *   to be an interactive stream (i.e. comes from a terminal).
     */
    public void setIn(InputStream in, boolean autoClose,
            boolean isInteractive) {
        
        if (LOG.isLoggable(Level.FINE)) {

            LOG.fine("Replacing (autoClose=" + autoClose + ")");
        }
        
        InputOutputState curState = ioStack.peek();
        
        InputStream oldIn = curState.in;
        boolean oldAutoClose = curState.autoCloseIn;
        
        curState.in = in;
        curState.autoCloseIn = autoClose;
        curState.isInteractive = isInteractive;
        
        if (oldAutoClose) {
            
            close(oldIn);
        }
    }
    
    /**
     * Closes an input stream if it is no longer in use.
     * 
     * @param in The stream to close.
     */
    private void close(InputStream in) {
        
        boolean doClose = true;
        for (InputOutputState ioState : ioStack) {
                
            if (ioState.in == in) {
                    
                doClose = false;
                break;
            }
        }
        
        if (doClose) {
            
            LOG.fine("Closing input handle");
            try {
                    
                in.close();
            }
            catch (IOException e) {
                    
                /* IGNORED */
            }
        }
        else {
            
            LOG.fine("Old input handle still in use. Not closing");
        }
    }
    
    /**
     * Closes a print stream if it is no longer in use.
     * 
     * @param stream The stream to close.
     */
    private void close(PrintStream stream, String name) {
        
        boolean doClose = true;
        for (InputOutputState ioState : ioStack) {
                
            if (ioState.err == stream
                    || ioState.out == stream) {
                    
                doClose = false;
                break;
            }
        }
            
        if (doClose) {
            
            LOG.fine("Closing " + name + " handle");
            stream.close();
        }
        else {
                
            LOG.fine("Old " + name + " handle still in use. Not closing");
        }
    }
    
    /**
     * Returns the current input stream.
     * 
     * @return The current input stream.
     */
    public InputStream getIn() {
        
        return ioStack.peek().in;
    }
    
    /**
     * Saves away the state of the current I/O handles 
     */
    public void save() {
        
        if (LOG.isLoggable(Level.FINE)) {

            LOG.fine("Saving I/O state");
        }
        
        ioStack.push((InputOutputState) ioStack.peek().clone());
    }
    
    /**
     * Restores the state.
     */
    public void restore() {
        
        if (LOG.isLoggable(Level.FINE)) {

            LOG.fine("Restoring I/O state");
        }
        
        InputOutputState state = ioStack.pop();
        if (state.autoCloseIn) {
            
            close(state.in);
        }
        if (state.autoCloseOut) {
            
            close(state.out, "output");
        }
        if (state.autoCloseErr) {
        
            close(state.out, "error");
        }
    }
    
    /**
     * Bag to hold push()/pop() state.
     */
    private static class InputOutputState
        implements Cloneable {
        
        public InputStream in = System.in;
        public boolean autoCloseIn = false;
        public boolean isInteractive = true;
        public PrintStream out = System.out;
        public boolean autoCloseOut = false;
        public PrintStream err = System.err;
        public boolean autoCloseErr = false;
        
        public Object clone() {
            
            try {
                
                return super.clone();
            }
            catch (CloneNotSupportedException e) {
                
                /* IGNORED */
            }
            
            return null;
        }
    }
}

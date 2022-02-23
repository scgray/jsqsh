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
package org.sqsh;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The InputOutputManager provides a mechanism by which the state of a sessions input, output, and error handles can be
 * saved and restored as necessary. This is useful for cases where a jsqsh command may need to redirect the I/O handles
 * for the duration of the command but wishes to conveniently restore them when complete.
 *
 * <p>Each time a new I/O handle is assigned, it can be handed a flag
 * that indicates if the handle should be closed when it is no longer referenced by the manager.  A handle is no longer
 * referenced when it is replaced by another call to setOut(), setErr() or setIn() or if a call to restore() is called
 * and the handle is no longer in use.
 *
 * <p>It is up to the user to avoid doing stupid things like calling
 * setOut(System.out, true)--that is, asking for a system handle to be closed when it is finished.
 */
public class InputOutputManager {

    private static final Logger LOG = Logger.getLogger(InputOutputManager.class.getName());

    private final Stack<InputOutputState> ioStack = new Stack<>();

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
     * Marks whether or not the current input stream should be considered an interactive input stream (i.e. belongs to a
     * terminal).
     *
     * @param isInteractive True if the stream is interactive.
     */
    public void setInteractive(boolean isInteractive) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Setting interactive to " + isInteractive);
        }
        InputOutputState state = ioStack.peek();
        state.isInteractive = isInteractive;
    }

    /**
     * Returns whether or not the current input stream is interactive.
     *
     * @return true if the session is interactive.
     */
    public boolean isInteractive() {
        InputOutputState state = ioStack.peek();
        return state.isInteractive;
    }

    /**
     * Assigns a new output stream for the manager. The prior output stream is closed if necessary.
     *
     * @param out The new output stream
     * @param autoClose A flag to indicate if this output stream should be closed when it is no longer being
     *         referenced. It is up to the caller to not do stupid things like passing System.out in here and marking it
     *         as autoclose.
     */
    public void setOut(PrintStream out, boolean autoClose) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Replacing (autoClose=" + autoClose + ")");
        }

        // Grab the current state of affairs.
        InputOutputState curState = ioStack.peek();

        // Save away the handle we may need to close.
        PrintStream oldOut = curState.out;
        boolean oldAutoClose = curState.autoCloseOut;

        // And replace it with the new handle.
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
     * Assigns a new error stream for the manager. The prior error stream is closed if necessary.
     *
     * @param err The new error stream
     * @param autoClose A flag to indicate if this error stream should be closed when it is no longer being
     *         referenced.
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
     * Assigns a new error stream for the manager. The prior error stream is closed if necessary.
     *
     * @param in The new input stream
     * @param autoClose A flag to indicate if this error stream should be closed when it is no longer being
     *         referenced.
     * @param isInteractive If true, then the input stream is assumed to be an interactive stream (i.e. comes
     *         from a terminal).
     */
    public void setIn(BufferedReader in, boolean autoClose, boolean isInteractive) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Replacing (autoClose=" + autoClose + ", isInteractive=" + isInteractive + ")");
        }
        InputOutputState curState = ioStack.peek();
        BufferedReader oldIn = curState.in;
        boolean oldAutoClose = curState.autoCloseIn;
        curState.in = in;
        curState.autoCloseIn = autoClose;
        curState.isInteractive = isInteractive;
        if (oldAutoClose) {
            close(oldIn);
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
            if (ioState.err == stream || ioState.out == stream) {
                doClose = false;
                break;
            }
        }

        if (doClose) {
            LOG.fine("Closing " + name + " handle");
            stream.close();
        } else {
            LOG.fine("Old " + name + " handle still in use. Not closing");
        }
    }

    /**
     * Closes an input stream if it is no longer in use.
     *
     * @param in The stream to close.
     */
    public void close(BufferedReader in) {
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
            } catch (IOException e) {

                /* IGNORED */
            }
        } else {
            LOG.fine("Old input handle still in use. Not closing");
        }
    }

    /**
     * Returns the current input stream.
     *
     * @return The current input stream.
     */
    public BufferedReader getIn() {
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
    private static class InputOutputState implements Cloneable {
        public BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        public boolean autoCloseIn = false;
        public boolean isInteractive = true;
        public PrintStream out = System.out;
        public boolean autoCloseOut = false;
        public PrintStream err = System.err;
        public boolean autoCloseErr = false;

        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                // IGNORED
            }
            return null;
        }
    }
}

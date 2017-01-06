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

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.logging.Logger;

/*
 * NOTE: These classes are available because they are embedded in jline.
 */
import org.sqsh.util.Ansi;
import org.sqsh.util.TimeUtils;

/**
 * Simple thread designed to show a visual query timer on the screen.  By
 * default the timer is disabled and the timer thread is not running, in which
 * case calls to {@link #start()} and {@link #stop} are silently ignored. However,
 * when the visual timer is enabled ({@link #setEnabled(boolean)}, a separate
 * thread is started, and the thread waits for the timer to {@link #start()}.
 * Once started, the thread will wait 5 seconds and then start displaying an
 * elapsed time timer.  This timer will continue to update until {@link #stop()}
 * is called.  Disabling the timer will shut down the support thread until
 * it is re-enabled.
 */
public class VisualTimer {
    
    private static final Logger LOG = Logger.getLogger(VisualTimer.class.getName());
    
    /**
     * Messages that can be sent to the timer thread
     */
    protected static enum Message {
        START_TIMING,  /// Request the timer start
        STOP_TIMING,   /// Request the timer stop
        SHUTDOWN,      /// Tell the timer thread to shut down
        NONE           /// Indicates there are no pending messages
    }
    
    /**
     * Indicates the state of the timer thread
     */
    protected static enum State {
        SHUTDOWN,      /// The timer thread is currently shut down
        TIMING,        /// The timer thread is currently running a timer
        IDLE           /// The timer thread is currently idle (no timer)
    }
    
    /**
     * Seconds that must pass before the visual timer is displayed
     */
    private static long START_DELAY = 5000;
    
    /**
     * This lock is used to control communications concurrency with the timer
     * thread, and is used for the thread and the client to communicate with
     * each other by notifying while on the lock.
     */
    private Object  lock = new Object();
    
    /**
     * Tracks whether or not the timer service is enabled. This is managed
     * completely withint <code>setEnabled()</code>
     */
    private boolean isEnabled = false;
    
    /**
     * This is used to send a message to the timer thread. The message is
     * "sent" by calling {@link #sendMessage(Message)}, which sets this variable
     * then notifies the thread it has been set.  The thread acknowledges the
     * message by setting it back to NONE and then notifying the caller.
     */
    private Message message = Message.NONE;
    
    /**
     * Thread state. This is controlled entirely within the body of the timer
     * thread. 
     */
    private State state = State.SHUTDOWN;

    /**
     * Where to send the output from the timer
     */
    private Writer out;

    /**
     * Creates a visual timer. By default the timer is disabled and calls to
     * {@link #start()} and {@link #stop()} are ignored.
     */
    public VisualTimer(boolean isEnabled, Writer out) {

        this.out = out;
        if (isEnabled) {
            
            setEnabled(isEnabled);
        }
    }
    
    /**
     * Creates a visual timer. By default the timer is disabled and calls to
     * {@link #start()} and {@link #stop()} are ignored.
     */
    public VisualTimer(Writer out) {
        
        this(false, out);
    }
    
    /**
     * Enables or disables the visual timer. Enabling the timer facility 
     * causes a thread to spin up that will be responsible for visually
     * displaying the timer when asked.
     *   
     * @param isEnabled true if the timer is enabled
     */
    public void setEnabled(boolean isEnabled) {
        
        synchronized (lock) {
            
            if (this.isEnabled == isEnabled) {
            
                LOG.fine("Timer is already enabled");
                return;
            }
            
            this.isEnabled = isEnabled;
            if (isEnabled) {
            
                LOG.fine("Requesting timer thread to start up");
                message = Message.NONE;
                
                new TimerThread().start();
            }
            else {
                
                // Let the thread know it needs to shut down
                LOG.fine("Notifying timer thread to shut down");
                sendMessage(Message.SHUTDOWN);
                isEnabled = false;
            }
        }
    }
    
    /**
     * @return true if the timer is enabled, false otherwise
     */
    public boolean isEnabled() {
        
        synchronized (lock) {
            
            return isEnabled;
        }
    }
    
    /**
     * Starts displaying a visual timer.  If the timer service is not enabled,
     * then this call does nothing. If the timer service is enabled, timing 
     * starts and after 5 seconds delay, the timer will begin display.  If
     * {@link #stop()} is called before the 5 second delay, then the timer 
     * will never display (so fast queries never flash up a timer).
     */
    public void start() {
        
        synchronized (lock) {
            
            if (isEnabled) {
                
                sendMessage(Message.START_TIMING);
            }
        }
    }
    
    
    /**
     * Stops displaying a visual timer. Calling this method if the timer 
     * service is disabled or there is no timer currently started will have
     * no effect.
     */
    public void stop() {
        
        synchronized (lock) {
            
            if (isEnabled && state == State.TIMING) {
                
                sendMessage(Message.STOP_TIMING);
            }
        }
    }
    
    /**
     * Used internally to communicate with the timer thread.
     * @param message The message to send
     */
    private void sendMessage(Message message) {
        
        LOG.fine("Sending " + message);
        
        /*
         * Set the message for the thread to receive.
         */
        this.message = message;
        
        /*
         * Notify the timer thread of its availability.
         */
        lock.notify();
        
        /*
         * The timer thread acknowledges this message by clearing the
         * message out (setting it to NONE), so we wait until it does so.
         */
        while (this.message != Message.NONE) {
            
            try {
                
                lock.wait();
            }
            catch (InterruptedException e) {

                /* IGNORED */
            }
            
            LOG.fine("Got acknowledgement");
        }
    }
    
    /**
     * The timer thread itself.
     */
    class TimerThread 
        extends Thread {
        
        
        public TimerThread() {
            
            this.setDaemon(true);
            this.setName("JSqsh Visual Timer");
        }
    
        @Override
        public void run() {
            
            LOG.fine("Timer thread started");
            
            long startTime = 0L;
            long waitTime  = 0L;
            boolean done = false;
            
            synchronized (lock) {
                
                state = VisualTimer.State.IDLE;
                
                while (!done) {
                    
                    if (message != Message.NONE) {
                        
                        LOG.fine("Got message " + message);
                        
                        switch (message) {
                        
                        case START_TIMING: 
                            // If there was a timer going, clear it out
                            clear();
                            
                            state = VisualTimer.State.TIMING;
                            startTime = System.currentTimeMillis();
                            waitTime  = 1000L;
                            break;
                        case STOP_TIMING:
                            // If there was a timer going, clear it out
                            clear();
                            state = VisualTimer.State.IDLE;
                            startTime = 0L;
                            waitTime = 0L;
                            break;
                            
                        case SHUTDOWN:  
                            done = true;
                            break;
                        default:
                            /* Unknown message? */
                        }
                        
                        // Ack the message
                        LOG.fine("Acknowledging");
                        message = Message.NONE;
                        lock.notify();
                    }
                    else {
                        
                        // Timer is running
                        if (startTime != 0L) {
                            
                            long duration = System.currentTimeMillis() - startTime;
                            if (duration > START_DELAY) {
                                
                                update(duration);
                            }
                            
                            waitTime = 1000L;
                        }
                        else {
                            
                            waitTime = 0L;
                        }
                    }
                    
                    if (!done) {
                    
                        try {
                            
                            lock.wait(waitTime);
                        }
                        catch (InterruptedException e) {
                            
                            /* IGNORED */
                        }
                    }
                }
                
                state = VisualTimer.State.SHUTDOWN;
            }
            
            LOG.fine("Timer thread shutdown");
        }
        
        /*
         * Variables used to remember the state of the screen for safe drawing.
         */
        private boolean       isDisplaying = false; // Is the timer on the screen?
        private int           nback = 0;            // How far to backspace to erase the current time
        private StringBuilder sb = new StringBuilder();

        private void update (long duration) {
            
            String str = TimeUtils.millisToTimerString(duration);

            sb.setLength(0);

            /*
             * If we are currently displaying the timer, then back the cursor
             * up to the start of the current timestamp.
             */
            if (isDisplaying) {

                sb.append(Ansi.cursorLeft(nback));
            }
            else {
                
                /*
                 * The timer is now starting.
                 */
                sb.append("Elapsed time: ");
                isDisplaying = true;
            }
            
            // Remember how long the time string is.
            nback = str.length();
            
            // Now, write the time out.
            sb.append(str);
            
            // And send it to the screen.
            try {

                out.append(sb);
                out.flush();
            }
            catch (IOException e) {

                // IGNORED
            }
        }
                    
        private void clear () {
            
            if (isDisplaying) {
                
                // 14 == "Elapsed time: ".length()
                try {

                    out.append(Ansi.cursorLeft(14 + nback));
                    out.append(Ansi.eraseLine(Ansi.Erase.ALL));
                    out.flush();
                }
                catch (IOException e) {

                    // IGNORED
                }

                nback = 0;
                isDisplaying = false;
            }
        }
    }
}

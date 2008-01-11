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
package org.sqsh.jni;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.logging.Logger;

/**
 * The ShellManager provides key JNI functionality that sqsh
 * needs to do its job. Primarily, its job is to spawn external processes.
 * The {@link java.lang.ProcessBuilder} interface is fine and dandy and
 * all but unfortunately it is impossible to spawn a process that is
 * connected to the actual console TTY--every process that is spawned
 * must have its input and output connected to a JVM stream. 
 */
public class ShellManager {
    
    private static final Logger LOG = 
        Logger.getLogger(ShellManager.class.getName());
    
    private static ShellManager instance = null;
    
    private boolean haveJNI = false;
    
    /**
     * This array represents the command line that will be utilized
     * to execute all shell commands, any entry that contains a question
     * mark (?) will be replaced with the command that is being executed.
     * For example:
     * 
     * <pre>
     *     [0] = /bin/sh
     *     [1] = -c
     *     [2] = ?
     * </pre>
     * 
     * means that when a command, such as "ls -al" is executed, it will
     * be executed as
     * 
     * <pre>
     *    /bin/sh -c "ls -al"
     * </pre>
     */
    private String []shellCommand;
    
    
    /**
     * Creates a shell manager.
     */
    private ShellManager () {
        
        setDefaultShell();
        
        try {
            
            System.loadLibrary("jsqsh");
            init();
            haveJNI = true;
        }
        catch (Throwable e) {
            
            haveJNI = false;
            System.err.println("WARNING: Sqsh JNI layer not available ("
                + e.getMessage() + "). Run '\\help jni' for details");
        }
        
    }
    
    /**
     * Returns an instance of the ShellManager and initializes
     * the JNI layer. 
     * 
     * @return The ShellManager.
     */
    public static synchronized ShellManager getInstance() {
        
        if (instance == null) {
            
            instance = new ShellManager();
        }
        
        return instance;
    }
    
    /**
     * Called when the manager should be reset to utilize its default
     * shell based upon the operating system that the manager is
     * being run with.
     */
    private void setDefaultShell() {
        
        String os = System.getProperty("os.name");
        if (os.indexOf("Wind") >= 0) {
            
            shellCommand = new String[] { "cmd.exe", "/c", "?" };
        }
        else {
            
            shellCommand = new String[] { "/bin/sh", "-c", "?" };
        }
    }
    
    /**
     * Assigns the command that will be utilized when executing a command
     * shell. This is to be a comma delimited set of arguments with a 
     * question mark (?) acting as a place holder for the command to be
     * executed within the shell, for example:
     * 
     * <pre>
     *     bin/sh,-c,?
     * </pre>
     * 
     * means that when a command, such as "ls -al" is executed, it will
     * be executed as
     * 
     * <pre>
     *    /bin/sh -c "ls -al"
     * </pre>
     * 
     * @param cmd Comma delimited list of arguments.
     */
    public void setShellCommand(String cmd) {
        
        shellCommand = cmd.split(",");
    }
    
    /**
     * Returns the current shell command string.
     * @return The current shell command string.
     */
    public String getShellCommand() {
        
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < shellCommand.length; i++) {
            
            if (i > 0) {
                
                sb.append(',');
            }
            
            sb.append(shellCommand[i]);
        }
        
        return sb.toString();
    }
    
    /**
     * Returns whether or not the ShellManager is using its JNI interface
     * or its Java interface. The JNI interface allows command executed
     * by the shell to have access to the actual console TTY. When run
     * through the java interface commands that expect to control the
     * terminal (such as text editors and programs like "more") will
     * not function properly.
     * 
     * @return true if JNI is being utilized.
     */
    public boolean isJNI() {
        
        return haveJNI;
    }
    
    /**
     * Given a command to be executed in a sub-shell generates the
     * full shell command execution arguments that needs to be executed.
     * 
     * @param cmd The command string to be executed.
     * @return The full shell command arguments.
     */
    private String[] getShellExecution(String cmd) {
        
        String []args = new String[shellCommand.length];
        for (int i = 0; i < shellCommand.length; i++) {
            
            if ("?".equals(shellCommand[i])) {
                
                args[i] = cmd;
            }
            else {
                
                args[i] = shellCommand[i];
            }
        }
        
        return args;
    }
    
    /**
     * Spawns a command string as a sub-process that expects
     * to receive its input from the JVM. The input to the sub-process can
     * be retrieved via {@link Shell#getStdin). The stdout and stderr of the
     * sub-process is send to the calling terminal.
     * 
     * @param cmd The command to be executed in the sub-shell
     * @return A handle to the spawned process.
     */
    public Shell pipeShell (String cmd)
        throws ShellException {
        
        String []argv = getShellExecution(cmd);
        
        if (haveJNI) {
            
            return popen(argv);
        }
        
        ProcessBuilder pb = new ProcessBuilder(argv);
        pb.redirectErrorStream(true);
        
        Process p;
        
        try {
            
            p = pb.start();
        }
        catch (Exception e) {
            
            throw new ShellException(e.getMessage());
        }
        
        OutputGobbler oc = new OutputGobbler(p.getInputStream());
        oc.start();
        
        return new JavaShell(p);
    }
    
    /**
     * Spawns a shell to execute a command string as a sub-process that expects
     * to receive its input from the JVM. The input to the sub-process can
     * be retrieved via {@link Shell#getStdin). The stdout and stderr of the
     * sub-process is send to the calling terminal.
     * 
     * @param cmd The command to be executed in the sub-shell
     * @return A handle to the spawned process.
     */
    public Shell detachShell (String cmd)
        throws ShellException {
        
        String []argv = getShellExecution(cmd);
        
        if (haveJNI) {
            
            return exec(argv);
        }
        
        ProcessBuilder pb = new ProcessBuilder(argv);
        pb.redirectErrorStream(true);
        
        Process p;
        
        try {
            
            p = pb.start();
        }
        catch (Exception e) {
            
            throw new ShellException(e.getMessage());
        }
        
        OutputGobbler og = new OutputGobbler(p.getInputStream());
        og.start();
        
        InputGobbler ig = new InputGobbler(p.getOutputStream());
        ig.start();
        
        return new JavaShell(p);
    }
    
    /**
     * Spawns a shell to execute a command line as a sub-process that expects
     * to receive its input from the console. The stdout of the process
     * is available to the caller.
     * 
     * @param cmd The command to be executed in the sub-shell
     * @return A handle to the spawned process.
     */
    public Shell readShell (String cmd, boolean combineError)
        throws ShellException {
        
        String []argv = getShellExecution(cmd);
        
        ProcessBuilder pb = new ProcessBuilder(argv);
        Process p;
        pb.redirectErrorStream(combineError);
        
        try {
            
            p = pb.start();
        }
        catch (Exception e) {
            
            throw new ShellException(e.getMessage());
        }
        
        /*
         * If we aren't going to return the error stream as part of the
         * string we are returning, then send it to the session's
         * error stream.
         */
        if (combineError == false) {
            
            OutputGobbler oc = new OutputGobbler(p.getErrorStream());
            oc.start();
        } 
        
        return new JavaShell(p);
    }
    
    /**
     * Spawns an external process that is not attached to the current
     * process in any way shape or form.
     * 
     * @param cmd The command to execute.
     * @return A handle to the shell.
     * @throws ShellException Thrown if there is a problem.
     */
    public static native Shell exec (String []cmd)
        throws ShellException;
    
    /**
     * Spawns a sub-shell of a process that is has its own stdout and
     * stderr, but which receives its stdin from the JVM. 
     * @param cmd The command to execute.
     * @return A handle to the spawned process.
     * @throws ShellException Thrown if there is a problem.
     */
    public static native Shell popen (String []cmd)
        throws ShellException;
    
    private static native void init();
    
    /**
     * Waits for a process to exit.
     * 
     * @param pid The process id of the process to wait for.
     * @return The exit status of the process that is being waited for
     */
    protected static native int waitPid(long pid)
        throws InterruptedException;
    
    /**
     * Writes a byte to the output stream.
     * 
     * @param fd The file descriptor to write to.
     * @param b the byte to write.
     */
    protected static native void writeByte (long fd, int b)
        throws IOException;
    
    /**
     * Writes bytes to the output stream
     * 
     * @param bytes the bytes to write
     * @param offset the offset to start at
     * @param len the number of bytes to write.
     */
    protected static native void writeBytes (
            long fd, byte []bytes, int offset, int len)
        throws IOException;
    
    /**
     * Closes a file descriptor.
     * 
     * @param fd The file descriptor to close.
     * @throws IOException Thrown if there is a problem closing it.
     */
    protected static native void close(long fd)
        throws IOException;
    
    /**
     * Called by the JNI layer to log a debugging message.
     * 
     * @param msg The message to log.
     */
    protected static void debug (String msg) {
        
        LOG.finest(msg);
    }
    
    /**
     * Called by the JNI layer to log an informational message.
     * 
     * @param msg The message to log.
     */
    protected static void info (String msg) {
        
        LOG.info(msg);
    }
    
    /**
     * Called by the JNI layer to log an warning message.
     * 
     * @param msg The message to log.
     */
    protected static void warn (String msg) {
        
        LOG.warning(msg);
    }
    
    /**
     * Called by the JNI layer to log an error message.
     * 
     * @param msg The message to log.
     */
    protected static void error (String msg) {
        
        LOG.severe(msg);
    }
    
    /**
     * Called by the JNI layer to log a fatal message.
     * 
     * @param msg The message to log.
     */
    protected static void fatal (String msg) {
        
        LOG.severe(msg);
    }
    
    /**
     * Called by the JNI layer to log a fatal message.
     * 
     * @param msg The message to log.
     */
    protected static void abort (String msg) {
        
        LOG.severe(msg);
    }
    
    /**
     * This thread is used to consume the output of a process spawned for
     * pipe(). It reads from the spawned processes output stream and 
     * writes everything it reads to the sessions output stream.
     */
    private static class OutputGobbler
        extends Thread {
        
        private BufferedReader in;
        
        public OutputGobbler (InputStream in) {
            
            this.in = new BufferedReader(new InputStreamReader(in));
        }
        
        public void run() {
        
            try {
                    
                String line = in.readLine();
                while (line != null) {
                        
                    System.out.println(line);
                    line = in.readLine();
                }
            }
            catch (IOException e) {
                    
            }
        }
    }
    
    static class InputGobbler
        extends Thread {
    
        OutputStream os;

    	InputGobbler(OutputStream os) {
        
        	this.os = os;
    	}

    	public void run() {
        
        	try {
            
            	boolean done = false;
            	while (!done) {
                
                	int ch = System.in.read();
                	if (ch == -1) {
	                    
                    	done = true;
                	}
                
                	os.write(ch);
                	os.flush();
            	}
        	}
        	catch (IOException e) {
            
            	/* IGNORED */
        	}
    	}
    }
}

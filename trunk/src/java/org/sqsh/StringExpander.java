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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogChute;
import org.sqsh.jni.Shell;
import org.sqsh.jni.ShellException;

/**
 * The string expander is responsible for doing variable (well, technically
 * velocity) expansion for sqsh. The expansion can take place in one of two
 * modes: following quoting rules or ignoring quoting rules.
 */
public class StringExpander
    implements LogChute {
    
    private static final Logger LOG = 
        Logger.getLogger(StringExpander.class.getName()); 
    
    private Session session;
    
    private VelocityEngine velocity;
    private VelocityContext context;
    
    private String IFS = null;
    
    /**
     * Creates a string expander that will expand variables in the context
     * of the provided session.
     * 
     * @param session The session that variables will be snagged from.
     */
    public StringExpander (Session session) {
        
        setIFS("\\s");
        
        this.session = session;
        
        try {
            
            velocity = new VelocityEngine();
            velocity.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM, this);
            velocity.init();
        }
        catch (Exception e) {
            
            LOG.log(Level.SEVERE, "Unable to initialize velocity", e);
        }
        
        context = new VelocityContext(System.getenv());
        context = new VelocityContext(session.getVariableManager(), context);
    }
    
    /**
     * Sets the internal field separator. This value is used during 
     * back-tick processing to separate the results of the command
     * that is executed.
     * 
     * @param ifs The new IFS value. Certain special strings are
     * recognized: \t, \n, \r, \s (all white spaces).
     */
    public void setIFS(String ifs) {
        
        StringBuilder buf = new StringBuilder();
        
        int idx = 0;
        while (idx < ifs.length()) {
            
            char ch = ifs.charAt(idx);
            if (ch == '\\') {
                
                ++idx;
                if (idx < ifs.length()) {
                    
                    ch = ifs.charAt(idx);
                    ++idx;
                    
                    switch (ch) {
                        
                        case 'n':
                            buf.append("\n");
                            break;
                        case 'r':
                            buf.append("\r");
                            break;
                        case 't':
                            buf.append("\t");
                            break;
                        case 's':
                            buf.append(" \t\n\r");
                            break;
                        default:
                            buf.append(ch);
                    }
                }
                else {
                    
                    buf.append("\\");
                    ++idx;
                }
            }
            else {
                
                buf.append(ch);
                ++idx;
            }
        }
        
        IFS = buf.toString();
    }
    
    /**
     * Returns the current internal field separator.
     * 
     * @return The current field separator.
     */
    public String getIFS() {
        
        return IFS;
    }
    
    /**
     * Expands a string of any variables that it may contain.
     * 
     * @param str The string to be expanded.
     *   
     * @return the expanded version of the string.
     */
    public String expand(String str) {
        
        Writer writer = new StringWriter(str.length());
        
        try {
            
            velocity.evaluate(context, writer, "<string>", str);
        }
        catch (Exception e) {
            
            session.err.println("Error during variable expansion: "
                + e.getMessage());
        }
        
        return writer.toString();
    }
    
    /**
     * Expands a string of any variables that it may contain.
     * 
     * @param variables A set of variables to use during the expansion. These
     *    variables will be used first before referring to other variables
     *    defined within the session's scope.
     * @param str The string to be expanded.
     *   
     * @return the expanded version of the string.
     */
    public String expand(Map variables, String str) {
        
        VelocityContext ctx = new VelocityContext(variables, context); 
        Writer writer = new StringWriter(str.length());
        
        try {
            
            velocity.evaluate(ctx, writer, "<string>", str);
        }
        catch (Exception e) {
            
            session.err.println("Error during variable expansion: "
                + e.getMessage());
        }
        
        return writer.toString();
    }
    
    /**
     * Expands a string of its variables but follows basic shell-like rules
     * surrounding quotes that may be contained within the string. That is,
     * variables contained within single quotes will not be expanded and
     * back-ticks will be expanded as the results of executing the contents
     * of the string contained between them.
     * 
     * @param str The string to be expanded.
     * @return The expanded string.
     */
    public String expandWithQuotes(String str)
        throws CommandLineSyntaxException {
        
        StringBuilder expanded = new StringBuilder();
        doUnquoted(expanded, str, 0);
        
        return expanded.toString();
    }
    
    /**
     * Used during parsing to process text that is not contained within
     * any kind of quotes.
     * 
     * @param expanded The current view of the expanded string.
     * @param str The stirng being expanded.
     * @param idx Current location into the string being expanded (this
     *   should be located on the single quote).
     * @return The final index into the string.
     */
    private int doUnquoted (StringBuilder expanded, String str, int idx)
        throws CommandLineSyntaxException {
        
        StringBuilder buf = new StringBuilder();
        
        while (idx < str.length()) {
            
            char ch = str.charAt(idx);
            
            switch (ch) {
                
                case '\'':
                    if (buf.length() > 0) {
            
                        expanded.append(expand(buf.toString()));
                        buf.setLength(0);
                    }
                    
                    idx = doSingleQuote(expanded, str, idx);
                    break;
                    
                case '\\':
                    if (buf.length() > 0) {
            
                        expanded.append(expand(buf.toString()));
                        buf.setLength(0);
                    }
                    
                    idx = doEscape(buf, str, idx);
                    break;
                
                case '"':
                    if (buf.length() > 0) {
            
                        expanded.append(expand(buf.toString()));
                        buf.setLength(0);
                    }
                    
                    idx = doDoubleQuote(expanded, str, idx);
                    break;
                
                case '`':
                    if (buf.length() > 0) {
            
                        expanded.append(expand(buf.toString()));
                        buf.setLength(0);
                    }
                    
                    idx = doBackTick(expanded, str, idx);
                    break;
                    
                default:
                    buf.append(ch);
                    ++idx;
            }
        }
        
        if (buf.length() > 0) {
            
            expanded.append(expand(buf.toString()));
        }
        
        return idx;
    }
    
    /**
     * Used during parsing to process an escape character.
     * 
     * @param expanded The current view of the expanded string.
     * @param str The stirng being expanded.
     * @param idx Current location into the string being expanded (this
     *   should be located on the escape character).
     * @return The final index into the string.
     */
    private int doEscape (StringBuilder expanded, String str, int idx)
        throws CommandLineSyntaxException {
        
        expanded.append('\\');
        ++idx;
        
        if (idx < str.length()) {
            
            expanded.append(str.charAt(idx));
            ++idx;
        }
        
        return idx;
    }
    
    /**
     * Used while parsing a string that contains a single quote to process
     * the contents o the single-quoted string.
     * 
     * @param expanded The current view of the expanded string.
     * @param str The stirng being expanded.
     * @param idx Current location into the string being expanded (this
     *   should be located on the single quote).
     * @return The final index into the string.
     */
    private int doSingleQuote (StringBuilder expanded, String str, int idx)
        throws CommandLineSyntaxException {
        
        int startIdx = idx;
        expanded.append('\'');
                
        /*
         * Now, boogie along until we hit a close quote.
         */
        ++idx;
        while (idx < str.length()) {
                    
            char ch = str.charAt(idx);
            if (ch == '\'') {
                        
                break;
            }
                    
            expanded.append(ch);
            ++idx;
        }
        
        if (idx == str.length()) {
            
            throw new CommandLineSyntaxException("Missing closing single quote",
                idx, str);
        }
                
        expanded.append('\'');
        ++idx;
        
        return idx;
    }
    
    /**
     * Used while parsing a string that contains a double quote to process
     * the contents of the double-quoted string.
     * 
     * @param expanded The current view of the expanded string.
     * @param str The stirng being expanded.
     * @param idx Current location into the string being expanded (this
     *   should be located on the double quote).
     * @return The final index into the string.
     */
    private int doDoubleQuote (StringBuilder expanded, String str, int idx)
        throws CommandLineSyntaxException {
        
        int startIdx = idx;
        StringBuilder contents = new StringBuilder();
        
        expanded.append('"');
        
        ++idx;
        while (idx < str.length()) {
            
            char ch = str.charAt(idx);
            
            if (ch == '\\') {
                
                idx = doEscape(contents, str, idx);
            }
            else if (ch == '`') {
                
                idx = doBackTick(contents, str, idx);
            }
            else if (ch == '"') {
                
                break;
            }
            else {
                
                contents.append(ch);
                ++idx;
            }
        }
        
        if (idx == str.length()) {
            
            throw new CommandLineSyntaxException("Missing closing double-quote",
                startIdx, str);
        }
        
        expanded.append(expand(contents.toString()));
        expanded.append('"');
        ++idx;
        
        return idx;
    }
    
    /**
     * Called when a back-tick is encountered during string expansion. This
     *
     * @param expanded The expanded string thus-dar.
     * @param str The string being expanded.
     * @param idx The index of the back-tick.
     * @return The final index of the string being parsed.
     */
    private int doBackTick (StringBuilder expanded, String str, int idx)
        throws CommandLineSyntaxException {
        
        int startIdx = idx;
        boolean done = false;
        
        /*
         * Skip the back-tick.
         */
        ++idx;
        
        StringBuilder command = new StringBuilder();
        StringBuilder buf = new StringBuilder();
        
        while (!done && idx < str.length()) {
            
            char ch = str.charAt(idx);
            switch (ch) {
                
                case '\'':
                    if (buf.length() > 0) {
                        
                        command.append(session.expand(buf.toString()));
                        buf.setLength(0);
                    }
                    
                    idx = doSingleQuote(command, str, idx);
                    break;
                    
                case '\\':
                    if (buf.length() > 0) {
                        
                        command.append(session.expand(buf.toString()));
                        buf.setLength(0);
                    }
                    
                    idx = doEscape(buf, str, idx);
                    break;
                
                case '"':
                    if (buf.length() > 0) {
                        
                        command.append(session.expand(buf.toString()));
                        buf.setLength(0);
                    }
                    
                    idx = doDoubleQuote(buf, str, idx);
                    break;
                
                case '`':
                    done = true;
                    break;
                    
                default:
                    buf.append(ch);
                    ++idx;
            }
        }
        
        if (idx == str.length()) {
            
            throw new CommandLineSyntaxException(
                "Missing closing back-tick (`)", startIdx, str);
        }
        
       ++idx;
        
        if (buf.length() > 0) {
            
            command.append(expand(buf.toString()));
        }
        
        try {
            
            String output = getShellOutput(session, command.toString());
            if (output != null) {
            
                expanded.append(output);
            }
        }
        catch (ShellException e) {
            
            throw new CommandLineSyntaxException(
                "Failed to execute: " + command.toString() + ": "
                + e.getMessage(), idx, str);
        }
        
        return idx;
    }
    
    /**
     * Used during back-tick processing to suck up the results of an
     * external command and return it as a string. The string will be
     * broken up according to the value of $IFS.
     * 
     * @param session The session.
     * @param command The command to be executed.
     * @return The output of the command.
     * @throws ShellException
     */
    private String getShellOutput(Session session, String command)
        throws ShellException {
        
        StringBuilder sb = new StringBuilder();
        
        Shell shell = session.getShellManager().readShell(command,
            session.err == session.out);
        
        BufferedReader in =
             new BufferedReader(new InputStreamReader(shell.getStdout()));
        
        try {
            
            int ch = in.read();
            while (ch != -1) {
    
                /*
                 * Skip leading IFS characters.
                 */
                while (ch != -1 && IFS.indexOf((char) ch) >= 0) {
    
                    ch = in.read();
                }
    
                if (sb.length() > 0) {
    
                    sb.append(' ');
                }
    
                /*
                 * Suck in non-IFS characters.
                 */
                while (ch != -1 && IFS.indexOf(ch) < 0) {
    
                    sb.append((char) ch);
                }
    
                ch = in.read();
            }
        }
        catch (IOException e) {
            
            /* IGNORED - Probably a dead pipe */
        }
        finally {

            try {

                in.close();
            }
            catch (IOException e) {

                /* IGNORED */
            }
        }
        
        return sb.toString();
    }
    
    
	/**
  	 * This is required by LogChute.
 	 */
    public void init (RuntimeServices service)
        throws Exception {

        /* Do nothing. */
    }

    /**
     * Used by LogChute to determine if a logging level is current
     * enabled.
     * 
     * @param level The level we are testing.
     */
    public boolean isLevelEnabled (int level) {
        
        return (level >= LogChute.WARN_ID);
    }

    /**
     * Used by LogChute to log a stack trace.
     */
    public void log (int level, String message, Throwable cause) {
        
        log(level, message);
        cause.printStackTrace(session.err);
    }

    /**
     * Used to log a message.
     */
    public void log (int level, String message) {

        if (level >= LogChute.WARN_ID) {
            
            session.err.println(logPrefix(level)
            	+ ": Failed while performing variable expansion processing: "
            	+ message);
        }
    }
    
    private String logPrefix(int level) {
        
        switch (level) {
            
            case LogChute.TRACE_ID: return "TRACE";
            case LogChute.DEBUG_ID: return "DEBUG";
            case LogChute.INFO_ID: return "INOF";
            case LogChute.WARN_ID: return "WARN";
            case LogChute.ERROR_ID: return "ERROR";
            default:
                return "UNKNOWN";
        }
    }
}

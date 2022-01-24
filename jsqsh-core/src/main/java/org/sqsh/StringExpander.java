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

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sqsh.jni.Shell;
import org.sqsh.jni.ShellException;

import java.io.*;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * The string expander is responsible for doing variable (well, technically
 * velocity) expansion for sqsh. The expansion can take place in one of two
 * modes: following quoting rules or ignoring quoting rules.
 */
public class StringExpander {

    private static final Logger LOG = 
        Logger.getLogger(StringExpander.class.getName()); 
    
    private static StringExpander envExpander = null;
    
    private VelocityEngine velocity;
    private VelocityContext context;
    
    /**
     * This is the actual context used during expansion. All public
     * methods will temporarily create this as necessary so that
     * all private methods may refer to it.
     */
    private VelocityContext expandContext = null;
    
    /**
     * This is temporary reference to the session for which the expansion
     * is taking place and is populated by each public method before
     * doing the actual expansion logic.
     */
    private Session session = null;
    
    private String IFS = null;
    
    /**
     * Creates a string expander.
     */
    public StringExpander () {
        
        setIFS("\\s");
        
        try {
            
            velocity = new VelocityEngine();
            velocity.init();
        }
        catch (Exception e) {
            
            LOG.log(Level.SEVERE, "Unable to initialize velocity", e);
        }

        final Map<String, Object> env = System.getenv().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        context = new VelocityContext(env);
    }
    
    /**
     * @return A string expander only capable of expanding environment variables.
     */
    public static StringExpander getEnvironmentExpander() {
        
        if (envExpander == null) {
            
            envExpander = new StringExpander();
        }
        
        return envExpander;
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
     * Expands a string. This form of expand will only expand environment
     * variables.
     * @param str The string to expand
     * @return The expanded string
     */
    public String expand (String str) {
        
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
     * @param str The string to be expanded.
     *   
     * @return the expanded version of the string.
     */
    public String expand(Session session, String str) {
        
        this.session = session;
        this.expandContext = 
            new VelocityContext(session.getVariableManager(), context);
        
        return doExpand(str);
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
    public String expand(Session session, Map variables, String str) {
        
        this.session = session;
        this.expandContext = 
            new VelocityContext(variables, 
                new VelocityContext(session.getVariableManager(), context));
        
        return doExpand(str);
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
    public String expandWithQuotes(Session session, String str)
        throws CommandLineSyntaxException {
        
        this.session = session;
        this.expandContext = 
            new VelocityContext(session.getVariableManager(), context);
        
        StringBuilder expanded = new StringBuilder();
        doUnquoted(expanded, str, 0);
        
        return expanded.toString();
    }
    
    /**
     * Performs the actual variable expansion.
     * 
     * @param str The string to expand
     * @return The expanded string.
     */
    public String doExpand(String str) {
        
        Writer writer = new StringWriter(str.length());
        try {
            
            velocity.evaluate(expandContext, writer, "<string>", str);
        }
        catch (Exception e) {
            
            session.err.println("Error during variable expansion: "
                + e.getMessage());
        }
        
        return writer.toString();
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
            
                        expanded.append(doExpand(buf.toString()));
                        buf.setLength(0);
                    }
                    
                    idx = doSingleQuote(expanded, str, idx);
                    break;
                    
                case '\\':
                    if (buf.length() > 0) {
            
                        expanded.append(doExpand(buf.toString()));
                        buf.setLength(0);
                    }
                    
                    idx = doEscape(buf, str, idx);
                    break;
                
                case '"':
                    if (buf.length() > 0) {
            
                        expanded.append(doExpand(buf.toString()));
                        buf.setLength(0);
                    }
                    
                    idx = doDoubleQuote(expanded, str, idx);
                    break;
                
                case '`':
                    if (buf.length() > 0) {
            
                        expanded.append(doExpand(buf.toString()));
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
            
            expanded.append(doExpand(buf.toString()));
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
        
        expanded.append(doExpand(contents.toString()));
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
            
            command.append(doExpand(buf.toString()));
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
                    ch = in.read();
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
}

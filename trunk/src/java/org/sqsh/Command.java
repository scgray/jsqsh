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

import java.awt.BorderLayout;
import java.awt.Font;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.sqsh.variables.DimensionVariable;
import org.sqsh.variables.FontVariable;


/**
 * All jsqsh commands must extend this class. A Command follows a relatively
 * simple lifecycle when it is called.
 * <ol>
 *   <li> The Commands {@link #getOptions()} is called to retrieve the
 *        set of command line options that are supported by the command.
 *        Commands that have no command line options need not implement
 *        this method as a default is provided for you.
 *   <li> The command line options are processed and an error message displayed
 *        to the user if they provide an invalid option.
 *   <li> A command may also implement the marker interface 
 *        {@link DatabaseCommand} to indicate that it needs a database 
 *        connection to function. If someone tries to use the command
 *        without being connected, they will receive an error.
 *   <li> If all of the above pass muster, then the commands
 *        {@link #execute(Session, SqshOptions)} is called with the 
 *        command line options the user has passed.
 * That's pretty much it!
 */
public abstract class Command
    extends HelpTopic
    implements Comparable {
    
    private static final Logger LOG = 
        Logger.getLogger(Command.class.getName());
    
    private String name;
    
    /**
     * Handle back to the manager that owns this command.
     */
    private CommandManager manager = null;
    
    /**
     * This constructor should only be called by the CommandManager.
     */
    public Command () {
        
        name = null;
    }
    
    /**
     * Base constructor for a command.
     * @param name The name of the command.
     * @param description A brief description of what the command does.
     * @param helpText The full help text for the command.
     */
    public Command (String name, String description, String helpText) {
        
        super(name, description, helpText);
        this.name = name;
    }
    
    /**
     * Used by the CommandManager to record itself as being the "owner"
     * of this command.
     * 
     * @param manager The owner of the command.
     */
    protected void setManager(CommandManager manager) {
        
        this.manager = manager;
    }
    
    /**
     * This method returns the set of options that are accepted for this
     * command.  Commands that accept no options of their own need not
     * override this method, however commands that want to provide 
     * command line options should extend the {@link SqshOptions} class
     * to define them.   This method must return a freshly allocated 
     * SqshOptions object with all options set to their default values.
     * 
     * @return A newly allocated options object.
     */
    public SqshOptions getOptions() {
        
        return new SqshOptions();
    }
    
    /**
     * This method must be implemented by the command to perform its job.
     * 
     * @param session The session context in which the command is being
     *   executed.
     * @param options The options that the command received. This object
     *   will have been allocated by calling the commands {@link #getOptions()}
     *   method.
     *   
     * @return The "exit code" for the command. By convention, 0 indicates
     *   a successful execution.
     *   
     * @throws Exception This allows a command to be lazy about catching
     *   exceptions that it may cause. Any exception thrown is caught
     *   by Jsqsh and printed to stderr.
     */
    public abstract int execute (Session session, SqshOptions options)
        throws Exception;
    
    /**
     * This method must be implemented by the command and defines its
     * behavior.
     * @param session The session context in which the command is
     *   being executed.
     * @param argv The arguments to the command, this follows the same
     *   behavior as the main() method.
     * @return The "exit code" for the command. By convention, 0 indicates
     *   a successful execution.
     * @throws Exception Thrown if the command fails.
     */
    public final int execute(Session session, String argv[])
        throws Exception {
        
        SqshOptions options = getOptions();
        CmdLineParser parser = new CmdLineParser(options);
        
        try {
            
            parser.parseArgument(argv);
        }
        catch (CmdLineException e) {
            
            session.err.println(e.getMessage());
            parser.printUsage(session.err);
            
            return 1;
        }
        
        /*
         * If this command has declared that it needs to work against
         * the database, then ensure that we have a connection.
         */
        if (this instanceof DatabaseCommand
                && session.getConnection() == null) {
            
            session.err.println("You are not currently connect to a server. "
                + "Use the \\connect command to create a new connection.");
            return 1;
        }
        
        PrintStream origOut = session.out;
        PrintStream origErr = session.err;
        
        /*
         * If the graphical option was passed, then we will redirect output
         * and error to a window.
         */
        if (options.isGraphical) {
            
            JTextAreaCaptureStream textStream = 
                new JTextAreaCaptureStream(session, 
                    new ByteArrayOutputStream());
            session.setOut(textStream);
            session.setErr(textStream);
        }
        
        int rc = 0;
        try {
            
            rc = execute(session, options);
        }
        finally {
            
            /*
             * If we were sending to a graphical output, then fix it
             * here.
             */
            if (options.isGraphical) {
                
                session.setErr(origErr);
                session.setOut(origOut);
            }
        }
        
        return rc;
    }
    
    /**
     * Helper method to print the usage.
     * 
     * @param session
     * @param options
     */
    public void printUsage(Session session, Object options) {
        
        CmdLineParser parser = new CmdLineParser(options);
        parser.printUsage(session.err);
    }
    
    /**
     * Set the name of the command.
     * 
     * @param name The name of the command.
     */
    public void setName(String name) {
        
        setTopic(name);
        this.name = name;
    }
    
    /**
     * Returns the name of the variable.
     * 
     * @return The name of the variable.
     */
    public String getName() {
        
        return name;
    }
    
    /**
     * Compares the names of two commands. This method is provided primarily
     * to allow for easy sorting of commands on display.
     * 
     * @param o The object to compare to.
     * @return The results of the comparison.
     */
    public int compareTo(Object o) {
        
        if (o instanceof Command) {
            
            return name.compareTo(((Command) o).getName());
        }
        
        return -1;
    }
    
    /**
     * Compares two commands for equality. Commands are considered
     * equal if their names match.
     * 
     * @param o The object to test equality.
     * @return true if o is a Command that has the same name as this.
     */
    public boolean equals(Object o) {
        
        if (o instanceof Command) {
            
            return ((Command) o).getName().equals(name);
        }
        
        return false;
    }
    
    /**
     * Returns a hash value for the command. The hash code is nothing
     * more than the hash code for the command name itself.
     * 
     * @return The hash code of the command's name.
     */
    public int hashCode() {
        
        return name.hashCode();
    }
    
    /**
     * Class used to capture writes to output and send them to a text
     * area panel.
     */
    private static class JTextAreaCaptureStream
        extends PrintStream {
        
        private JTextArea textArea;
        private Session session;
        private ByteArrayOutputStream buffer;
        
        public JTextAreaCaptureStream(Session session,
                ByteArrayOutputStream buffer) {
            
            super(buffer, true);
            this.buffer = buffer;
            this.session = session;
            
            int width = 600;
            int height = 400;
            
            DimensionVariable v =
                (DimensionVariable) session.getVariableManager()
                .getVariable("window_size");
            
            if (v != null) {
                
                width = v.getWidth();
                height = v.getHeight();
            }
            
            JFrame frame = new JFrame();
            
            // Set the frame characteristics
            frame.setTitle("Query Results");
            frame.setSize(width, height);
            frame.setLocationByPlatform(true);

            // Create a panel to hold all other components
            JPanel topPanel = new JPanel();
            topPanel.setLayout(new BorderLayout());
            frame.getContentPane().add(topPanel);
            
            textArea = new JTextArea();
            textArea.setLineWrap(false);
            
            FontVariable fontVar =
                (FontVariable) session.getVariableManager()
                    .getVariable("font");
            
            if (fontVar != null) {
                
                textArea.setFont(new Font(fontVar.getFontName(), Font.PLAIN,
                    fontVar.getFontSize() ));
            }
            else {
            
                textArea.setFont(new Font( "Monospaced", Font.PLAIN, 10 ));
            }
            
            // Add the table to a scrolling pane
            JScrollPane scrollPane = new JScrollPane(textArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            topPanel.add( scrollPane, BorderLayout.CENTER );
            
            frame.setVisible(true);
        }

        @Override
        public void flush () {

            super.flush();
            
            String text = buffer.toString();
            textArea.append(text);
            buffer.reset();
        }
    }
}

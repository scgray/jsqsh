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

import java.awt.BorderLayout;
import java.awt.Font;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import org.sqsh.variables.DimensionVariable;
import org.sqsh.variables.FontVariable;
import org.sqsh.options.OptionException;
import org.sqsh.options.OptionProcessor;

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
    extends HelpTopic {
    
    private String name;
    
    /**
     * Handle back to the manager that owns this command.
     */
    private CommandManager manager = null;
    
    /**
     * If true, then this command is "hidden" -- it is considered internal
     * and should not be displayed in the general command list.
     */
    private boolean isHidden = false;
    
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
     * @param helpLocation The location of the help text
     */
    public Command (String name, String description, String helpLocation) {
        
        super(name, description, helpLocation);
        this.name = name;
    }
    
    /**
     * @return whether or not this command should appear in documentation
     */
    public boolean isHidden() {
    
        return isHidden;
    }

    /**
     * Sets whether or not the command will be hidden from documentation
     * @param isHidden if true, the command will not appear in documentation
     */
    public void setHidden(boolean isHidden) {
    
        this.isHidden = isHidden;
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
     * @return The managing command manager.
     */
    protected CommandManager getManager() {
        
        return manager;
    }
    
    /**
     * This method is called when parsing the command line for the command and
     * is an indicator of whether or not double quoted strings should retain 
     * the surrounding double quotes when passed to the argument.  The default
     * value is false, indicating that the parsing should happen like a normal
     * UNIX shell and the contents of the double quoted string should be passed
     * without surrounding quotes.  Commands wishing to retain the double quotes
     * should override this method.
     * 
     * @return false
     */
    public boolean keepDoubleQuotes() {
        
        return false;
    }
    
    /**
     * This method must return the set of options that are accepted for this
     * command.  
     * 
     * @return A newly allocated options object.
     */
    public abstract SqshOptions getOptions();
    
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
        OptionProcessor cmdParser = new OptionProcessor(options);
        
        try {
            
            cmdParser.parseOptions(argv);
        }
        catch (OptionException e) {
            
            session.err.println(e.getMessage());
            session.err.println(cmdParser.getUsage());
            
            return 1;
        }
        
        /*
         * Display help if requested.
         */
        if (options.doHelp) {
            
            session.out.println(cmdParser.getUsage());
            return 0;
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
        
        /*
         * If the graphical option was passed, then we will redirect output
         * and error to a window.
         */
        if (options.isGraphical) {
            
            JTextAreaCaptureStream textStream = 
                new JTextAreaCaptureStream(session, 
                    new ByteArrayOutputStream());
            session.setOut(textStream, true);
            session.setErr(textStream, true);
        }
        
        return execute(session, options);
    }
    
    /**
     * Display the usage of the command.
     * 
     * @param out The stream to display to.
     */
    public void printUsage(PrintStream out) {
        
        OptionProcessor cmdParser = new OptionProcessor(getOptions());
        out.println(cmdParser.getUsage());
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
        @SuppressWarnings("unused")
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

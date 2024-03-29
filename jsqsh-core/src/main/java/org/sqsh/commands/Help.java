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
package org.sqsh.commands;

import org.sqsh.ColumnDescription;
import org.sqsh.ColumnDescription.Alignment;
import org.sqsh.ColumnDescription.OverflowBehavior;
import org.sqsh.Command;
import org.sqsh.HelpTopic;
import org.sqsh.MarkdownFormatter;
import org.sqsh.PagedCommand;
import org.sqsh.Renderer;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.Variable;
import org.sqsh.options.Argv;
import org.sqsh.options.OptionProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.sqsh.options.ArgumentRequired.REQUIRED;

/**
 * Implements the 'help' command.
 */
public class Help extends Command implements PagedCommand {

    /**
     * Used to contain the command line options that were passed in by the caller.
     */
    private static class Options extends SqshOptions {

        @OptionProperty(option = 'f', longOption = "format", arg = REQUIRED, argName = "style",
                description = "The display format for the help text (markdown, raw, or pretty)")
        public String format = null;

        @Argv(program = "\\help", min = 0, max = 1, usage = "[topics|vars|commands|name]")
        public List<String> arguments = new ArrayList<>();
    }

    /**
     * Return our overridden options.
     */
    @Override
    public SqshOptions getOptions() {
        return new Options();
    }

    @Override
    public int execute(Session session, SqshOptions opts) throws Exception {
        Options options = (Options) opts;

        int nArgs = options.arguments.size();
        if (nArgs > 1) {
            session.err.println("use: help [command]");
            return 1;
        }

        if (options.format != null) {
            if (!(options.format.equals("markdown") || options.format.equals("raw") || options.format.equals("pretty"))) {
                System.err.println("Invalid display format: " + options.format + ".  Valid formats are: \"markdown\", \"raw\", and \"pretty\"");
                return 1;
            }
        }

        if (nArgs == 0) {
            displayHelpCategories(session);
            return 0;
        }

        String word = options.arguments.get(0);
        int rc = 0;
        switch (word) {
            case "topics":
                displayTopics(session);
                break;
            case "vars":
                displayVariables(session);
                break;
            case "commands":
                displayCommands(session);
                break;
            default:
                rc = displayHelpText(session, word, options);
                break;
        }
        return rc;
    }

    public static int displayHelpText(Session session, String word) {
        return displayHelpText(session, word, new Options());
    }

    /**
     * Looks up a specific help topic and displays it.
     *
     * @param session The session to use for display.
     * @param word The topic to look for.
     * @return 0 if the topic was found and displayed, 1 otherwise.
     */
    public static int displayHelpText(Session session, String word, Options options) {
        int rc = 0;
        HelpTopic topic = session.getCommandManager().getCommand(word);

        // If we fail to find the command the user asked for it *could* be because the shell interpreted the backslash
        // in front of the command name, so will append one and try again.
        if (topic == null) {
            topic = session.getCommandManager().getCommand('\\' + word);
        }

        // If we fail that, try to see if it is a variable that we are talking about.
        if (topic == null) {
            topic = (HelpTopic) session.getVariableManager().getVariable(word);
            if (topic != null && topic.getDescription() == null) {
                topic = null;
            }
        }

        // If we're still null, then we'll check to see if it is a general topic that is being requested.
        if (topic == null) {
            topic = session.getHelpManager().getTopic(word);
        }

        // At this point we're S.O.L.
        if (topic == null) {
            session.err.println("No such command or topic '" + word + "'");
            rc = 1;
        } else {
            if (options.format != null && "markdown".equals(options.format)) {
                session.out.println(topic.getHelp());
            } else {
                int width = session.getScreenWidth();
                if (width > 70) {
                    width -= 8;
                } else if (width < 40) {
                    width = 40;
                }

                MarkdownFormatter formatter = new MarkdownFormatter(width, session.out);
                if (options.format != null && "raw".equals(options.format)) {
                    formatter.setDecorationsEnabled(false);
                }
                formatter.format(topic.getHelp());
            }
        }
        return rc;
    }

    /**
     * Displays available help categories.
     *
     * @param session The session to be used for displaying.
     */
    private void displayHelpCategories(Session session) {
        session.out.println("Available help categories. Use " + "\"\\help <category>\" to display topics within that category.");
        session.out.println("For online help, getting started guide, and tutorials, visit " + "https://github.com/scgray/jsqsh/wiki");

        ColumnDescription[] columns = new ColumnDescription[2];
        columns[0] = new ColumnDescription("Category", -1, Alignment.LEFT, OverflowBehavior.WRAP, false);
        columns[1] = new ColumnDescription("Description", -1);

        Renderer renderer = session.getRendererManager().getCommandRenderer(session);

        renderer.header(columns);

        renderer.row(new String[]{"commands", "Help on all avaiable commands"});
        renderer.row(new String[]{"vars", "Help on all avaiable configuration variables"});
        renderer.row(new String[]{"topics", "General help topics for jsqsh"});

        renderer.flush();
    }

    /**
     * Displays available commands.
     *
     * @param session The session to be used for displaying.
     */
    private void displayCommands(Session session) {
        session.out.println("Available commands. " + "Use \"\\help <command>\"" + " to display detailed help for a given command");

        Renderer renderer = session.getRendererManager().getCommandRenderer(session);
        Command[] commands = session.getCommandManager().getCommands();
        Arrays.sort(commands);

        ColumnDescription[] columns = new ColumnDescription[2];
        columns[0] = new ColumnDescription("Command", -1, Alignment.LEFT, OverflowBehavior.WRAP, false);
        columns[1] = new ColumnDescription("Description", -1);

        renderer.header(columns);

        for (int i = 0; i < commands.length; i++) {
            if (!commands[i].isHidden()) {
                String[] row = new String[2];
                row[0] = commands[i].getName();
                row[1] = commands[i].getDescription();
                renderer.row(row);
            }
        }
        renderer.flush();
    }

    /**
     * Displays available variables.
     *
     * @param session The session to be used for displaying.
     */
    private void displayVariables(Session session) {
        session.out.println("Available configuration variables. " + "Use \"\\help <variable>\" to display detailed " + "help for a given variable");

        Renderer renderer = session.getRendererManager().getCommandRenderer(session);
        Variable[] variables = session.getVariableManager().getVariables(true);
        Arrays.sort(variables);

        ColumnDescription[] columns = new ColumnDescription[2];
        columns[0] = new ColumnDescription("Variable", -1, Alignment.LEFT, OverflowBehavior.WRAP, false);
        columns[1] = new ColumnDescription("Description", -1);

        renderer.header(columns);

        for (int i = 0; i < variables.length; i++) {
            if (variables[i].getDescription() != null) {
                String[] row = new String[2];
                row[0] = variables[i].getName();
                row[1] = variables[i].getDescription();
                renderer.row(row);
            }
        }
        renderer.flush();
    }

    /**
     * Displays available topics.
     *
     * @param session The session to be used for displaying.
     */
    private void displayTopics(Session session) {
        session.out.println("Available help topics. " + "Use \"\\help <topic>\" to display detailed " + "help for a given variable");
        Renderer renderer = session.getRendererManager().getCommandRenderer(session);

        ColumnDescription[] columns = new ColumnDescription[2];
        columns[0] = new ColumnDescription("Topic", -1);
        columns[1] = new ColumnDescription("Description", -1);

        renderer.header(columns);

        HelpTopic[] topics = session.getHelpManager().getTopics();
        Arrays.sort(topics);

        for (int i = 0; i < topics.length; i++) {
            String[] row = new String[2];
            row[0] = topics[i].getTopic();
            row[1] = topics[i].getDescription();
            renderer.row(row);
        }
        renderer.flush();
    }
}

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
/**
 * Internal command to generate jsqsh's wiki docs
 */

import org.sqsh.Alias;
import org.sqsh.Command;
import org.sqsh.HelpTopic;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.Variable;
import org.sqsh.options.Argv;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Wiki extends Command {

    private static class Options extends SqshOptions {

        @Argv(program = "\\wiki", min = 0, max = 1, usage = "[dest_dir]")
        public List<String> arguments = new ArrayList<String>();
    }

    @Override
    public SqshOptions getOptions() {
        return new Options();
    }

    @Override
    public int execute(Session session, SqshOptions opts) throws Exception {
        Options options = (Options) opts;
        String dir = ".";
        if (options.arguments.size() > 0) {
            dir = options.arguments.get(0);
        }
        session.out.println("Creating commands documentation:");
        doCommands(session, new File(dir));
        session.out.println("Creating topics documentation:");
        doTopics(session, new File(dir));
        session.out.println("Creating variable documentation:");
        doVariables(session, new File(dir));
        return 0;
    }

    private void doCommands(Session session, File dir) throws IOException {
        Command[] commands = session.getCommandManager().getCommands();
        Arrays.sort(commands);

        Alias[] aliases = session.getAliasManager().getAliases();
        PrintStream masterPage = open(dir, "Command-Reference.md");
        masterPage.println("Most jsqsh commands are preceded with a backslash (`\\`), however\n"
                + "a number of commands come with built-in aliases for convenience \n"
                + "(see the **Alias(s)** column).  Most jsqsh command take additional\n"
                + "command line arguments, for details on how command line arguments\n"
                + "work within jsqsh see [[options]].");
        masterPage.println();
        masterPage.println("Command | Alias(s) | Description\n"
                         + "--------|----------|------------");
        for (int i = 0; i < commands.length; i++) {
            Command command = commands[i];
            if (!command.isHidden()) {
                String commandFile = command.getName();
                if (commandFile.charAt(0) == '\\') {
                    commandFile = commandFile.substring(1);
                }
                session.out.println("  " + command.getName());
                masterPage.print("[[");
                masterPage.print(command.getName());
                if (commandFile.indexOf('-') >= 0) {
                    commandFile = commandFile.replace("-", "_");
                }
                masterPage.print('|');
                masterPage.print(commandFile);
                masterPage.print("]] | ");
                int cnt = 0;
                for (Alias alias : aliases) {
                    if (alias.getText().equals(command.getName())) {
                        if (cnt > 0) {
                            masterPage.print(", ");
                        }
                        ++cnt;
                        masterPage.print(alias.getName());
                    }
                }
                masterPage.print(" | ");
                masterPage.println(command.getDescription());
                PrintStream commandDocs = open(dir, commandFile + ".md");
                commandDocs.println("# " + command.getName());
                commandDocs.println();
                commandDocs.println(command.getDescription());
                commandDocs.println();
                commandDocs.println(command.getHelp());
                commandDocs.close();
            }
        }
        masterPage.close();
    }

    private void doTopics(Session session, File dir) throws IOException {
        HelpTopic[] topics = session.getHelpManager().getTopics();
        Arrays.sort(topics);

        PrintStream masterPage = open(dir, "Topics-Reference.md");
        masterPage.println("This page contains general topics about jsqsh");
        masterPage.println();
        masterPage.println("Topic | Description\n"
                + "--------|------------");
        for (HelpTopic topic : topics) {
            session.out.println("  " + topic.getTopic());
            masterPage.print("[[");
            masterPage.print(topic.getTopic());
            masterPage.print("]] | ");
            masterPage.println(topic.getDescription());
            PrintStream topicDocs = open(dir, topic.getTopic() + ".md");
            topicDocs.println(topic.getHelp());
            topicDocs.close();
        }
        masterPage.close();
    }

    private void doVariables(Session session, File dir) throws IOException {
        Variable[] variables = session.getVariableManager().getVariables(true);
        Arrays.sort(variables);

        PrintStream masterPage = open(dir, "Variables-Reference.md");
        masterPage.println("Like any shell, jsqsh allows variables to be defined and used in\n"
                + "almost any context (you can use the [[\\set]] command to set them).\n"
                + "The variables below are special in that they allow you to configure\n"
                + "or view various aspects of jsqsh's behavior.");
        masterPage.println();
        masterPage.println("Variable | Description\n"
                + "--------|------------");

        for (Variable var : variables) {
            session.out.println("  " + var.getName());
            masterPage.print("[[");
            masterPage.print(var.getName());
            masterPage.print("]] | ");
            masterPage.println(var.getDescription());
            PrintStream varDoc = open(dir, var.getName() + ".md");
            varDoc.println(var.getHelp());
            varDoc.close();
        }

        masterPage.close();
    }

    private PrintStream open(File dir, String filename) throws IOException {
        return new PrintStream(new File(dir, filename));
    }

}

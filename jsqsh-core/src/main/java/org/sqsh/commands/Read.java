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

import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;
import org.sqsh.Command;
import org.sqsh.Session;
import org.sqsh.SqshConsole;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;
import org.sqsh.options.OptionProperty;

import java.util.ArrayList;
import java.util.List;

import static org.sqsh.options.ArgumentRequired.NONE;
import static org.sqsh.options.ArgumentRequired.REQUIRED;

public class Read extends Command {

    /**
     * Used to contain the command line options that were passed in by the caller.
     */
    private static class Options extends SqshOptions {
        @OptionProperty(option = 'p', longOption = "prompt", arg = REQUIRED, argName = "prompt",
                description = "A prompt to display prior to allowing input from the user")
        public String prompt = "";

        @OptionProperty(option = 'u', longOption = "if-unset", arg = NONE,
                description = "Only perform the read if any of the variable are not already set")
        public boolean ifUnset = false;

        @OptionProperty(option = 's', longOption = "silent", arg = NONE,
                description = "Silent mode. Characters are not echoed to the screen")
        public boolean isSilent = false;

        @Argv(program = "\\read", min = 1, usage = "[-p prompt] [-u] [-s] var [var [var ...]]")
        public List<String> arguments = new ArrayList<>();
    }

    @Override
    public SqshOptions getOptions() {
        return new Options();
    }

    @Override
    public int execute(Session session, SqshOptions opts) throws Exception {
        Options options = (Options) opts;

        if (options.ifUnset) {
            boolean hasUnset = false;
            for (int i = 0; !hasUnset && i < options.arguments.size(); i++) {
                if (session.getVariable(options.arguments.get(i)) == null) {
                    hasUnset = true;
                }
            }
            if (!hasUnset) {
                return 0;
            }
        }

        SqshConsole in = session.getContext().getConsole();
        String str;
        try {
            str = in.readSingleLine(options.prompt, options.isSilent ? '*' : null);
        } catch (UserInterruptException | EndOfFileException e) {
            str = null;
        }

        String[] words = splitIntoWords(str == null ? "" : str, options.arguments.size());
        for (int i = 0; i < options.arguments.size(); i++) {
            if (i < words.length) {
                session.setVariable(options.arguments.get(i), words[i]);
            } else {
                session.setVariable(options.arguments.get(i), "");
            }
        }

        return 0;
    }

    private String[] splitIntoWords(String str, int nWords) {
        if (nWords == 1) {
            return new String[]{str};
        }
        List<String> words = new ArrayList<>();
        int len = str.length();
        int startIdx = 0;
        int wordCount = 0;

        while (wordCount < (nWords - 1)) {
            // Skip leading whitespace
            for (; startIdx < len && Character.isWhitespace(str.charAt(startIdx)); ++startIdx);

            int endIdx = startIdx;

            // Skip all non-whitespace to find end of the word
            for (; endIdx < len && !Character.isWhitespace(str.charAt(endIdx)); ++endIdx);

            if (endIdx > startIdx) {
                words.add(str.substring(startIdx, endIdx));
            }
            startIdx = endIdx;
            ++wordCount;
        }
        // Skip whitespace for the last portion
        for (; startIdx < len && Character.isWhitespace(str.charAt(startIdx)); ++startIdx) ;

        if (startIdx < len) {
            words.add(str.substring(startIdx));
        }
        return words.toArray(new String[0]);
    }
}

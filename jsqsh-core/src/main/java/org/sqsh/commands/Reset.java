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

import org.sqsh.Command;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the \reset command.
 */
public class Reset extends Command {

    /**
     * Used to contain the command line options that were passed in by the caller.
     */
    private static class Options extends SqshOptions {
        @Argv(program = "\\reset", min = 0, max = 0)
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
        session.getBufferManager().newBuffer();
        return 0;
    }
}

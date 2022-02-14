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
import org.sqsh.Command;
import org.sqsh.Renderer;
import org.sqsh.SQLDriver;
import org.sqsh.SQLDriverManager;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;
import org.sqsh.options.OptionProperty;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.sqsh.options.ArgumentRequired.NONE;
import static org.sqsh.options.ArgumentRequired.REQUIRED;

/**
 * Implements the \drivers command.
 */
public class Drivers extends Command {

    private static class Options extends SqshOptions {
        @OptionProperty(option = 's', longOption = "setup", arg = NONE,
                description = "Enters the driver setup wizard")
        public boolean doSetup = false;

        @OptionProperty(option = 'l', longOption = "load", arg = REQUIRED, argName = "file",
                description = "Specifies a drivers.xml format file to load")
        public String file = null;

        @Argv(program = "\\drivers", min = 0, max = 0, usage = "[--load driver.xml | --setup]")
        public List<String> arguments = new ArrayList<String>();
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

        SQLDriverManager driverMan = session.getDriverManager();
        if (options.doSetup) {
            Command setup = session.getCommandManager().getCommand("\\setup");
            return setup.execute(session, new String[]{"drivers"});
        }

        if (options.file != null) {
            File file = new File(options.file);
            if (file.exists()) {
                driverMan.load(file);
            } else {
                session.err.println("Unable to load driver file '" + options.file + "'");
                return 1;
            }
        } else {
            SQLDriver[] drivers = driverMan.getDrivers();
            Arrays.sort(drivers);

            ColumnDescription[] columns = new ColumnDescription[4];
            columns[0] = new ColumnDescription("Target", -1);
            columns[1] = new ColumnDescription("Name", -1);
            columns[2] = new ColumnDescription("URL", -1);
            columns[3] = new ColumnDescription("Class", -1);
            Renderer renderer = session.getRendererManager().getCommandRenderer(session);
            renderer.header(columns);
            for (SQLDriver driver : drivers) {
                String target = driver.getTarget();
                if (driver.isAvailable()) {
                    target = "* " + target;
                } else {
                    target = "  " + target;
                }
                String[] row = new String[4];
                row[0] = target;
                row[1] = driver.getName();
                row[2] = driver.getUrl();
                row[3] = driver.getDriverClass();
                renderer.row(row);
            }
            renderer.flush();
        }
        return 0;
    }
}

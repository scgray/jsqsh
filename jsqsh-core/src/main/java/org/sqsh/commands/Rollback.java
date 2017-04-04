package org.sqsh.commands;

import org.sqsh.Command;
import org.sqsh.ConnectionContext;
import org.sqsh.DatabaseCommand;
import org.sqsh.SQLConnectionContext;
import org.sqsh.SQLTools;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Rollback
        extends Command
        implements DatabaseCommand {

    private static class Options
            extends SqshOptions {

        @Argv(program="\\rollback", usage="", min=0, max=0)
        public List<String> arguments = new ArrayList<String>();
    }

    @Override
    public SqshOptions getOptions() {

        return new Options();
    }

    @Override
    public int execute (Session session, SqshOptions opts)
            throws Exception {

        ConnectionContext connectionContext = session.getConnectionContext();
        if (connectionContext instanceof SQLConnectionContext) {

            try {

                (((SQLConnectionContext) connectionContext).getConnection()).rollback();
            }
            catch (SQLException e) {

                SQLTools.printException(session, e);
                return 1;
            }
        }

        return 0;
    }
}

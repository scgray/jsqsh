package org.sqsh.commands;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.sqsh.Command;
import org.sqsh.Session;
import org.sqsh.SqshOptions;

/**
 * Implements the debug command.
 */
public class Debug
    extends Command {

    @Override
    public int execute (Session session, SqshOptions options)
        throws Exception {
        
        boolean ok = true;

        for (int i = 0; i < options.arguments.size(); i++) {
            
            String name = options.arguments.get(i);
            Logger log = Logger.getLogger(name);
            
            if (log != null) {
                
                log.setLevel(Level.FINE);
            }
            else {
                
                session.err.println("Unable to find logger '"
                    + name + "'");
                ok = false;
            }
        }
        
        return (ok ? 0 : 1);
    }
}

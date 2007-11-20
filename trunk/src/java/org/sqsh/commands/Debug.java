package org.sqsh.commands;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.sqsh.Command;
import org.sqsh.Session;

/**
 * Implements the debug command.
 */
public class Debug
    extends Command {

    @Override
    public int execute (Session session, String[] argv)
        throws Exception {
        
        boolean ok = true;

        for (int i = 0; i < argv.length; i++) {
            
            String name = argv[i];
            Logger log = Logger.getLogger(argv[i]);
            
            if (log != null) {
                
                log.setLevel(Level.FINE);
            }
            else {
                
                session.err.println("Unable to find logger '"
                    + argv[i] + "'");
                ok = false;
            }
        }
        
        return (ok ? 0 : 1);
    }
}

package org.sqsh.commands;

import org.sqsh.Command;
import org.sqsh.Session;
import org.sqsh.SqshContextSwitchMessage;
import org.sqsh.SqshOptions;

/**
 * Implements the \end command to end the current session and switch to
 * another session.
 */
public class End
    extends Command {

    @Override
    public int execute (Session session, SqshOptions options)
        throws Exception {
        
        int targetSession = -1;
        Session target = null;
        
        if (options.arguments.size() > 1) {
            
            session.err.println("Use: \\end [next-session]");
            return 1;
        }
        
        if (options.arguments.size() == 1) {
            
            try {
                
                targetSession = Integer.parseInt(options.arguments.get(0));
            }
            catch (NumberFormatException e) {
                
                session.err.println("Invalid session id '" 
                    + options.arguments.get(0) + "'");
                return 1;
            }
            
            target = session.getContext().getSession(targetSession);
            if (target == null) {
                
                session.err.println("Specified next session id '"
                    + options.arguments.get(0) + "'" + " does not exist");
                return 1;
            }
        }
        
        throw new SqshContextSwitchMessage(session, target, true);
    }
}

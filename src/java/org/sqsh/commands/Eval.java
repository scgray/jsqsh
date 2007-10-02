package org.sqsh.commands;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.sqsh.Command;
import org.sqsh.Session;

/**
 * Implements the \eval command.
 */
public class Eval
    extends Command {

    @Override
    public int execute (Session session, String[] argv)
        throws Exception {
        
        if (argv.length != 1) {
            
            session.err.println("use: \\eval filename");
            return 1;
        }
        
        File filename = new File(argv[0]);
        if (!filename.canRead()) {
            
            session.err.println("Cannot open '" + filename.toString()
                + "' for read'");
            return 1;
        }
        
        InputStream origIn = session.in;
        boolean wasInteractive = session.isInteractive();
        InputStream in = 
            new BufferedInputStream(new FileInputStream(filename));
        session.setIn(in);
        
        try {
            
            session.setInteractive(false);
            session.readEvalPrint();
        }
        catch (Exception e) {
            
            return 1;
        }
        finally {
            
            session.setIn(origIn);
            session.setInteractive(wasInteractive);
        }
        
        return 0;
    }

}

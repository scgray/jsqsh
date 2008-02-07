package org.sqsh.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.sqsh.Buffer;
import org.sqsh.Command;
import org.sqsh.Session;
import org.sqsh.SessionRedrawBufferMessage;
import org.sqsh.SqshOptions;

/**
 * Implements the \buf-load command.
 */
public class BufLoad
    extends Command {
    
    private static class Options
       extends SqshOptions {
        
        @Option(name="-a",usage="Appends file contents to specified buffer")
            public boolean doAppend = false;
    }
    
    @Override
    public SqshOptions getOptions() {
        
        return new Options();
    }

    @Override
    public int execute (Session session, SqshOptions opts)
        throws Exception {
        
        Options options = (Options) opts;
        if (options.arguments.size() > 2 || options.arguments.size() < 1) {
            
            session.err.println("use: \buf-load [-a] filename [dest-buf]");
            return 1;
        }
        
        String filename = options.arguments.get(0);
        String destBuf = "!.";
        if (options.arguments.size() == 2) {
            
            destBuf = options.arguments.get(1);
        }
        
        Buffer buf = session.getBufferManager().getBuffer(destBuf);
        if (buf == null) {
            
            session.err.println("Specified destination buffer '" + destBuf
                + "' does not exist");
            return 1;
        }
        
        File file = new File(filename);
        if (file.exists() == false) {
            
            session.err.println("File '" + filename + "' does not exist");
            return 1;
        }
        
        if (options.doAppend == false) {
            
            buf.clear();
        }
        
        buf.load(file);
        
        if (buf == session.getBufferManager().getCurrent()) {
            
            throw new SessionRedrawBufferMessage();
        }
        
        return 0;
    }

}

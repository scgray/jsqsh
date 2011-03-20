package org.sqsh.input;

import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineLibrary;
import org.sqsh.SqshContext;

public class PureJavaLineReader
        extends ReadlineLineReader {
    
    public PureJavaLineReader(SqshContext ctx)
        throws ConsoleException {
       
        try {
            
            Readline.load(ReadlineLibrary.PureJava);
        }
        catch (Throwable e) {
            
            throw new ConsoleException(e.getMessage(), e);
        }
        
        init(ctx);
    }

    @Override
    public String getName() {

        return "PureJava";
    }
}
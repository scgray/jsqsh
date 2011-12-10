package org.sqsh.input.completion;

import org.sqsh.Session;

/**
 * The NullCompleter is a completer that never completes anything. It
 * is used for connection types and contexts where we cannot do tab 
 * completion.
 */
public class NullCompleter
    extends Completer {
    
    public NullCompleter (Session session, String line, int position, 
        String word) {
        
        super(session, line, position, word);
    }

    @Override
    public String next() {

        return null;
    }
}

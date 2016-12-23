/*
 * Copyright 2007-2012 Scott C. Gray
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
package org.sqsh.completion;

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

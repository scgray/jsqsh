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
package org.sqsh.signals;

/**
 * Implementation of a signal handler for JVM's that don't support the
 * functionality. This implementation does nothing but print out a warning
 * that signal handling will not be supported.
 */
public class NullSignalCatcher
    extends AbstractSignalCatcher {
    
    /**
     * Creates a signal handler.
     * 
     * @param manager The manager that created us.
     */
    public NullSignalCatcher(SignalManager manager) {
        
        super(manager);
        System.out.println(
            "WARNING: This JVM does not support signal handling. This means "
                 + "that CTRL-C cannot be used to cancel queries. Currently "
                 + "only the Sun JVM (http://java.sun.com) is supported.");
    }
}

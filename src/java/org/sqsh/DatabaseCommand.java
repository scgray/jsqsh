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
package org.sqsh;

/**
 * This interface is just a marker for commands that wish to declare that
 * they require a database connection in order to function. If a command
 * is executed that implements this interface and a connection is not
 * established, and error message will be displayed and the command will
 * not be executed.
 */
public interface DatabaseCommand {
    
    /* EMPTY */
}

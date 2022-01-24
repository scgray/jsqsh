/*
 * Copyright 2007-2022 Scott C. Gray
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
 * A session object is an object that can be attached to the session by
 * a command. This allows commands to maintain arbitrary state in a cleaner
 * fashion than shoving a lot of variables into the session's variable map.
 */
public class SessionObject {
    
    /**
     * Close will be called by the session when the session is closed. The
     * default implementation does nothing, but implementations that
     * require some sort of cleanup to happen should implement this method.
     */
    public void close()
    {
        /* EMPTY */
    }
}

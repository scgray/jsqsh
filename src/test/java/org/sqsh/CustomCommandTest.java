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

import org.junit.Test;
import org.junit.Assert;

public class CustomCommandTest {
    
    public static class TestCommand extends Command {
        
        @Override
        public SqshOptions getOptions() {
            
            return new SqshOptions();
        }
        
        @Override
        public int execute (Session session, SqshOptions opts)
            throws Exception {
            
            return 0;
        }
     }
    
    @Test
    public void testCustomCommand() {
        
        CommandManager cm = new CommandManager();
        Command command = cm.getCommand("\\test_command");
        Assert.assertNotNull(command);
    }
}

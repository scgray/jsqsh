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

import static org.sqsh.options.ArgumentRequired.NONE;

import org.sqsh.options.OptionProperty;

/**
 * This object represents the command line options that are accepted by
 * a jsqsh command. All jsqsh commands accept at least the following
 * options due to inheritence from this class:
 * 
 * <pre>
 *    -g  Send output to a popup graphical window
 * </pre>
 * 
 * Commands should avoid utilizing these command line switches.
 */
public class SqshOptions {
    
    @OptionProperty(
        option='h', longOption="help", arg=NONE,
        description="Display help for command line usage")
    public boolean doHelp = false;
    
    @OptionProperty(
        option='g', longOption="gui", arg=NONE,
        description="Send all command output to a graphical window")
    public boolean isGraphical = false;
}

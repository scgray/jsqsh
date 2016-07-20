/*
 * Copyright 2007-2016 Scott C. Gray
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
 * An <code>ExtensionConfigurator</code> is a class that can be created
 * and invoked when a jsqsh extension (see {@link Extension} and 
 * {@link ExtensionManager}) is loaded.  An extension already has hooks
 * that can cause new commands and variables to be added to jsqsh, but
 * an extension may want to do something a little more low level like
 * play around with the current session or context.  A class implementing
 * this interface can be invoked to do just such configuration.
 */
public abstract class ExtensionConfigurator {
    
    /**
     * Method that is invoked after the extension has been loaded. It is 
     * possible to get a handle to the current session (for example, the
     * session that may have imported the extension via the \import command)
     * via {@link SqshContext#getCurrentSession()}.
     * 
     * @param context The context in which the extension was loaded.
     */
    public abstract void configure (SqshContext context, Extension extension);
}

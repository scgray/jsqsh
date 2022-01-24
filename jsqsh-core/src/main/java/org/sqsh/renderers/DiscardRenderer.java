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
package org.sqsh.renderers;

import org.sqsh.ColumnDescription;
import org.sqsh.Renderer;
import org.sqsh.RendererManager;
import org.sqsh.Session;

/**
 * Renderer that doesn't actually do any rendering.  This is used
 * primarily for performance testing (so that you can avoid the
 * overhead involved with the display itself).
 */
public class DiscardRenderer
    extends Renderer {

    public DiscardRenderer(Session session, RendererManager manager) {

        super(session, manager);
    }
    
    @Override
    public boolean isDiscard() {

        return true;
    }

    @Override
    public void header (ColumnDescription[] columns) {

        super.header(columns);
    }
    
    @Override
    public boolean row (String[] row) {
        
        return true;
    }

    @Override
    public boolean flush () {

        return true;
    }
}

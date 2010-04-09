package org.sqsh.renderers;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.sqsh.ColumnDescription;
import org.sqsh.Renderer;
import org.sqsh.RendererManager;
import org.sqsh.SQLTools;
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

    /* (non-Javadoc)
     * @see org.sqsh.Renderer#footer(java.lang.String)
     */
    @Override
    public void footer (String footer) {

    }
}

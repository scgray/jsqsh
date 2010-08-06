/*
 * Copyright (C) 2007 by Scott C. Gray
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, write to the Free Software Foundation, 675 Mass Ave,
 * Cambridge, MA 02139, USA.
 */
package org.sqsh.commands;

import static org.sqsh.options.ArgumentRequired.REQUIRED;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;

import org.sqsh.Command;
import org.sqsh.DatabaseCommand;
import org.sqsh.Renderer;
import org.sqsh.SQLRenderer;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;
import org.sqsh.options.Option;
import org.sqsh.renderers.GraphicalTreeRenderer;
import org.sqsh.renderers.GraphicalTreeRenderer.JSqshNode;

/**
 * Implements the \tree command. A clone of the table command
 */
public class Tree extends Command implements DatabaseCommand,
        TreeWillExpandListener, TreeSelectionListener {

    private Session session;

    private static class Options extends SqshOptions {

        @Option(option = 't', longOption = "table-pattern", arg = REQUIRED, argName = "pattern", description = "Provides a pattern to match against table names")
        public String tablePattern = "%";

        @Option(option = 's', longOption = "schema-pattern", arg = REQUIRED, argName = "pattern", description = "Provides a pattern to match against schema names")
        public String schemaPattern = "%";

        @Argv(program = "\\tree", min = 0, max = 1, usage = "[-t table-pattern] [-s schema-pattern] [type]")
        public List<String> arguments = new ArrayList<String>();
    }

    @Override
    public SqshOptions getOptions() {

        return new Options();
    }

    @Override
    public int execute(final Session session, final SqshOptions opts)
            throws Exception {

        final Options options = (Options) opts;
        String type = null;

        this.session = session;

        if (options.arguments.size() > 0) {

            type = options.arguments.get(0);
        }

        final Connection con = session.getConnection();
        ResultSet result = null;

        try {

            //stripped from the tabes command
            String[] types = null;
            if (type != null) {

                types = new String[1];
                if ("TABLE".equals(type.toUpperCase())
                        || "USER".equals(type.toUpperCase())) {

                    types[0] = "TABLE";
                }
                else if ("VIEW".equals(type.toUpperCase())) {

                    types[0] = "VIEW";
                }
                else if ("SYSTEM".equals(type.toUpperCase())) {

                    types[0] = "SYSTEM TABLE";
                }
                else if ("ALIAS".equals(type.toUpperCase())) {

                    types[0] = "ALIAS";
                }
                else if ("SYNONYM".equals(type.toUpperCase())) {

                    types[0] = "SYNONYM";
                }
                else {

                    session.err.println("The object type '" + type
                            + "' is not recognized. "
                            + "Valid types are: user, system, view, "
                            + "alias, synonym");
                    return 1;
                }
            }

            final DatabaseMetaData meta = con.getMetaData();

            result = meta.getTables(con.getCatalog(), options.schemaPattern,
                    options.tablePattern, types);

            final SQLRenderer sqlRenderer = session.getSQLRenderer();
            final Renderer renderer = session.getContext().getRendererManager()
                    .getRenderer(session, "tree");

            // displaying only the columns I want to see in the order I want.
            
            ((GraphicalTreeRenderer) renderer)
                    .setColOrder(new int[] { 1, 3, 2 });

            // setting listener
            ((GraphicalTreeRenderer) renderer)
                .setTreeListeners(this, this);

            sqlRenderer.displayResults(renderer, session, result, null);

        }
        catch (final SQLException e) {

            session.err.println("Failed to retrieve database metadata: "
                    + e.getMessage());

            return 1;
        }
        finally {

            if (result != null) {

                try {

                    result.close();
                }
                catch (final SQLException e) {

                    /* IGNORED */
                }
            }
        }

        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public void treeWillCollapse(final TreeExpansionEvent event)
            throws ExpandVetoException {

    }

    /**
     * {@inheritDoc}
     * 
     * on the third expand event show tables
     */
    public void treeWillExpand(final TreeExpansionEvent event)
            throws ExpandVetoException {

        //Check where we are at.
        if (event.getPath().getPathCount() == 3) {

            //Force all table name to say they are not leafs
            final JSqshNode node = (JSqshNode) event
                    .getPath().getLastPathComponent();

            final Enumeration<JSqshNode> kids = node.children();

            while (kids.hasMoreElements()) {

                final JSqshNode kid = kids.nextElement();

                if (kid.isLeaf()) {
                
                    kid.forceHasChildren(true);
                    
                }
                
            }
        }
        else if (event.getPath().getPathCount() == 4) {
                
            //If expanding a table go and get the columns
            
            final JSqshNode node = (JSqshNode) event
                .getPath().getLastPathComponent();
            
            if (node.isTrueLeaf()) {

                try {

                    final DatabaseMetaData meta = 
                    session.getConnection().getMetaData();

                    final ResultSet set = meta.getColumns(null, node
                            .getParent().getParent().toString(), 
                            node.toString(), null);

                    while (set.next()) {

                        node.add(new DefaultMutableTreeNode(
                                set.getString(4)));

                    }

                    set.close();
                }
                catch (final Exception e) {
                    
                    session.err.println("Failed to retrieve table metadata: "
                            + e.getMessage());
                    e.printStackTrace(session.err);
                }
            }
        }
    }

    /**
     * This is here to add features for selection listeners to the tree.
     */
    
    public void valueChanged(TreeSelectionEvent e) {

    }
}

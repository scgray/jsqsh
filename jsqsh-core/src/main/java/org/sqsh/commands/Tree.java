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
package org.sqsh.commands;

import org.sqsh.Command;
import org.sqsh.DatabaseCommand;
import org.sqsh.Renderer;
import org.sqsh.SQLRenderer;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;
import org.sqsh.options.OptionProperty;
import org.sqsh.renderers.GraphicalTreeRenderer;
import org.sqsh.renderers.GraphicalTreeRenderer.JSqshNode;

import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static org.sqsh.options.ArgumentRequired.REQUIRED;

/**
 * Implements the \tree command. A clone of the table command
 */
public class Tree extends Command implements DatabaseCommand, TreeWillExpandListener, TreeSelectionListener {

    private Session session;

    private static class Options extends SqshOptions {
        @OptionProperty(option = 't', longOption = "table-pattern", arg = REQUIRED, argName = "pattern",
                description = "Provides a pattern to match against table names")
        public String tablePattern = "%";

        @OptionProperty(option = 's', longOption = "schema-pattern", arg = REQUIRED, argName = "pattern",
                description = "Provides a pattern to match against schema names")
        public String schemaPattern = "%";

        @Argv(program = "\\tree", min = 0, max = 1, usage = "[-t table-pattern] [-s schema-pattern] [type]")
        public List<String> arguments = new ArrayList<>();
    }

    @Override
    public SqshOptions getOptions() {
        return new Options();
    }

    @Override
    public int execute(final Session session, final SqshOptions opts) throws Exception {
        final Options options = (Options) opts;
        String type = null;
        this.session = session;

        if (options.arguments.size() > 0) {
            type = options.arguments.get(0);
        }

        //stripped from the tabes command
        String[] types = null;
        if (type != null) {
            types = new String[1];
            if ("TABLE".equalsIgnoreCase(type) || "USER".equalsIgnoreCase(type)) {
                types[0] = "TABLE";
            } else if ("VIEW".equalsIgnoreCase(type)) {
                types[0] = "VIEW";
            } else if ("SYSTEM".equalsIgnoreCase(type)) {
                types[0] = "SYSTEM TABLE";
            } else if ("ALIAS".equalsIgnoreCase(type)) {
                types[0] = "ALIAS";
            } else if ("SYNONYM".equalsIgnoreCase(type)) {
                types[0] = "SYNONYM";
            } else {
                session.err.println("The object type '" + type + "' is not recognized. Valid types are: "
                        + "user, system, view, alias, synonym");
                return 1;
            }
        }

        final Connection con = session.getConnection();
        final DatabaseMetaData meta = con.getMetaData();
        try (ResultSet result = meta.getTables(con.getCatalog(), options.schemaPattern, options.tablePattern, types)) {
            final SQLRenderer sqlRenderer = session.getSQLRenderer();
            final Renderer renderer = session.getContext().getRendererManager().getRenderer(session, "tree");
            // displaying only the columns I want to see in the order I want.
            ((GraphicalTreeRenderer) renderer).setColOrder(new int[]{1, 3, 2});
            // setting listener
            ((GraphicalTreeRenderer) renderer).setTreeListeners(this, this);
            sqlRenderer.displayResults(renderer, session, result, null);

        } catch (final SQLException e) {
            session.err.println("Failed to retrieve database metadata: " + e.getMessage());
            return 1;
        }

        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public void treeWillCollapse(final TreeExpansionEvent event) throws ExpandVetoException {
    }

    /**
     * {@inheritDoc}
     * <p>
     * on the third expand event show tables
     */
    public void treeWillExpand(final TreeExpansionEvent event) throws ExpandVetoException {
        //Check where we are at.
        if (event.getPath().getPathCount() == 3) {
            //Force all table name to say they are not leafs
            final JSqshNode node = (JSqshNode) event.getPath().getLastPathComponent();
            final Enumeration<TreeNode> kids = node.children();
            while (kids.hasMoreElements()) {
                final JSqshNode kid = (JSqshNode) kids.nextElement();
                if (kid.isLeaf()) {
                    kid.forceHasChildren(true);
                }
            }
        } else if (event.getPath().getPathCount() == 4) {
            //If expanding a table go and get the columns
            final JSqshNode node = (JSqshNode) event.getPath().getLastPathComponent();
            if (node.isTrueLeaf()) {
                try {
                    final DatabaseMetaData meta = session.getConnection().getMetaData();
                    final ResultSet set = meta.getColumns(null,
                            node.getParent().getParent().toString(),
                            node.toString(),
                            null);

                    while (set.next()) {
                        node.add(new DefaultMutableTreeNode(set.getString(4)));
                    }
                    set.close();
                } catch (final Exception e) {
                    session.err.println("Failed to retrieve table metadata: " + e.getMessage());
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

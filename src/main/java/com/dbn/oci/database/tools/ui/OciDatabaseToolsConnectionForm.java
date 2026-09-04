/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.oci.database.tools.ui;

import com.dbn.common.thread.Progress;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.list.ColoredListCellRenderer;
import com.dbn.common.ui.tree.DBNColoredTreeCellRenderer;
import com.dbn.common.ui.tree.DBNTree;
import com.dbn.common.ui.util.Mouse;
import com.dbn.common.ui.util.Splitters;
import com.dbn.common.util.Messages;
import com.dbn.oci.config.OciAuthenticationConfig;
import com.dbn.oci.database.tools.OciCompartmentInfo;
import com.dbn.oci.database.tools.OciDatabaseToolsConnectionInfo;
import com.dbn.oci.database.tools.OciDatabaseToolsConnectionLoader;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.oci.util.OciIdentifiers.isTenancyOcid;

class OciDatabaseToolsConnectionForm extends DBNFormBase {
    private DBNTree compartmentsTree;
    private JList<OciDatabaseToolsConnectionInfo> connectionsList;
    private JSplitPane contentSplitPane;
    private JPanel mainPanel;

    private final OciDatabaseToolsConnectionDialog dialog;
    private final OciAuthenticationConfig authentication;
    private final OciDatabaseToolsConnectionLoader loader = new OciDatabaseToolsConnectionLoader();

    OciDatabaseToolsConnectionForm(OciDatabaseToolsConnectionDialog dialog, OciAuthenticationConfig authentication) {
        super(dialog);
        this.dialog = dialog;
        this.authentication = authentication;

        compartmentsTree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode()));
        compartmentsTree.setRootVisible(false);
        compartmentsTree.addTreeSelectionListener(e -> loadConnections());
        connectionsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) dialog.setSelectedConnection(connectionsList.getSelectedValue());
        });
        connectionsList.setCellRenderer(new ConnectionRenderer());
        connectionsList.addMouseListener(Mouse.listener().onClick(e -> {
            if (Mouse.isMainDoubleClick(e)) dialog.acceptSelectedConnection();
        }));

        Splitters.makeRegular(contentSplitPane);
        Splitters.setSplitPaneProportion(contentSplitPane, 0.4);
        setAccessibleName(compartmentsTree, txt("cfg.oci.aria.Compartments"));
        setAccessibleName(connectionsList, txt("cfg.oci.aria.DatabaseToolsConnections"));
        whenFirstShown(this::loadCompartments);
    }

    private void loadCompartments() {
        Progress.prompt(getProject(), null, true,
                txt("prc.oci.title.LoadingCompartments"),
                txt("prc.oci.text.LoadingCompartments"), progress -> {
                    try {
                        List<OciCompartmentInfo> compartments = loader.loadCompartments(authentication);
                        if (!progress.isCanceled()) dispatch(() -> setCompartments(compartments));
                    } catch (Exception e) {
                        dispatch(() -> Messages.showErrorDialog(
                                getProject(), txt("msg.oci.title.LoadCompartmentsFailed"), e));
                    }
                });
    }

    private void setCompartments(List<OciCompartmentInfo> compartments) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        Map<String, DefaultMutableTreeNode> nodes = new HashMap<>();
        compartments.forEach(compartment -> nodes.put(
                compartment.getId(), new DefaultMutableTreeNode(compartment)));
        compartments.forEach(compartment -> {
            DefaultMutableTreeNode node = nodes.get(compartment.getId());
            DefaultMutableTreeNode parent = nodes.get(compartment.getParentId());
            (parent == null ? root : parent).add(node);
        });

        compartmentsTree.setModel(new DefaultTreeModel(root));
        compartmentsTree.setRootVisible(false);
        compartmentsTree.setCellRenderer(new CompartmentRenderer());
        for (int row = 0; row < compartmentsTree.getRowCount(); row++) {
            compartmentsTree.expandRow(row);
        }
        if (root.getChildCount() > 0) {
            DefaultMutableTreeNode firstNode = (DefaultMutableTreeNode) root.getChildAt(0);
            compartmentsTree.setSelectionPath(new TreePath(firstNode.getPath()));
        }
    }

    private void loadConnections() {
        TreePath path = compartmentsTree.getSelectionPath();
        if (path == null) return;

        Object value = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
        if (!(value instanceof OciCompartmentInfo compartment)) return;

        dialog.setSelectedConnection(null);
        connectionsList.setModel(new DefaultListModel<>());
        Progress.prompt(getProject(), null, true,
                txt("prc.oci.title.LoadingDatabaseToolsConnections"),
                txt("prc.oci.text.LoadingDatabaseToolsConnections"), progress -> {
                    try {
                        List<OciDatabaseToolsConnectionInfo> connections =
                                loader.loadConnections(authentication, compartment.getId());
                        if (!progress.isCanceled()) {
                            dispatch(() -> setConnections(compartment.getId(), connections));
                        }
                    } catch (Exception e) {
                        dispatch(() -> Messages.showErrorDialog(
                                getProject(), txt("msg.oci.title.LoadDatabaseToolsConnectionsFailed"), e));
                    }
                });
    }

    private void setConnections(String compartmentId, List<OciDatabaseToolsConnectionInfo> connections) {
        if (!isSelectedCompartment(compartmentId)) return;

        DefaultListModel<OciDatabaseToolsConnectionInfo> model = new DefaultListModel<>();
        connections.forEach(model::addElement);
        connectionsList.setModel(model);
    }

    private boolean isSelectedCompartment(String compartmentId) {
        TreePath path = compartmentsTree.getSelectionPath();
        if (path == null) return false;

        Object value = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
        return value instanceof OciCompartmentInfo compartment && compartment.getId().equals(compartmentId);
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    private void createUIComponents() {
        compartmentsTree = new DBNTree(this);
    }

    private static class CompartmentRenderer extends DBNColoredTreeCellRenderer {
        @Override
        protected void customizeCellRenderer(DBNTree tree, Object value, boolean selected,
                boolean expanded, boolean leaf, int row, boolean hasFocus) {
            if (value instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof OciCompartmentInfo compartment) {
                String name = compartment.getName();
                if (isTenancyOcid(compartment.getId())) name += " (root)";
                append(name, SimpleTextAttributes.REGULAR_ATTRIBUTES);
                setToolTipText(compartment.getId());
            }
        }
    }

    private static class ConnectionRenderer extends ColoredListCellRenderer<OciDatabaseToolsConnectionInfo> {
        @Override
        protected void customize(@NotNull JList<? extends OciDatabaseToolsConnectionInfo> list,
                OciDatabaseToolsConnectionInfo connection, int index, boolean selected, boolean hasFocus) {
            append(connection.getDisplayName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            setToolTipText(connection.getId());
        }
    }
}

/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.browser.ui;

import com.dbn.browser.model.BrowserTreeNode;
import com.dbn.browser.model.LoadInProgressTreeNode;
import com.dbn.browser.options.DatabaseBrowserSettings;
import com.dbn.common.ui.tree.DBNColoredTreeCellRenderer;
import com.dbn.common.ui.tree.DBNTree;
import com.dbn.common.ui.tree.Trees;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionBundle;
import com.dbn.connection.ConnectionHandler;
import com.dbn.object.DBColumn;
import com.dbn.object.DBSchema;
import com.dbn.object.DBUser;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.common.list.DBObjectList;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTree;
import javax.swing.tree.TreeCellRenderer;
import java.awt.Component;

public class DatabaseBrowserTreeCellRenderer implements TreeCellRenderer {
    private final DefaultTreeCellRenderer cellRenderer = new DefaultTreeCellRenderer();
    private final DatabaseBrowserSettings browserSettings;

    public DatabaseBrowserTreeCellRenderer(@NotNull Project project) {
        browserSettings = DatabaseBrowserSettings.getInstance(project);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        return cellRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
    }

    private class DefaultTreeCellRenderer extends DBNColoredTreeCellRenderer {
        @Override
        public void customizeCellRenderer(DBNTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            if (value instanceof LoadInProgressTreeNode) {
                LoadInProgressTreeNode loadInProgressTreeNode = (LoadInProgressTreeNode) value;
                setIcon(loadInProgressTreeNode.getIcon(0));
                append("Loading...", SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
                return;
            }

            if (!(value instanceof BrowserTreeNode)) return;

            BrowserTreeNode treeNode = (BrowserTreeNode) value;
            setIcon(treeNode.getIcon(0));

            boolean isDirty = false;
            String displayName;
            if (treeNode instanceof ConnectionBundle) {
                displayName = "PROJECT";
            } else {
                displayName = treeNode.getPresentableText();
            }

            if (treeNode instanceof DBObjectList) {
                DBObjectList objectsList = (DBObjectList) treeNode;
                boolean isEmpty = objectsList.getChildCount() == 0;
                isDirty = /*objectsList.isDirty() ||*/ objectsList.isLoading() || (!objectsList.isLoaded() && !hasConnectivity(objectsList));
                SimpleTextAttributes textAttributes =
                        isDirty ? SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES :
                                isEmpty ? SimpleTextAttributes.REGULAR_ATTRIBUTES :
                                        SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;

                append(Commons.nvl(displayName, ""), textAttributes);

                // todo display load error
                    /*
                                SimpleTextAttributes descriptionTA = objectsList.getErrorMessage() == null ?
                                        SimpleTextAttributes.GRAY_ATTRIBUTES : SimpleTextAttributes.ERROR_ATTRIBUTES;
                                append(" " + displayDetails, descriptionTA);

                                if (objectsList.getErrorMessage() != null) {
                                    String msg = "Could not load " + displayName + ". Cause: " + objectsList.getErrorMessage();
                                    setToolTipText(msg);
                                }  else {
                                    setToolTipText(null);
                                }
                    */
            } else {
                boolean showBold = false;
                boolean showGrey = false;
                boolean isDisposed = false;
                if (treeNode instanceof DBObject) {
                    DBObject object = (DBObject) treeNode;
                    if (object instanceof DBSchema) {
                        DBSchema schema = (DBSchema) object;
                        showBold = schema.isUserSchema();
                        showGrey = schema.isEmptySchema();
                    } else if (object instanceof DBUser) {
                        DBUser user = (DBUser) object;
                        showBold = user.isSessionUser();
                        showGrey = user.isExpired();
                    } else if (object instanceof DBSchemaObject) {
                        DBSchemaObject schemaObject = (DBSchemaObject) object;
                        showGrey = schemaObject.isDisabled();
                    }

                    isDisposed = object.isDisposed();
                }

                if (!showGrey && treeNode instanceof DBColumn) {
                    DBColumn column = (DBColumn) treeNode;
                    showGrey = column.isAudit();
                }

                SimpleTextAttributes textAttributes =
                        isDisposed ? SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES :
                                showBold ? (showGrey ? SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES : SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES) :
                                        (showGrey ? SimpleTextAttributes.GRAYED_ATTRIBUTES : SimpleTextAttributes.REGULAR_ATTRIBUTES);

                if (displayName == null) displayName = "displayName null!!";

                append(displayName, textAttributes);

                Trees.applySpeedSearchHighlighting(tree, this, true, selected);
            }
            String displayDetails = treeNode.getPresentableTextDetails();
            if (!Strings.isEmptyOrSpaces(displayDetails)) {
                append(" " + displayDetails, isDirty ? SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES : SimpleTextAttributes.GRAY_ATTRIBUTES);
            }


            if (browserSettings.getGeneralSettings().isShowObjectDetails()) {
                String conditionalDetails = treeNode.getPresentableTextConditionalDetails();
                if (!Strings.isEmptyOrSpaces(conditionalDetails)) {
                    append(" - " + conditionalDetails, SimpleTextAttributes.GRAY_ATTRIBUTES);
                }

            }
        }

        private boolean hasConnectivity(@NotNull DBObjectList objectsList) {
            ConnectionHandler connection = objectsList.getConnection();
            return connection.canConnect() && connection.isValid();
        }
    }
}

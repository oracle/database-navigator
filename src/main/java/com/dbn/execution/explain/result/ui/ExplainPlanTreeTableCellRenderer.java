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

package com.dbn.execution.explain.result.ui;

import com.intellij.ui.treeStructure.treetable.TreeTable;
import com.intellij.ui.treeStructure.treetable.TreeTableCellRenderer;
import com.intellij.ui.treeStructure.treetable.TreeTableTree;

import javax.swing.JComponent;
import javax.swing.JTable;
import java.awt.Component;

public class ExplainPlanTreeTableCellRenderer extends TreeTableCellRenderer {
    private final TreeTableTree tree;

    public ExplainPlanTreeTableCellRenderer(TreeTable treeTable, TreeTableTree tree) {
        super(treeTable, tree);
        this.tree = tree;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        int modelRow  = table.convertRowIndexToModel(row);
        //TableModel model = myTreeTable.getModel();
        //myTree.setTreeTableTreeBorder(hasFocus && model.getColumnClass(column).equals(TreeTableModel.class) ? myDefaultBorder : null);
        tree.setVisibleRow(modelRow);

        final Object treeObject = tree.getPathForRow(modelRow).getLastPathComponent();
        boolean leaf = tree.getModel().isLeaf(treeObject);
        final boolean expanded = tree.isExpanded(modelRow);
        Component component = tree.getCellRenderer().getTreeCellRendererComponent(tree, treeObject, isSelected, expanded, leaf, modelRow, hasFocus);
        if (component instanceof JComponent) {
            table.setToolTipText(((JComponent)component).getToolTipText());
        }

        //myTree.setCellFocused(false);

        return tree;
    }


}

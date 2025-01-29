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

package com.dbn.code.common.completion.options.filter.ui;

import com.dbn.common.color.Colors;
import com.dbn.common.ui.tree.DBNTreeTransferHandler;
import com.dbn.common.ui.tree.Trees;
import com.intellij.ui.CheckboxTree;

import javax.swing.tree.TreeNode;

import static com.dbn.common.ui.util.UserInterface.enableSelectOnFocus;

public class CodeCompletionFilterTree extends CheckboxTree {

    public CodeCompletionFilterTree(CodeCompletionFilterTreeModel model) {
        super(new CodeCompletionFilterTreeCellRenderer(), null);
        setModel(model);
        setRootVisible(true);
        TreeNode expandedTreeNode = (TreeNode) getModel().getChild(getModel().getRoot(), 5);
        setExpandedState(Trees.createTreePath(expandedTreeNode), true);
        installSpeedSearch();
        setTransferHandler(DBNTreeTransferHandler.INSTANCE);
        setBackground(Colors.getTextFieldBackground());

        enableSelectOnFocus(this);
    }
}

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

import com.dbn.code.common.completion.options.filter.CodeCompletionFilterOption;
import com.dbn.code.common.completion.options.filter.CodeCompletionFilterOptionBundle;
import com.dbn.code.common.completion.options.filter.CodeCompletionFilterSettings;
import com.dbn.common.color.Colors;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

public class CodeCompletionFilterTreeCellRenderer extends CheckboxTree.CheckboxTreeCellRenderer { //implements TreeCellEditor {

    @Override
    public void customizeRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object userObject = node.getUserObject();

        ColoredTreeCellRenderer textRenderer = getTextRenderer();
        if (userObject instanceof CodeCompletionFilterOptionBundle) {
            CodeCompletionFilterOptionBundle optionBundle = (CodeCompletionFilterOptionBundle) userObject;
            textRenderer.append(optionBundle.getName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        }
        else if(userObject instanceof CodeCompletionFilterOption) {
            CodeCompletionFilterOption option = (CodeCompletionFilterOption) userObject;
            Icon icon = option.getIcon();
            textRenderer.append(option.getName(), icon == null ?
                    SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES :
                    SimpleTextAttributes.REGULAR_ATTRIBUTES);
            textRenderer.setIcon(icon);
        }
        else if (userObject instanceof CodeCompletionFilterSettings){
            CodeCompletionFilterSettings codeCompletionFilterSettings = (CodeCompletionFilterSettings) userObject;
            textRenderer.append(codeCompletionFilterSettings.getDisplayName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }

        textRenderer.setBackground(Colors.getTextFieldBackground());
        setBackground(Colors.getTextFieldBackground());
    }
}


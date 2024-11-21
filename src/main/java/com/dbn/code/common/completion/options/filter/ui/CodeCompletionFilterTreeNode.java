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
import com.intellij.ui.CheckedTreeNode;

public class CodeCompletionFilterTreeNode extends CheckedTreeNode {
    public CodeCompletionFilterTreeNode(Object userObject, boolean checked) {
        super(userObject);
        super.setChecked(checked);
    }

/*    public boolean isModified() {
        if (isModified) {
            return true;
        } else {
            for (int i = 0; i < getChildCount(); i++) {
                CodeCompletionSchemeTreeNode childNode = (CodeCompletionSchemeTreeNode) getChildAt(i);
                if (childNode.isModified()) {
                    return true;
                }
            }
        }
        return false;
    }*/

    public void applyChanges() {
        Object userObject = getUserObject();
        
        if (userObject instanceof CodeCompletionFilterOption) {
            CodeCompletionFilterOption option = (CodeCompletionFilterOption) userObject;
            option.setSelected(isChecked());
        } else {
            for (int i = 0; i < getChildCount(); i++) {
                CodeCompletionFilterTreeNode childNode = (CodeCompletionFilterTreeNode) getChildAt(i);
                childNode.applyChanges();
            }
        }
    }

    public void resetChanges() {
        Object userObject = getUserObject();

        if (userObject instanceof CodeCompletionFilterOption) {
            CodeCompletionFilterOption option = (CodeCompletionFilterOption) userObject;
            setCheckedSilently(option.isSelected());
        } else {
            for (int i = 0; i < getChildCount(); i++) {
                CodeCompletionFilterTreeNode childNode = (CodeCompletionFilterTreeNode) getChildAt(i);
                childNode.resetChanges();
            }
        }
    }

    @Override
    public void setChecked(boolean checked) {
        setCheckedPropagateDown(checked);
        setCheckedPropagateUp(checked);
        Object userObject = getUserObject();

        if (userObject instanceof CodeCompletionFilterOption) {
            CodeCompletionFilterOption option = (CodeCompletionFilterOption) userObject;
            option.getFilterSettings().setModified(true);
        } else if (userObject instanceof CodeCompletionFilterOptionBundle) {
            CodeCompletionFilterOptionBundle optionBundle = (CodeCompletionFilterOptionBundle) userObject;
            optionBundle.getFilterSettings().setModified(true);
        }
    }

    public void setCheckedSilently(boolean checked) {
        super.setChecked(checked);
    }

    private void setCheckedPropagateDown(boolean checked) {
        setCheckedSilently(checked);
        for (int i = 0; i < getChildCount(); i++) {
            CodeCompletionFilterTreeNode childNode = (CodeCompletionFilterTreeNode) getChildAt(i);
            childNode.setCheckedPropagateDown(isChecked());
        }
    }

    private void setCheckedPropagateUp(boolean checked) {
        setCheckedSilently(checked);
        CodeCompletionFilterTreeNode parent = (CodeCompletionFilterTreeNode) getParent();
        while (parent != null) {
            for (int i = 0; i < parent.getChildCount(); i++) {
                CheckedTreeNode treeNode = (CheckedTreeNode) parent.getChildAt(i);
                if (!treeNode.isChecked()) {
                    parent.setCheckedPropagateUp(false);
                    break;
                }
                if (i == parent.getChildCount() - 1) {
                    parent.setCheckedPropagateUp(true);
                }
            }
            parent = (CodeCompletionFilterTreeNode) parent.getParent();
        }
    }

    public void updateCheckedStatusFromChildren() {
        for (int i = 0; i < getChildCount(); i++) {
            CheckedTreeNode treeNode = (CheckedTreeNode) getChildAt(i);
            if (!treeNode.isChecked()) {
                setCheckedSilently(false);
                break;
            }
            if (i == getChildCount() - 1) {
                setCheckedPropagateUp(true);
            }
        }
    }

    @Override
    public String toString() {
        if (userObject instanceof CodeCompletionFilterOptionBundle) {
            CodeCompletionFilterOptionBundle optionBundle = (CodeCompletionFilterOptionBundle) userObject;
            return optionBundle.getName();
        }
        else if(userObject instanceof CodeCompletionFilterOption) {
            CodeCompletionFilterOption option = (CodeCompletionFilterOption) userObject;
            return option.getName();
        }
        else if (userObject instanceof CodeCompletionFilterSettings){
            CodeCompletionFilterSettings codeCompletionFilterSettings = (CodeCompletionFilterSettings) userObject;
            return codeCompletionFilterSettings.getDisplayName();
        }
        return super.toString();
    }
}

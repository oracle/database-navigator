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

import com.dbn.code.common.completion.options.filter.CodeCompletionFilterSettings;

import javax.swing.tree.DefaultTreeModel;

public class CodeCompletionFilterTreeModel extends DefaultTreeModel {

    public CodeCompletionFilterTreeModel(CodeCompletionFilterSettings setup) {
        super(setup.createCheckedTreeNode());
    }

    @Override
    public CodeCompletionFilterTreeNode getRoot() {
        return (CodeCompletionFilterTreeNode) super.getRoot();
    }

    public void applyChanges() {
        getRoot().applyChanges();
    }

    public void resetChanges() {
        getRoot().resetChanges();
    }


/*
    public Object getChild(Object o, int i) {
        CheckedTreeNode node = (CheckedTreeNode) o;
        return node.getChildAt(i);
    }

    public int getChildCount(Object o) {
        CheckedTreeNode node = (CheckedTreeNode) o;
        return node.getChildCount();
    }

    public boolean isLeaf(Object o) {
        CheckedTreeNode node = (CheckedTreeNode) o;
        return node.isLeaf();
    }


    public int getIndexOfChild(Object o, Object o1) {
        CheckedTreeNode node = (CheckedTreeNode) o;
        return node.getIndex((TreeNode) o1);
    }*/
}

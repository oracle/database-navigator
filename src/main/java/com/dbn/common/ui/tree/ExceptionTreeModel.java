package com.dbn.common.ui.tree;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

public class ExceptionTreeModel extends DefaultTreeModel {

        public ExceptionTreeModel(TreeNode root) {
            super(root, true);
        }

        @Override
        public Object getChild(Object parent, int index) {
            return super.getChild(parent, index);
        }
}

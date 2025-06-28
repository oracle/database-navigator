package com.dbn.common.ui.tree;

import com.dbn.common.ui.dialog.ExceptionTreeDialog;

import javax.swing.tree.DefaultMutableTreeNode;

public class ExceptionTreeNode extends DefaultMutableTreeNode {

    public ExceptionTreeNode(Throwable e) {
        super(e);
        if (e.getCause() != null && e.getCause() != e) {
            add(new ExceptionTreeNode(e.getCause()));
        }
    }

    @Override
    public boolean getAllowsChildren() {
        return getUserObject().getCause()!=null && getUserObject().getCause() != getUserObject();
    }

    public Throwable getUserObject() {
        return (Throwable) super.getUserObject();
    }
}
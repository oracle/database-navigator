package com.dbn.common.ui.dialog;

import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.tree.DBNTree;
import com.dbn.common.ui.tree.DBNTreeModel;
import com.dbn.common.ui.tree.DBNTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;

public class ExceptionTreeDialog extends DialogWrapper {

    private final Exception root;
    private JPanel mainPanel;
    private JTree tree;

    public ExceptionTreeDialog(Exception root) {
        super(false);
        this.root = root;
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return this.mainPanel;
    }

    protected void init() {
        this.mainPanel = new JPanel();
        BoxLayout layout = new BoxLayout(mainPanel, BoxLayout.Y_AXIS);
        this.mainPanel.setLayout(layout);
        this.tree = new JTree();
        ExceptionTreeNode root = new ExceptionTreeNode(this.root);
        ExceptionTreeModel treeModel = new ExceptionTreeModel(root);
        this.tree.setModel(treeModel);

        JBScrollPane scrollPane = new JBScrollPane(tree);
        scrollPane.setPreferredSize(new Dimension(800, 600));

        this.mainPanel.add(scrollPane);


        this.mainPanel.layout();

        super.init();
    }

    private static class ExceptionTreeNode extends DefaultMutableTreeNode {

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
    private static class ExceptionTreeModel extends DefaultTreeModel {

        public ExceptionTreeModel(TreeNode root) {
            super(root, true);
        }

        @Override
        public Object getChild(Object parent, int index) {
            return super.getChild(parent, index);
        }
    }
}

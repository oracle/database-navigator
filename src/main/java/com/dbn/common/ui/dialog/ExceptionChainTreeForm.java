package com.dbn.common.ui.dialog;

import com.dbn.common.exception.Exceptions;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.tree.DBNTree;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreeModel;
import java.awt.*;

public class ExceptionChainTreeForm extends DBNFormBase {
    private final Exceptions.ExceptionCauseChain chain;
    private final String message;
    private final Object contextObject;
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JTextPane messagePane;
    private DBNTree exceptionTree;

    public ExceptionChainTreeForm(ExceptionTreeDialog dialog, @Nullable Object contextObject, String message, Exceptions.ExceptionCauseChain chain) {
        super(dialog);
        this.message = message;
        this.contextObject = contextObject;
        this.chain = chain;
        init();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return this.exceptionTree;
    }

    protected void init() {
        if (contextObject == null) {
            headerPanel.setVisible(false);
        } else {
            DBNHeaderForm headerForm = new DBNHeaderForm(this, contextObject);
            headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);
        }
        messagePane.setText(this.message);
        BoxLayout layout = new BoxLayout(mainPanel, BoxLayout.Y_AXIS);
        this.mainPanel.setLayout(layout);
        @NotNull TreeModel treeModel = chain.adaptTo(TreeModel.class);
        this.exceptionTree = new DBNTree(this, treeModel);

        JBScrollPane scrollPane = new JBScrollPane(exceptionTree);
        scrollPane.setPreferredSize(new Dimension(800, 600));

        this.mainPanel.add(scrollPane);

        this.mainPanel.layout();
    }
}

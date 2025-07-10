package com.dbn.common.ui.dialog;

import com.dbn.common.exception.Exceptions;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.tree.DBNTree;
import com.dbn.common.ui.util.Accessibility;
import com.dbn.common.ui.util.Fonts;
import com.dbn.common.util.Dialogs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreeModel;
import java.util.Optional;

public class ExceptionChainTreeForm extends DBNFormBase {
    private final Exceptions.ExceptionCauseChain chain;
    private final String message;
    private final Object contextObject;
    private final String title;
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JTextArea messagePane;
    private JPanel treePanel;
    private JLabel treeLabel;
    private JTree exceptionTree;
    private JLabel iconLabel;

    public ExceptionChainTreeForm(ExceptionTreeDialog dialog, @Nullable Object contextObject, String title, String message, Exceptions.ExceptionCauseChain chain) {
        super(dialog);
        this.message = message;
        this.contextObject = contextObject;
        this.chain = chain;
        this.title = title;
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
        iconLabel.setIcon(Icons.DIALOG_ERROR);
        iconLabel.setText(title);
        iconLabel.setFont(Fonts.regularBold(2));

        messagePane.setText(this.message);
        messagePane.setColumns(40);

        this.treeLabel.setLabelFor(exceptionTree);
        expandTreePane(false);

        this.mainPanel.doLayout();
    }

    public void expandTreePane(boolean expand) {
        treePanel.setVisible(expand);
        this.mainPanel.invalidate();
        this.mainPanel.doLayout();
        Optional.ofNullable(getParentDialog()).
                ifPresent(d -> Dialogs.resizeToFitContent(this.mainPanel));
    }

    protected void initAccessibility() {
        Accessibility.setAccessibleDescription(messagePane, "Message");
    }

    private void createUIComponents() {
        @NotNull TreeModel treeModel = chain.adaptTo(TreeModel.class);
        this.exceptionTree = new DBNTree(this, treeModel);
    }
}

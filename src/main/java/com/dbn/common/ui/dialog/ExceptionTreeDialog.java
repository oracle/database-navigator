package com.dbn.common.ui.dialog;

import com.dbn.common.clipboard.Clipboard;
import com.dbn.common.exception.Exceptions;
import com.dbn.common.util.Unsafe;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ExceptionTreeDialog extends DBNDialog<ExceptionChainTreeForm> {
    private final Object contextObject;
    private final String message;
    private final Exceptions.ExceptionCauseChain chain;
    private Action detailAction;
    private boolean expanded;
    private Action copyToClipboardAction;

    public ExceptionTreeDialog(Project project,
                               String title,
                               String message,
                               @NotNull Throwable exception,
                               @Nullable Object contextObject) {
        super(project, title, false);
        this.chain = new Exceptions.ExceptionCauseChain(exception);
        this.contextObject = contextObject;
        this.message = message;

        init();
    }

    protected void createCustomActions() {
        this.detailAction =  createShowDetailsAction();
        this.copyToClipboardAction = createCopyToClipboardAction();
    }

    @Override
    protected void init() {
        createCustomActions();
        super.init();
    }

    @Override
    protected @NotNull ExceptionChainTreeForm createForm() {
        return new ExceptionChainTreeForm(this, contextObject, getTitle(), message, chain);
    }


    @Override
    protected final Action @NotNull [] createActions() {
        return new Action[]{
                getCopyToClipboard(),
                getDetailAction(),
                getOKAction()
        };
    }

    protected Action getCopyToClipboard() {
        return this.copyToClipboardAction;
    }

    protected Action createCopyToClipboardAction() {
        return new DialogWrapperAction("Copy To Clipboard") {
            @Override
            protected void doAction(ActionEvent actionEvent) {
                StringBuilder content = new StringBuilder();
                content.append(message);
                content.append(chain.toString());
                Unsafe.logged(() -> Clipboard.copyTextToClipboard(content.toString()));
            }
        };
    }
    protected Action getDetailAction() {
        return this.detailAction;
    }
    protected Action createShowDetailsAction() {
        return new DialogWrapperAction( "Show Details") {
            @Override
            protected void doAction(ActionEvent actionEvent) {
                expanded = !expanded;
                getDetailAction().putValue("Name", expanded ? "Hide Details" : "Show Details");
                getForm().expandTreePane(expanded);
            }
        };
    }

}

package com.dbn.common.ui.dialog;

import com.dbn.common.exception.Exceptions;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ExceptionTreeDialog extends DBNDialog<ExceptionChainTreeForm> {

    private final Throwable exception;
    private final Object contextObject;
    private final String message;

    public ExceptionTreeDialog(Project project,
                               String title,
                               String message,
                               @NotNull Throwable exception,
                               @Nullable Object contextObject) {
        super(project, title, false);
        this.exception = exception;
        this.contextObject = contextObject;
        this.message = message;
        init();
    }

    @Override
    protected @NotNull ExceptionChainTreeForm createForm() {
        return new ExceptionChainTreeForm(this, contextObject, message, new Exceptions.ExceptionCauseChain(exception));
    }
    @Override
    protected final Action[] createActions() {
        return new Action[]{
                getOKAction()
        };
    }
}

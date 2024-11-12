package com.dbn.common.ui.dialog;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.project.ProjectRef;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.util.Listeners;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Titles;
import com.dbn.diagnostics.Diagnostics;
import com.dbn.nls.NlsSupport;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static com.dbn.common.util.Unsafe.cast;

public abstract class DBNDialog<F extends DBNForm> extends DialogWrapper implements DBNComponent, NlsSupport {
    private F form;
    private final ProjectRef project;
    private final Listeners<DBNDialogListener> listeners = Listeners.create(getDisposable());

    private @Getter boolean rememberSelection;
    private @Getter Dimension defaultSize;

    protected DBNDialog(Project project, String title, boolean canBeParent) {
        super(project, canBeParent);
        this.project = ProjectRef.of(project);
        setTitle(Titles.signed(title));
        getHelpAction().setEnabled(false);
    }

    @Override
    protected void init() {
        if (defaultSize != null) {
            setSize(
                (int) defaultSize.getWidth(),
                (int) defaultSize.getHeight());
        }
        super.init();
    }

    public void setDialogCallback(@Nullable Dialogs.DialogCallback<?> callback) {
        if (callback == null) return;

        Window window = getPeer().getWindow();
        if (window == null) return;

        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                callback.call(cast(DBNDialog.this), getExitCode());
            }
        });
    }

    public void addDialogListener(DBNDialogListener listener) {
        listeners.add(listener);
    }

    public void setDefaultSize(int width, int height) {
        this.defaultSize = new Dimension(width, height);
    }

    @NotNull
    public final F getForm() {
        if (form == null && !isDisposed()) {
            form = createForm();
        }
        return Failsafe.nn(form);
    }

    @Override
    public final void show() {
        super.show();
        listeners.notify(l -> l.onAction(DBNDialogListener.Action.OPEN));
    }

    @Override
    @NotNull
    protected final JComponent createCenterPanel() {
        return getComponent();
    }

    @NotNull
    protected abstract F createForm();

    @Nullable
    public final <T extends Disposable> T getParentComponent() {
        return null;
    }

    @NotNull
    @Override
    public final JComponent getComponent() {
        return getForm().getComponent();
    }

    @Override
    protected String getDimensionServiceKey() {
        return Diagnostics.isDialogSizingReset() ? null : "DBNavigator." + getClass().getSimpleName();
    }

    protected static void renameAction(@NotNull Action action, String name) {
        action.putValue(Action.NAME, name);
    }

    protected static void makeDefaultAction(@NotNull Action action) {
        action.putValue(DEFAULT_ACTION, Boolean.TRUE);
    }

    protected static void makeFocusAction(@NotNull Action action) {
        action.putValue(FOCUSED_ACTION, Boolean.TRUE);
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        if (isDisposed()) return null;

        return Commons.coalesce(
                () -> getForm().getPreferredFocusedComponent(),
                () -> super.getPreferredFocusedComponent(),
                () -> getButton(getOKAction()),
                () -> getButton(getCancelAction()));
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
    }

    @Override
    protected void doHelpAction() {
        super.doHelpAction();
    }

    @Override
    @NotNull
    public Project getProject() {
        return project.ensure();
    }

    public void registerRememberSelectionCheckBox(JCheckBox rememberSelectionCheckBox) {
        rememberSelectionCheckBox.addActionListener(e -> rememberSelection = rememberSelectionCheckBox.isSelected());
    }


    @Getter
    @Setter
    private boolean disposed;

    @Override
    public final void dispose() {
        if (disposed) return;
        disposed = true;

        listeners.notify(l -> l.onAction(DBNDialogListener.Action.CLOSE));
        super.dispose();
        Disposer.dispose(form);
        disposeInner();
        //nullify();
    }

    @Override
    public void disposeInner() {

    }
}

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

package com.dbn.common.ui.dialog;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.project.ProjectRef;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Titles;
import com.dbn.diagnostics.Diagnostics;
import com.dbn.nls.NlsSupport;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.AppIcon;
import com.intellij.util.ui.JBDimension;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import static com.dbn.common.ui.dialog.DBNDialogMonitor.registerDialog;
import static com.dbn.common.ui.dialog.DBNDialogMonitor.releaseDialog;
import static com.dbn.common.util.Classes.simpleClassName;
import static com.dbn.common.util.Lists.firstElement;
import static com.dbn.common.util.Unsafe.cast;

public abstract class DBNDialog<F extends DBNForm> extends DialogWrapper implements DBNComponent, NlsSupport {
    private F form;
    private final ProjectRef project;

    private @Getter boolean rememberSelection;
    private @Getter Dimension defaultSize;

    protected DBNDialog(@Nullable Project project, String title, boolean canBeParent) {
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

    /**
     * Validates the input provided in the specified component and updates the validation state
     * of the form. This method also determines whether the main action button should be enabled
     * based on the overall validation results.
     *
     * @param component the UI component to validate; typically a part of the dialog form
     */
    public void validateInput(JComponent component) {
        F form = getForm();
        List<ValidationInfo> validationInfos = form.validate(component);

        setErrorInfoAll(validationInfos);

        // do validation for all fields to decide whether to enable main button
        validationInfos = form.validate();
        setOKActionEnabled(validationInfos.isEmpty());
    }


    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        F form = getForm();
        List<ValidationInfo> validationInfos = form.validate();
        return firstElement(validationInfos);
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

    public void setDefaultSize(int width, int height) {
        this.defaultSize = new JBDimension(width, height);
    }

    @NotNull
    public final F getForm() {
        if (form == null && !isDisposed()) {
            form = createForm();
        }
        return Failsafe.nn(form);
    }

    @Override
    public void show() {
        AppIcon.getInstance().requestAttention(getProject(), true);
        registerDialog(this);
        super.show();
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
        return Diagnostics.isDialogSizingReset() ? null : "DBNavigator." + simpleClassName(this);
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

        releaseDialog(this);
        super.dispose();
        Disposer.dispose(form);
        disposeInner();
        //nullify();
    }

    @Override
    public void disposeInner() {

    }
}

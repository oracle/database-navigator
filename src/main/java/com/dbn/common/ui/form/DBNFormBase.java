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

package com.dbn.common.ui.form;

import com.dbn.common.action.DataProviders;
import com.dbn.common.dispose.ComponentDisposer;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.environment.options.EnvironmentSettings;
import com.dbn.common.event.ApplicationEvents;
import com.dbn.common.latent.Latent;
import com.dbn.common.notification.NotificationSupport;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.component.DBNComponentBase;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.util.ClientProperty;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.options.general.GeneralProjectSettings;
import com.intellij.ide.DataManager;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.ide.ui.UISettingsListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.experimental.Delegate;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.text.JTextComponent;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Arrays;

import static com.dbn.common.ui.alignment.FieldAligner.alignFormFields;
import static com.dbn.common.ui.form.DBNFormBinding.bindForm;
import static com.dbn.common.ui.form.field.DBNFormFieldDisabler.disableFormField;
import static com.dbn.common.ui.form.field.DBNFormFieldDisabler.enableFormField;
import static com.dbn.common.ui.util.Accessibility.initComponentGroupsAccessibility;
import static com.dbn.common.ui.util.Accessibility.initCustomComponentAccessibility;
import static com.dbn.common.ui.util.ClientProperty.NON_DISABLEABLE;
import static com.dbn.common.ui.util.UserInterface.hasChildComponent;
import static com.dbn.common.ui.util.UserInterface.whenFirstShown;
import static com.dbn.common.util.Unsafe.cast;
import static com.intellij.util.ui.UIUtil.getScrollBarWidth;

public abstract class DBNFormBase
        extends DBNComponentBase
        implements DBNForm, NotificationSupport {

    @Getter
    private boolean initialized;

    private final Latent<DBNFormFieldAdapter> fieldAdapter = Latent.basic(() -> DBNFormFieldAdapter.create(this));
    private final Latent<Boolean> hasScrollBars = Latent.basic(() -> hasChildComponent(getMainComponent(), c -> c instanceof JScrollPane));

    public DBNFormBase(@Nullable Disposable parent) {
        super(parent);
    }

    public DBNFormBase(@Nullable Disposable parent, @Nullable Project project) {
        super(parent, project);
    }

    protected DBNFormFieldAdapter getFieldAdapter() {
        return fieldAdapter.get();
    }

    @NotNull
    @Override
    public final JComponent getComponent() {
        initialize();
        return getMainComponent();
    }

    @Delegate
    protected DBNFormValidator getFormValidator() {
        DBNDialog dialog = getParentDialog();
        return dialog == null ?
                DBNFormValidator.SURROGATE :
                dialog.getFormValidator();
    }

    public void focusPreferredComponent() {
        JComponent preferredFocusedComponent = getPreferredFocusedComponent();
        if (preferredFocusedComponent != null) {
            preferredFocusedComponent.requestFocus();
        }
    }

    /**
     * Passes on the runnable to the dispatch thread (Application.invokeLater) under full awareness of the component modality state
     * @param runnable the runnable to be sent to dispatch thread
     */
    protected void dispatch(Runnable runnable) {
        JComponent mainComponent = getMainComponent();
        if (initialized) {
            Dispatch.run(mainComponent, runnable);
        } else {
            whenFirstShown(mainComponent, () -> Dispatch.run(mainComponent, runnable));
        }
    }

    /**
     * Allows invoking a task when the form is first shown.
     * Useful for delaying the execution of a given task when the modality state of the form is clarified.
     * Can also be helpful when component size decisions have to be taken based on surrounding container size.
     *
     * @param runnable the task to execute when the form is shown
     */
    protected final void whenShown(Runnable runnable) {
        whenFirstShown(getMainComponent(), runnable);
    }

    protected final void whenSettingsChange(Runnable runnable) {
        UISettingsListener uiSettingsListener = s -> runnable.run();
        ApplicationEvents.subscribe(this, UISettingsListener.TOPIC, uiSettingsListener);
    }

    private void initialize() {
        if (isDisposed()) return;
        if (initialized) return;
        initialized = true;


        initValidation();
        initStatePersistence();
        initFormAccessibility();
        initFieldAvailability();
        initFieldAlignment();
        initEventListeners();

        JComponent mainComponent = getMainComponent();
        bindForm(mainComponent, this);

        DataProviders.register(mainComponent, this);
        UserInterface.updateScrollPanes(mainComponent);
        UserInterface.updateTitledBorders(mainComponent);
        UserInterface.updateSplitPanes(mainComponent);
        UserInterface.updateTextPanes(mainComponent);
        adjustFormSize(mainComponent);

        ApplicationEvents.subscribe(this, LafManagerListener.TOPIC, source -> lookAndFeelChanged());
        updateFieldAvailability();
    }

    /**
     * Adjusts the size of the given form component to account for its content and any additional elements,
     * such as scrollbars, when it is displayed within a parent component (e.g., a dialog).
     * Validates and lays out the component to ensure its proper rendering.
     *
     * @param mainComponent the main component of the form whose size needs to be adjusted
     */
    private void adjustFormSize(JComponent mainComponent) {
        Disposable parentComponent = getParentComponent();
        if (parentComponent instanceof DBNDialog) {
            mainComponent.validate();

            boolean hasScrollBars = this.hasScrollBars.get();
            if (!hasScrollBars) return;

            // buffers to be added to the form size to hide scroll-bars unless absolutely necessary
            int buffer = getScrollBarWidth();

            Dimension dimension = mainComponent.getPreferredSize();
            dimension = new Dimension(dimension.width + buffer * 4, dimension.height + buffer * 2);
            mainComponent.setPreferredSize(dimension);
            mainComponent.revalidate();
            mainComponent.repaint();
        }
    }

    public void revalidateForm() {
        JComponent mainComponent = getMainComponent();
        mainComponent.revalidate();
        mainComponent.repaint();
    }

    private void initFormAccessibility() {
        JComponent mainComponent = getMainComponent();
        initAccessibility();
        initComponentGroupsAccessibility(mainComponent);
        initCustomComponentAccessibility(mainComponent);
        //...
    }

    @ApiStatus.OverrideOnly
    protected void initValidation() {}

    @ApiStatus.OverrideOnly
    protected void initAccessibility() {}

    @ApiStatus.OverrideOnly
    protected void initFieldAvailability() {}

    @ApiStatus.OverrideOnly
    protected void initEventListeners() {}

    protected void updateFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.updateFieldsVisibility();
        fieldAdapter.updateFieldsAvailability();
    }

    public void updateFieldAlignment() {
        alignFormFields(this);
    }

    protected void validateFormFields() {
        DBNDialog parentDialog = getParentDialog();
        if (parentDialog == null) return;

        parentDialog.validateInput(null);
    }


    protected void initFieldAlignment() {}

    protected FieldAlignerData getFieldAlignerData() {
        return ClientProperty.FIELD_ALIGNER_DATA.get(getComponent(), () -> new FieldAlignerData());
    }

    /**
     * Initializes the persistence mechanisms for the state of the form or component.
     * This method is intended to be overridden by subclasses to define custom state
     * persistence logic, such as saving and restoring UI state or settings.
     * <br>
     * It does not include any default implementation and should be implemented in
     * subclasses if state persistence is required.
     */
    @ApiStatus.OverrideOnly
    protected void initStatePersistence () {}

    @ApiStatus.OverrideOnly
    protected void lookAndFeelChanged() {}

    public void applyFormChanges() throws ConfigurationException {}

    public void resetFormChanges() {}

    protected void updateActionToolbars() {
        dispatch(() -> UserInterface.updateActionToolbars(getMainComponent()));
    }

    protected abstract JComponent getMainComponent();

    public EnvironmentSettings getEnvironmentSettings(Project project) {
        return GeneralProjectSettings.getInstance(project).getEnvironmentSettings();
    }

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        return null;
    }


    /**
     * Retrieves the parent dialog associated with the current form or component, if present.
     * The method attempts to determine the parent dialog by navigating the hierarchy of parent components.
     *
     * @param <D> the type of the parent dialog, extending from {@link DBNDialog}
     * @return the parent dialog instance if available; otherwise, returns {@code null}
     */
    @Override
    public <D extends DBNDialog> D getParentDialog() {
        Disposable parent = getParentComponent();
        if (parent instanceof DBNDialog) return cast(parent);
        if (parent instanceof DBNForm form) {
            return form.getParentDialog();
        }
        return null;
    }

    @Nullable
    @Override
    public final <F extends DBNForm> F getParentFrom(Class<F> formClass) {
        DBNComponent parent = getParentComponent();
        if (parent == null) return null;
        if (formClass.isAssignableFrom(parent.getClass())) return cast(parent);

        if (parent instanceof DBNForm parentForm) {
            return parentForm.getParentFrom(formClass);
        }
        return null;
    }


    @NotNull
    public final <F extends DBNForm> F ensureParentFrom(Class<F> formClass) {
        return Failsafe.nd(getParentFrom(formClass));
    }

    protected static Action createAction(
            @NotNull @Nls String name,
            @NotNull Runnable runnable) {
        return createAction(name, null, runnable);
    }

    protected static Action createAction(
            @NotNull @Nls String name,
            @Nullable String description,
            @NotNull Runnable runnable) {
        return new AbstractAction(name) {
            {
                putValue(Action.SHORT_DESCRIPTION, description);
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                runnable.run();
            }
        };
    }

    protected static Action[] createActions(Action ... actions) {
        return Arrays.stream(actions)
                .filter(value -> value != null)
                .toArray(l -> new Action[l]);

    }

    @Override
    public void disposeInner() {
        JComponent component = getComponent();
        DataManager.removeDataProvider(component);
        ComponentDisposer.dispose(component);
        nullify();
    }

    public void freezeForm() {
        UserInterface.visitRecursively(getComponent(), c -> disable(c));
    }

    public void unfreezeForm() {
        UserInterface.visitRecursively(getComponent(), c -> enable(c));
    }

    private void disable(JComponent c) {
        if (NON_DISABLEABLE.is(c)) return;

        if (c instanceof JTextComponent textComponent) {
            if (textComponent instanceof JEditorPane || textComponent instanceof JTextArea) {
                if (!textComponent.isEditable()) return;
            }
        }

        if (c instanceof AbstractButton ||
                c instanceof JTextComponent ||
                c instanceof JComboBox ||
                c instanceof ActionToolbar ||
                c instanceof JList ||
                c instanceof JTable ||
                c instanceof JLabel) {

            disableFormField(c, "READONLY_FORM");
        }
    }

    private void enable(JComponent c) {
        enableFormField(c, "READONLY_FORM");
    }
}

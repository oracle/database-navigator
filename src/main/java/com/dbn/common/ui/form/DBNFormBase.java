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
import com.dbn.common.environment.options.EnvironmentSettings;
import com.dbn.common.event.ApplicationEvents;
import com.dbn.common.latent.Latent;
import com.dbn.common.notification.NotificationSupport;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.component.DBNComponentBase;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.options.general.GeneralProjectSettings;
import com.intellij.ide.DataManager;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.text.JTextComponent;
import java.awt.Dimension;
import java.util.List;
import java.util.Set;

import static com.dbn.common.ui.util.Accessibility.initComponentGroupsAccessibility;
import static com.dbn.common.ui.util.Accessibility.initCustomComponentAccessibility;
import static com.dbn.common.ui.util.UserInterface.findChildComponent;
import static com.dbn.common.ui.util.UserInterface.hasChildComponent;
import static com.dbn.common.ui.util.UserInterface.isFocusableComponent;
import static com.dbn.common.ui.util.UserInterface.whenFirstShown;
import static com.dbn.common.util.Unsafe.cast;
import static com.intellij.util.ui.UIUtil.getScrollBarWidth;

public abstract class DBNFormBase
        extends DBNComponentBase
        implements DBNForm, NotificationSupport {

    private boolean initialized;
    private final Set<JComponent> enabled = ContainerUtil.createWeakSet();
    private final Latent<DBNFormFieldAdapter> fieldAdapter = Latent.basic(() -> DBNFormFieldAdapter.create(this));
    private final Latent<Boolean> hasScrollBars = Latent.basic(() -> hasChildComponent(getMainComponent(), c -> c instanceof JScrollPane));

    protected final DBNFormValidator formValidator = new DBNFormValidatorImpl(this);

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

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return findChildComponent(getMainComponent(), c -> isFocusableComponent(c));
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
        Dispatch.run(getMainComponent(), runnable);
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

    private void initialize() {
        if (isDisposed()) return;
        if (initialized) return;
        initialized = true;


        initValidation();
        initStatePersistence();
        initFormAccessibility();

        JComponent mainComponent = getMainComponent();
        DataProviders.register(mainComponent, this);
        UserInterface.updateScrollPaneBorders(mainComponent);
        UserInterface.updateTitledBorders(mainComponent);
        UserInterface.updateSplitPanes(mainComponent);
        adjustFormSize(mainComponent);

        ApplicationEvents.subscribe(this, LafManagerListener.TOPIC, source -> lookAndFeelChanged());
    }

    /**
     * Adjusts the size of the given form component to account for its content and any additional elements,
     * such as scrollbars, when it is displayed within a parent component (e.g., a dialog).
     * Validates and lays out the component to ensure its proper rendering.
     *
     * @param mainComponent the main component of the form whose size needs to be adjusted
     */
    private void adjustFormSize(JComponent mainComponent) {
        mainComponent.doLayout();
        mainComponent.validate();

        Disposable parentComponent = getParentComponent();
        if (parentComponent instanceof DBNDialog) {

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

    private void initFormAccessibility() {
        JComponent mainComponent = getMainComponent();
        initAccessibility();
        initComponentGroupsAccessibility(mainComponent);
        initCustomComponentAccessibility(mainComponent);
        //...
    }



    @Override
    public final List<ValidationInfo> validate(JComponent... components) {
        return formValidator.validateForm(components);
    }

    @ApiStatus.OverrideOnly
    protected void initValidation() {}

    @ApiStatus.OverrideOnly
    protected void initAccessibility() {}

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
        if (parent instanceof DBNForm) {
            DBNForm form = (DBNForm) parent;
            return form.getParentDialog();
        }
        return null;
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
        if (c instanceof AbstractButton ||
                c instanceof JTextComponent ||
                c instanceof ActionToolbar ||
                c instanceof JList ||
                c instanceof JTable ||
                c instanceof JLabel) {

            if (c.isEnabled()) {
                enabled.add(c);
                c.setEnabled(false);
            }
        }
    }

    private void enable(JComponent c) {
        if (enabled.remove(c)) {
            c.setEnabled(true);
        }
    }
}

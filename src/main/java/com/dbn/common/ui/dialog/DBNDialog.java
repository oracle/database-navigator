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

import com.dbn.common.action.UserDataKeys;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.project.ProjectRef;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNFormValidator;
import com.dbn.common.ui.form.DBNFormValidatorImpl;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Titles;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionRef;
import com.dbn.diagnostics.Diagnostics;
import com.dbn.help.HelpTopic;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.OptionAction;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.NlsContexts.Button;
import com.intellij.openapi.util.NlsContexts.DialogTitle;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.ui.AppIcon;
import com.intellij.ui.components.JBOptionButton;
import com.intellij.util.Consumer;
import com.intellij.util.ui.JBDimension;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.dbn.common.action.UserDataKeys.CONNECTION_REF;
import static com.dbn.common.action.UserDataKeys.PROJECT_REF;
import static com.dbn.common.data.Data.asBooleanPrimitive;
import static com.dbn.common.dispose.Failsafe.guarded;
import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.ui.dialog.DBNDialogMonitor.registerDialog;
import static com.dbn.common.ui.dialog.DBNDialogMonitor.releaseDialog;
import static com.dbn.common.ui.util.Buttons.installMousePressFocus;
import static com.dbn.common.ui.util.UserInterface.findTopLeftmostFocusComponent;
import static com.dbn.common.ui.util.UserInterface.whenFirstShown;
import static com.dbn.common.util.Classes.simpleClassName;
import static com.dbn.common.util.Lists.filter;
import static com.dbn.common.util.Lists.firstElement;
import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.nls.NlsResources.txt;

@Getter
@Setter
public abstract class DBNDialog<F extends DBNForm> extends DialogWrapper implements DBNComponent, UserDataHolder {
    public static final String HIDDEN = "HIDDEN";
    public static final String PARENT = "PARENT";

    private F form;
    private boolean rememberSelection;
    private boolean initialized;
    private boolean autoSize;
    private Dimension defaultSize;
    private final DBNFormValidator formValidator = new DBNFormValidatorImpl(this);
    private final UserDataHolder userDataHolder = new UserDataHolderBase();

    protected DBNDialog(@NotNull ConnectionHandler connection, @DialogTitle String title, boolean canBeParent) {
        this(connection.getProject(), title, canBeParent);
        putUserData(UserDataKeys.CONNECTION_REF, ConnectionRef.of(connection));
    }

    protected DBNDialog(@Nullable Project project, @DialogTitle String title, boolean canBeParent) {
        super(project, canBeParent);
        putUserData(PROJECT_REF, ProjectRef.of(project));
        setTitle(Titles.signed(title));
    }

    @Override
    protected void init() {
        if (initialized) return;
        initialized = true;

        if (defaultSize != null) {
            setSize(
                (int) defaultSize.getWidth(),
                (int) defaultSize.getHeight());
        }
        super.init();
        initActions();
        initFocusComponent();
        validateInput(null);
    }

    private void initFocusComponent() {
        JComponent focusComponent = form.getPreferredFocusedComponent();
        if (focusComponent != null) return; // explicitly defined focus component

        JComponent container = getComponent();
        whenFirstShown(container, () -> {
            JComponent component = findTopLeftmostFocusComponent(container);
            if (component != null) component.requestFocusInWindow(FocusEvent.Cause.ACTIVATION);
        });
    }

    private void initActions() {
        for (Action action : getButtonMap().keySet()) {
            boolean hidden = asBooleanPrimitive(action.getValue(HIDDEN));
            if (hidden) hideAction(action);
        }
    }

    /**
     * Validates the input provided in the specified component and updates the validation state
     * of the form. This method also determines whether the main action button should be enabled
     * based on the overall validation results.
     *
     * @param component the UI component to validate; typically a part of the dialog form
     */
    public void validateInput(@Nullable JComponent component) {
        if (isDisposed()) return;
        if (!formValidator.hasValidators()) return;
        if (!formValidator.hasValidators(component)) return;

        List<ValidationInfo> validationInfos = component == null ?
                buildValidationInfos() :
                buildValidationInfos(component);

        List<ValidationInfo> notifiedValidationInfos = filter(validationInfos, v -> formValidator.isVisitedField(v.component));
        setErrorInfoAll(notifiedValidationInfos);

        // do validation for all fields (if not already the case) to decide whether to enable main button
        validationInfos = component == null ? validationInfos : buildValidationInfos();
        setOKActionEnabled(validationInfos.isEmpty());

        // revalidate the ui if dialog errors are listed in the dialog footer
        Window window = getWindow();
        window.revalidate();
        window.repaint();
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        List<ValidationInfo> validationInfos = buildValidationInfos();
        return firstElement(validationInfos);
    }

    private List<ValidationInfo> buildValidationInfos(JComponent ... components) {
        DBNFormValidatorImpl formValidator = (DBNFormValidatorImpl) this.formValidator;
        return formValidator.buildValidationInfo(components);
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
        this.autoSize = false;
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
        Project project = guarded(null, () -> getProject());
        AppIcon.getInstance().requestAttention(project, true);
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


    @Override
    protected final @NonNls @Nullable String getHelpId() {
        HelpTopic helpTopic = getHelpTopic();
        return helpTopic == null ? null : helpTopic.asHelpTopicId();
    }

    protected HelpTopic getHelpTopic() {
        return null;
    }

    public void resetFormChanges() {
        getForm().resetFormChanges();
    }

    public void applyFormChanges() throws ConfigurationException {
        getForm().applyFormChanges();
    }

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
        return autoSize || Diagnostics.isDialogSizingReset() ? null : "DBNavigator." + simpleClassName(this);
    }

    protected static Action createAction(@NotNull @Button String name, @NotNull Runnable runnable) {
        return new AbstractAction(name) {
            @Override
            public void actionPerformed(ActionEvent e) {
                runnable.run();
            }
        };
    }

    protected static Action createAction(@NotNull @Button String name, @NotNull Consumer<JButton> consumer) {
        return new AbstractAction(name) {
            @Override
            public void actionPerformed(ActionEvent e) {
                consumer.consume(e.getSource() instanceof JButton ? (JButton) e.getSource() : null);
            }
        };
    }

    protected static OptionAction createCompositeAction(Action ... actions) {
        return new DBNCompositeAction(actions);
    }

    protected static Action[] actions(Action ... actions) {
        return Arrays.stream(actions)
                .filter(value -> value != null)
                .toArray(l -> new Action[l]);
    }

    protected static void renameAction(@NotNull Action action, @Button String name) {
        action.putValue(Action.NAME, name);
    }

    protected void showAction(@NotNull Action action) {
        makeActionVisible(action, true);
    }

    protected void hideAction(@NotNull Action action) {
        makeActionVisible(action, false);
    }

    @Override
    protected JButton createJButtonForAction(Action action) {
        JButton button = super.createJButtonForAction(action);
        if (button instanceof JBOptionButton optionButton) {
            installMousePressFocus(optionButton);
        }

        return button;
    }

    protected void makeActionVisible(@NotNull Action action, boolean visible) {
        OptionAction parentAction = (OptionAction) action.getValue(PARENT);
        if (parentAction != null) {
            makeActionOptionVisible(parentAction, action, visible);
            return;
        }

        action.putValue(HIDDEN, !visible);

        JButton button = getButton(action);
        if (button == null) return;

        button.setVisible(visible);
    }

    private void makeActionOptionVisible(@NotNull OptionAction action, @NotNull Action option, boolean visible) {
        JBOptionButton button = (JBOptionButton) getButton(action);
        if (button == null) return;

        List<Action> visibleOptions = new ArrayList<>();
        Action[] actionOptions = action.getOptions();
        Set<Action> buttonOptions = Set.of(button.getOptions());
        for (Action actionOption : actionOptions) {
            if (actionOption == option) {
                if (visible) {
                    visibleOptions.add(actionOption);
                }
            } else {
                if (buttonOptions.contains(actionOption)) {
                    visibleOptions.add(actionOption);
                }
            }
        }
        button.setOptions(visibleOptions.toArray(new Action[0]));

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

    @NotNull
    @Override
    protected final Action[] createActions() {
        initializeDefaultActionLabels();
        Action[] actions = initializeActions();
        if (getHelpId() == null) return actions;

        Action[] allActions = new Action[actions.length + 1];
        System.arraycopy(actions, 0, allActions, 0, actions.length);
        allActions[actions.length] = getHelpAction();
        return allActions;
    }

    private void initializeDefaultActionLabels() {
        renameAction(getOKAction(), txt("msg.shared.button.OK"));
        renameAction(getCancelAction(), txt("msg.shared.button.Cancel"));
        renameAction(getHelpAction(), txt("msg.shared.button.Help"));
    }

    protected abstract Action[] initializeActions();

    public boolean isCancelButton(JButton button) {
        if (button == null) return false;
        Action cancelAction = getCancelAction();
        Map<Action, JButton> buttonMap = getButtonMap();
        JButton cancelButton = buttonMap.get(cancelAction);
        return Objects.equals(button, cancelButton);
    }

    @Override
    @NotNull
    public Project getProject() {
        ProjectRef project = getUserData(PROJECT_REF);
        return ProjectRef.ensure(project);
    }

    public ConnectionId getConnectionId() {
        ConnectionHandler connection = getConnection();
        return connection == null ? null : connection.getConnectionId();
    }

    @Nullable
    public ConnectionHandler getConnection() {
        ConnectionRef connection = getUserData(CONNECTION_REF);
        return ConnectionRef.get(connection);
    }

    public ConnectionHandler ensureConnection() {
        return nd(getConnection());
    }

    public void setConnection(ConnectionHandler connection) {
        putUserData(CONNECTION_REF, ConnectionRef.of(connection));
    }

    @Delegate
    public UserDataHolder getUserDataHolder() {
        return userDataHolder;
    }

    public void registerRememberSelectionCheckBox(JCheckBox rememberSelectionCheckBox) {
        rememberSelectionCheckBox.addActionListener(e -> rememberSelection = rememberSelectionCheckBox.isSelected());
    }

    /**
     * Passes on the runnable to the dispatch thread (Application.invokeAndWait) under full awareness of the component modality state
     * @param runnable the runnable to be sent to dispatch thread
     */
    protected void dispatch(Runnable runnable) {
        Dispatch.execute(getComponent(), runnable);
    }

    protected boolean isRootDialog() {
        return getOwner() instanceof IdeFrame;
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

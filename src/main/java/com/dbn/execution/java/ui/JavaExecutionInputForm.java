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

package com.dbn.execution.java.ui;

import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.ui.panel.DBNCollapsiblePanel;
import com.dbn.common.ui.util.ComponentAligner;
import com.dbn.common.ui.util.Listeners;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.execution.common.ui.ExecutionOptionsForm;
import com.dbn.execution.java.JavaExecutionInput;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBJavaParameter;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.ui.DocumentAdapter;
import com.intellij.util.ui.AsyncProcessIcon;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.util.List;

import static com.dbn.common.ui.util.ComponentAligner.alignFormComponents;
import static java.util.Collections.emptyList;

public class JavaExecutionInputForm extends DBNFormBase implements ComponentAligner.Container {
    private JPanel mainPanel;
    private JPanel argumentsPanel;
    private JPanel headerPanel;
    private JLabel emptyParamsLabel;
    private JLabel debuggerVersionLabel;
    private JPanel versionPanel;
    private JLabel debuggerTypeLabel;
    private JPanel optionsPanel;
    private JPanel loadingParamsPanel;
    private JPanel loadingArgumentsIconPanel;
    private DBNScrollPane argumentsScrollPane;

    private final List<JavaExecutionInputParameterForm> parameterForms = DisposableContainers.list(this);
    private final Listeners<ChangeListener> changeListeners = Listeners.create(this);
    private ExecutionOptionsForm executionOptionsForm;

    @Getter @Setter
    private JavaExecutionInput executionInput;
    private final DBDebuggerType debuggerType;

    public JavaExecutionInputForm(
            DBNComponent parentComponent,
            @NotNull JavaExecutionInput executionInput,
            @NotNull DBDebuggerType debuggerType,
            boolean showHeader) {

        super(parentComponent);
        this.executionInput = executionInput;
        this.debuggerType = debuggerType;

        initDebuggerPanel();
        initOptionsPanel();
        initHeaderPanel(showHeader);
        initArgumentsPanel();
    }

    private void initDebuggerPanel() {
        if (debuggerType.isDebug()) {
            versionPanel.setVisible(true);
            debuggerTypeLabel.setText(debuggerType.name());
            debuggerVersionLabel.setText("...");
            Dispatch.async(
                    debuggerVersionLabel,
                    () -> executionInput.getDebuggerVersion(),
                    v -> debuggerVersionLabel.setText(v));
        } else {
            versionPanel.setVisible(false);
        }
    }

    private void initOptionsPanel() {
        executionOptionsForm = new ExecutionOptionsForm(this, executionInput, debuggerType);

        DBNCollapsiblePanel collapsiblePanel = new DBNCollapsiblePanel(this, executionOptionsForm, false);
        collapsiblePanel.setExpanded(executionInput.isContextExpanded());
        collapsiblePanel.addToggleListener(expanded -> executionInput.setContextExpanded(expanded));
        optionsPanel.add(collapsiblePanel.getComponent());
    }

    private void initHeaderPanel(boolean showHeader) {
        DBObjectRef<?> methodRef = executionInput.getMethodRef();
        if (showHeader) {
            DBNHeaderForm headerForm = new DBNHeaderForm(this, methodRef);
            headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);
        }
        headerPanel.setVisible(showHeader);
    }

    private boolean methodDetailsInitialized() {
        // method details are expected to be pre-initialized if opened in an isolated execution dialog
        // TODO find a clearer solution to this
        return getParentComponent() instanceof DBNDialog;
    }

    private void initArgumentsPanel() {
        if (methodDetailsInitialized()) {
            createArgumentsPanel();
            return;
        }

        emptyParamsLabel.setVisible(false);
        loadingParamsPanel.setVisible(true);
        loadingArgumentsIconPanel.add(new AsyncProcessIcon("Loading"), BorderLayout.CENTER);

        //lazy arguments initialization
        Dispatch.async(
                mainPanel,
                () -> getExecutionInput().initDatabaseElements(),
                () -> createArgumentsPanel());
    }

    private void createArgumentsPanel() {
        List<DBJavaParameter> arguments = getMethodArguments();
        checkDisposed();

        loadingParamsPanel.setVisible(false);
        loadingArgumentsIconPanel.removeAll();
        argumentsPanel.setLayout(new BoxLayout(argumentsPanel, BoxLayout.Y_AXIS));

        boolean noArguments = true;
        for (DBJavaParameter argument: arguments) {
            addArgumentPanel(argument);
            noArguments = false;
        }
        emptyParamsLabel.setVisible(noArguments);
        parameterForms.forEach(c -> c.addDocumentListener(documentListener));
        argumentsScrollPane.setPreferredSize(argumentsPanel.getPreferredSize());

        alignFormComponents(this);

    }


    private List<DBJavaParameter> getMethodArguments() {
        DBJavaMethod method = executionInput.getMethod();
        return method == null ? emptyList() : method.getParameters();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    int countFields() {
        return parameterForms.stream().mapToInt(f -> f.countFields()).sum();
    }

    private void addArgumentPanel(DBJavaParameter argument) {
        JavaExecutionInputParameterForm argumentComponent = new JavaExecutionInputParameterForm(this, argument);
        argumentsPanel.add(argumentComponent.getComponent());
        parameterForms.add(argumentComponent);
   }

    public void updateExecutionInput() {
        parameterForms.forEach(f -> f.updateExecutionInput());
        executionOptionsForm.updateExecutionInput();
    }

    public void addChangeListener(ChangeListener changeListener) {
        changeListeners.add(changeListener);
        executionOptionsForm.addChangeListener(changeListener);
    }

    private final DocumentListener documentListener = new DocumentAdapter() {
        @Override
        protected void textChanged(@NotNull DocumentEvent e) {
            notifyChangeListeners();
        }
    };

    private void notifyChangeListeners() {
        ChangeEvent changeEvent = new ChangeEvent(this);
        changeListeners.notify(l -> l.stateChanged(changeEvent));
    }

    /*********************************************************************
     *                      {@link ComponentAligner}                     *
     *********************************************************************/
    @Override
    public List<? extends ComponentAligner.Form> getAlignableForms() {
        return parameterForms;
    }
}

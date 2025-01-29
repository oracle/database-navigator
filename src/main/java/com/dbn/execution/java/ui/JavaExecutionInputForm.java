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
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.ui.panel.DBNCollapsiblePanel;
import com.dbn.common.ui.util.Listeners;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.execution.common.ui.ExecutionOptionsForm;
import com.dbn.execution.java.JavaExecutionInput;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaField;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBJavaParameter;
import com.dbn.object.DBOrderedObject;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.intellij.ui.DocumentAdapter;
import com.intellij.util.ui.AsyncProcessIcon;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class JavaExecutionInputForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel argumentsPanel;
    private JPanel headerPanel;
    private JLabel noArgumentsLabel;
    private JLabel debuggerVersionLabel;
    private JPanel versionPanel;
    private JLabel debuggerTypeLabel;
    private JPanel executionOptionsPanel;
    private JPanel argumentsContainerPanel;
    private JPanel loadingArgumentsPanel;
    private JPanel loadingArgumentsIconPanel;
    private DBNScrollPane argumentsScrollPane;

    private final List<JavaExecutionInputParameterForm> argumentForms = DisposableContainers.list(this);
    private final List<JavaExecutionInputFieldForm> complexArgumentForms = DisposableContainers.list(this);
    private final ExecutionOptionsForm executionOptionsForm;
    private final Listeners<ChangeListener> changeListeners = Listeners.create(this);

    @Getter @Setter
    private JavaExecutionInput executionInput;

    public JavaExecutionInputForm(
            DBNComponent parentComponent,
            @NotNull JavaExecutionInput executionInput,
            boolean showHeader,
            @NotNull DBDebuggerType debuggerType) {

        super(parentComponent);
        this.executionInput = executionInput;
        DBObjectRef<?> methodRef = executionInput.getMethodRef();

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


        executionOptionsForm = new ExecutionOptionsForm(this, executionInput, debuggerType);

        DBNCollapsiblePanel collapsiblePanel = new DBNCollapsiblePanel(this, executionOptionsForm, false);
        collapsiblePanel.setExpanded(executionInput.isContextExpanded());
        collapsiblePanel.addToggleListener(expanded -> executionInput.setContextExpanded(expanded));
        executionOptionsPanel.add(collapsiblePanel.getComponent());

        if (showHeader) {
            DBNHeaderForm headerForm = new DBNHeaderForm(this, methodRef);
            headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);
        }
        headerPanel.setVisible(showHeader);


        initArgumentsPanel();
    }

    private void initArgumentsPanel() {
        if (methodArgumentsLoaded()) {
            loadingArgumentsPanel.setVisible(false);
            List<DBJavaParameter> arguments = getMethodArguments();
            initArgumentsPanel(arguments);
            updatePreferredSize();
            return;
        }

        noArgumentsLabel.setVisible(false);
        loadingArgumentsPanel.setVisible(true);
        loadingArgumentsIconPanel.add(new AsyncProcessIcon("Loading"), BorderLayout.CENTER);

        //lazy load
        Dispatch.async(
                mainPanel,
                () -> getMethodArguments(),
                a -> initArgumentsPanel(a));
    }

    private void initArgumentsPanel(List<DBJavaParameter> arguments) {
        checkDisposed();

        loadingArgumentsPanel.setVisible(false);
        loadingArgumentsIconPanel.removeAll();
        argumentsPanel.setLayout(new BoxLayout(argumentsPanel, BoxLayout.Y_AXIS));
        int[] metrics = new int[]{0, 0, 0};

        boolean noArguments = true;
        for (DBJavaParameter argument: arguments) {
			if (argument.getParameterType().equals("class")) {
                List<DBJavaField> fields = argument.getParameterClass().getFields();
                String parentClass = getInnerClassName("", argument.getParameterClass());
                DBNCollapsiblePanel panel = addTreeArgumentPanel(fields, parentClass, argument.getName());

                argumentsPanel.add(panel.getMainComponent());
			} else {
				metrics = addArgumentPanel(argument, metrics);
			}
            noArguments = false;
        }
        noArgumentsLabel.setVisible(noArguments);

        for (JavaExecutionInputParameterForm component : argumentForms) {
            component.adjustMetrics(metrics);
            component.addDocumentListener(documentListener);
        }

        for (JavaExecutionInputFieldForm component : complexArgumentForms) {
            component.addDocumentListener(documentListener);
        }

        if (argumentForms.isEmpty() && complexArgumentForms.isEmpty()) {
            argumentsScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            Dimension preferredSize = argumentsScrollPane.getViewport().getView().getPreferredSize();
            preferredSize.setSize(preferredSize.getWidth(), preferredSize.getHeight() + 2);
            argumentsScrollPane.setMinimumSize(preferredSize);
        } else if(complexArgumentForms.isEmpty()) {
            int scrollUnitIncrement = argumentForms.get(0).getScrollUnitIncrement();
            Dimension minSize = new Dimension(-1, Math.min(argumentForms.size(), 10) * scrollUnitIncrement + 2);
            argumentsScrollPane.setMinimumSize(minSize);
            argumentsScrollPane.getVerticalScrollBar().setUnitIncrement(scrollUnitIncrement);
        } else {
            int scrollUnitIncrement = complexArgumentForms.get(0).getScrollUnitIncrement();
            Dimension minSize = new Dimension(-1, Math.min(complexArgumentForms.size(), 10) * scrollUnitIncrement + 2);
            argumentsScrollPane.setMinimumSize(minSize);
            argumentsScrollPane.getVerticalScrollBar().setUnitIncrement(scrollUnitIncrement);
        }

        updatePreferredSize();
    }

    private void updatePreferredSize() {
        Dimension preferredSize = mainPanel.getPreferredSize();
        int width = (int) preferredSize.getWidth() + 24;
        int height = (int) Math.min(preferredSize.getHeight(), 380);
        mainPanel.setPreferredSize(new Dimension(width, height));
        UserInterface.repaint(mainPanel);
    }

    private List<DBJavaParameter> getMethodArguments() {
        DBJavaMethod method = executionInput.getMethod();

        if( method == null ){
            return Collections.emptyList();
        } else {
            List<DBJavaParameter> parameters = new ArrayList<>(method.getParameters());
            parameters.sort(Comparator.comparingInt(DBOrderedObject::getPosition));
            for (DBJavaParameter parameter : parameters) {
                loadJavaFields(parameter.getParameterClass());
            }

            return parameters;
        }
    }

    private void loadJavaFields(DBJavaClass dbJavaClass){
        if(dbJavaClass == null) return;
        for (DBJavaField field : dbJavaClass.getFields()) {
            DBJavaClass innerClass = field.getFieldClass();
            if(innerClass != null){
                loadJavaFields(innerClass);
            }
        }
    }

    private boolean methodArgumentsLoaded() {
        if (!executionInput.getMethodRef().isLoaded()) return false;

        DBJavaMethod method = executionInput.getMethod();
        if (method == null) return false;

        DBObjectList<DBObject> argumentList = method.getChildObjectList(DBObjectType.JAVA_PARAMETER);
        return argumentList != null && argumentList.isLoaded();
    }


    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    private int[] addArgumentPanel(DBJavaParameter argument, int[] gridMetrics) {
        JavaExecutionInputParameterForm argumentComponent = new JavaExecutionInputParameterForm(this, argument);
        argumentsPanel.add(argumentComponent.getComponent());
        argumentForms.add(argumentComponent);
        return argumentComponent.getMetrics(gridMetrics);
   }

   private String getInnerClassName(String parentClass, DBJavaClass javaClass){
       String[] parts = javaClass.getName().split("/");
       if(parentClass.isEmpty()){
           return parts[parts.length - 1];
       }
       return parentClass + "." + parts[parts.length - 1];
   }

	private DBNCollapsiblePanel addTreeArgumentPanel(List<DBJavaField> arguments, String parentClass, String fieldName) {
        List<JavaExecutionInputFieldForm> list = DisposableContainers.list(this);
        DBNCollapsiblePanel cp;
        DBNCollapsiblePanel childPanel = null;
        String panelTitle = parentClass + " -> " + fieldName;
        for(DBJavaField argument : arguments) {
            if (argument.getType().equals("class") && argument.getFieldClass() != null) {
                String innerClass = getInnerClassName(parentClass, argument.getFieldClass());
                childPanel = addTreeArgumentPanel(argument.getFieldClass().getFields(), innerClass, argument.getName());
            } else {
                JavaExecutionInputFieldForm argumentComponent = new JavaExecutionInputFieldForm(this, argument);
                complexArgumentForms.add(argumentComponent);
                list.add(argumentComponent);
            }
        }
        JavaExecutionComplexInputForm cf = new JavaExecutionComplexInputForm(this, panelTitle, list);
        cp = new DBNCollapsiblePanel(this, cf,true);
        if(childPanel != null) {
            cp.addChild(childPanel);
        }
        return cp;
    }

    public void updateExecutionInput() {
        for (JavaExecutionInputParameterForm argumentComponent : argumentForms) {
            argumentComponent.updateExecutionInput();
        }
        for (JavaExecutionInputFieldForm argumentComponent : complexArgumentForms) {
            argumentComponent.updateExecutionInput();
        }
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
}

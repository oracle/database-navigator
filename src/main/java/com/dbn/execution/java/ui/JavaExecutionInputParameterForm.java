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
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.ComponentAligner;
import com.dbn.common.ui.util.TextFields;
import com.dbn.common.util.Commons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.data.editor.ui.ListPopupValuesProvider;
import com.dbn.data.editor.ui.TextFieldWithPopup;
import com.dbn.data.editor.ui.UserValueHolderImpl;
import com.dbn.execution.common.input.ExecutionVariable;
import com.dbn.execution.common.input.ExecutionVariableHistory;
import com.dbn.execution.java.JavaExecutionInput;
import com.dbn.execution.java.JavaExecutionManager;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaField;
import com.dbn.object.DBJavaParameter;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.ui.Layouts.verticalBoxLayout;
import static com.dbn.object.lookup.DBJavaNameCache.getCanonicalName;
import static java.util.Collections.emptyList;

public class JavaExecutionInputParameterForm extends DBNFormBase implements ComponentAligner.Form {
	private JPanel mainPanel;
	private JLabel parameterLabel;
	private JLabel parameterTypeLabel;
	private JPanel fieldsPanel;
	private JPanel inputFieldPanel;

	private JTextField inputTextField;
	private UserValueHolderImpl<String> userValueHolder;

	private final DBObjectRef<DBJavaParameter> parameter;
	private final List<JavaExecutionInputFieldForm> fieldForms = DisposableContainers.list(this);

	JavaExecutionInputParameterForm(DBNForm parentForm, DBJavaParameter parameter) {
		super(parentForm);

		this.parameter = DBObjectRef.of(parameter);
		parameterLabel.setText(parameter.getName());
		parameterLabel.setIcon(parameter.getIcon());
		parameterLabel.setBorder(Borders.insetBorder(4, 0, 4, 0));

		parameterTypeLabel.setForeground(UIUtil.getInactiveTextColor());
		if (parameter.isPlainValue()) {
			initPlainField();
		} else {
			initClassField();
		}
	}

	private void initPlainField() {
		DBJavaParameter parameter = getParameter();
		Project project = parameter.getProject();
		JavaExecutionInput executionInput = getExecutionInput();
		String value = executionInput.ensureInputValue(parameter.getName());

		TextFieldWithPopup<?> inputField = new TextFieldWithPopup<>(project);
		inputField.setPreferredSize(new Dimension(240, -1));

		inputField.createValuesListPopup(createValuesProvider(), parameter, true);
		String javaClassName = parameter.getJavaClassName();
		if (parameter.isClass()) {
			String className = getCanonicalName(javaClassName);
			parameterTypeLabel.setText(className);
			parameterTypeLabel.setIcon(/*parameter.getParameterClass().getIcon()*/Icons.DBO_JAVA_CLASS); // TODO performance issue (do not force loading the field class)
		} else {
			parameterTypeLabel.setText(javaClassName);
		}

		inputTextField = inputField.getTextField();
		inputTextField.setText(value);
		inputFieldPanel.add(inputField, BorderLayout.CENTER);

		inputTextField.setDisabledTextColor(inputTextField.getForeground());
		fieldsPanel.setVisible(false);
	}

	private void initClassField() {
		DBJavaClass javaClass = getParameter().getJavaClass();

		parameterTypeLabel.setText("");
		parameterTypeLabel.setVisible(false);

		JLabel classLabel = new JLabel(javaClass.getPresentableText());
		classLabel.setIcon(javaClass.getIcon());
		classLabel.setForeground(UIUtil.getInactiveTextColor());
		inputFieldPanel.add(classLabel, BorderLayout.WEST);


		verticalBoxLayout(fieldsPanel);
		List<DBJavaField> fields = javaClass.getFields();
		fields.forEach(f -> addFieldPanel(f));
	}

	@NotNull
	public JavaExecutionInputForm getParentForm() {
		return ensureParentComponent();
	}

	@NotNull
	private ListPopupValuesProvider createValuesProvider() {
		return new ListPopupValuesProvider() {
			@Override
			public String getName() {
				return "Value History";
			}

			@Override
			public List<String> getValues() {
				DBJavaParameter parameter = getParameter();
                if (parameter == null) return emptyList();

                JavaExecutionInput executionInput = getParentForm().getExecutionInput();
                return executionInput.getInputValueHistory(parameter.getName());

            }

			@Override
			public List<String> getSecondaryValues() {
				DBJavaParameter parameter = getParameter();
                if (parameter == null) return emptyList();

                ConnectionHandler connection = parameter.getConnection();
                ConnectionId connectionId = connection.getConnectionId();
                JavaExecutionManager executionManager = JavaExecutionManager.getInstance(parameter.getProject());
                ExecutionVariableHistory valuesHistory = executionManager.getInputValuesHistory();
                ExecutionVariable argumentValue = valuesHistory.getExecutionVariable(connectionId, parameter.getName(), false);
                if (argumentValue == null) return emptyList();

                List<String> cachedValues = new ArrayList<>(argumentValue.getValueHistory());
                cachedValues.removeAll(getValues());
                return cachedValues;
            }
		};
	}

	private void addFieldPanel(DBJavaField field) {
		JavaExecutionInputFieldForm argumentComponent = new JavaExecutionInputFieldForm(this, field);
		fieldsPanel.add(argumentComponent.getComponent());
		fieldForms.add(argumentComponent);
	}

	public DBJavaParameter getParameter() {
		return DBObjectRef.ensure(parameter);
	}

	public String getParameterName() {
		return parameter.getObjectName();
	}

	@NotNull
	@Override
	public JPanel getMainComponent() {
		return mainPanel;
	}

	public void updateExecutionInput() {
		DBJavaParameter parameter = getParameter();
        if (parameter == null) return;

        if (fieldForms.isEmpty()) {
			JavaExecutionInput executionInput = getParentForm().getExecutionInput();
			String parameterName = parameter.getName();
			if (userValueHolder != null) {
				String value = userValueHolder.getUserValue();
				executionInput.setInputValue(parameterName, value);
			} else {
				String value = Commons.nullIfEmpty(inputTextField == null ? null : inputTextField.getText());
				executionInput.setInputValue(parameterName, value);
			}
		} else {
			fieldForms.forEach(f -> f.updateExecutionInput());
		}
    }

	public void addDocumentListener(DocumentListener documentListener) {
		TextFields.addDocumentListener(inputTextField, documentListener);
	}

	public int getScrollUnitIncrement() {
		return (int) mainPanel.getPreferredSize().getHeight();
	}

	@NotNull
	JavaExecutionInput getExecutionInput() {
		JavaExecutionInputForm executionInputForm = getParentFrom(JavaExecutionInputForm.class);
		return nd(executionInputForm).getExecutionInput();
	}

	/*********************************************************************
	 *                      {@link ComponentAligner}                     *
	 *********************************************************************/

	@Override
	public Component[] getAlignableComponents() {
		return new Component[] {parameterLabel, inputFieldPanel, parameterTypeLabel};
	}

	@Override
	public List<? extends ComponentAligner.Form> getAlignableForms() {
		return fieldForms;
	}

	public int countFields() {
		return 1 + fieldForms.stream().mapToInt(f -> f.countFields()).sum();
	}
}

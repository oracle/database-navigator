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

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.TextFields;
import com.dbn.common.util.Commons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.data.editor.ui.ListPopupValuesProvider;
import com.dbn.data.editor.ui.TextFieldWithPopup;
import com.dbn.data.editor.ui.UserValueHolderImpl;
import com.dbn.execution.java.JavaExecutionInput;
import com.dbn.execution.method.MethodExecutionArgumentValue;
import com.dbn.execution.method.MethodExecutionArgumentValueHistory;
import com.dbn.execution.method.MethodExecutionManager;
import com.dbn.object.DBJavaField;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JavaExecutionComplexInputArgumentForm extends DBNFormBase {
	private JPanel mainPanel;
	private JLabel argumentLabel;
	private JLabel argumentTypeLabel;
	private JPanel typeAttributesPanel;
	private JPanel inputFieldPanel;

	private JTextField inputTextField;
	private UserValueHolderImpl<String> userValueHolder;

	private final DBObjectRef<DBJavaField> argument2;

	JavaExecutionComplexInputArgumentForm(JavaExecutionInputForm parentForm, DBJavaField argument) {
		super(parentForm);
		this.argument2 = DBObjectRef.of(argument);
		String argumentName = argument.getName();
		argumentLabel.setText(argumentName);
		argumentLabel.setIcon(argument.getIcon());

		String dataType = argument.getType();

		argumentTypeLabel.setForeground(UIUtil.getInactiveTextColor());

		argumentTypeLabel.setText(dataType);
		typeAttributesPanel.setVisible(false);

		Project project = argument.getProject();
		JavaExecutionInput executionInput = parentForm.getExecutionInput();
		String value = executionInput.getInputValue(argument);

		TextFieldWithPopup<?> inputField = new TextFieldWithPopup<>(project);
		inputField.setPreferredSize(new Dimension(240, -1));


		inputField.createValuesListPopup(createValuesProvider(), true);
		inputTextField = inputField.getTextField();
		inputTextField.setText(value);
		inputFieldPanel.add(inputField, BorderLayout.CENTER);


		inputTextField.setDisabledTextColor(inputTextField.getForeground());
	}

	@NotNull
	public JavaExecutionInputForm getParentForm() {
		return ensureParentComponent();
	}

	@NotNull
	private ListPopupValuesProvider createValuesProvider() {
		return new ListPopupValuesProvider() {
			@Override
			public String getDescription() {
				return "History Values List";
			}

			@Override
			public java.util.List<String> getValues() {
				DBJavaField argument = getArgument2();
				if (argument != null) {
					JavaExecutionInput executionInput = getParentForm().getExecutionInput();
					return executionInput.getInputValueHistory(argument);
				}

				return Collections.emptyList();
			}

			@Override
			public java.util.List<String> getSecondaryValues() {
				DBJavaField argument = getArgument2();
				if (argument != null) {
					ConnectionHandler connection = argument.getConnection();
					ConnectionId connectionId = connection.getConnectionId();
					MethodExecutionManager executionManager = MethodExecutionManager.getInstance(argument.getProject());
					MethodExecutionArgumentValueHistory valuesHistory = executionManager.getArgumentValuesHistory();
					MethodExecutionArgumentValue argumentValue = valuesHistory.getArgumentValue(connectionId, argument.getName(), false);
					if (argumentValue != null) {
						List<String> cachedValues = new ArrayList<>(argumentValue.getValueHistory());
						cachedValues.removeAll(getValues());
						return cachedValues;
					}
				}
				return Collections.emptyList();
			}
		};
	}

	public DBJavaField getArgument2() {
		return DBObjectRef.get(argument2);
	}

	@NotNull
	@Override
	public JPanel getMainComponent() {
		return mainPanel;
	}

	public void updateExecutionInput() {
		DBJavaField argument = getArgument2();
		if (argument != null) {
			JavaExecutionInput executionInput = getParentForm().getExecutionInput();
			if (userValueHolder != null) {
				String value = userValueHolder.getUserValue();
				executionInput.setInputValue(argument, value);
			} else {
				String value = Commons.nullIfEmpty(inputTextField == null ? null : inputTextField.getText());
				executionInput.setInputValue(argument, value);
			}
		}
	}


	public void addDocumentListener(DocumentListener documentListener) {
		TextFields.addDocumentListener(inputTextField, documentListener);
	}

	public int getScrollUnitIncrement() {
		return (int) mainPanel.getPreferredSize().getHeight();
	}
}

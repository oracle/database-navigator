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
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

public class JavaExecutionInputParameterForm extends DBNFormBase {
	private JPanel mainPanel;
	private JLabel parameterLabel;
	private JLabel parameterTypeLabel;
	private JPanel typeAttributesPanel;
	private JPanel inputFieldPanel;

	private final JTextField inputTextField;
	private UserValueHolderImpl<String> userValueHolder;

	private final DBObjectRef<DBJavaParameter> parameter;

	JavaExecutionInputParameterForm(JavaExecutionInputForm parentForm, DBJavaParameter parameter) {
		super(parentForm);
		this.parameter = DBObjectRef.of(parameter);
		String argumentName = parameter.getName();
		parameterLabel.setText(argumentName);
		parameterLabel.setIcon(parameter.getIcon());

		String dataType = parameter.getParameterType();

		parameterTypeLabel.setForeground(UIUtil.getInactiveTextColor());
		parameterTypeLabel.setText(dataType);
		typeAttributesPanel.setVisible(false);

		Project project = parameter.getProject();
		JavaExecutionInput executionInput = parentForm.getExecutionInput();
		String value = executionInput.getInputValue(parameter);

		TextFieldWithPopup<?> inputField = new TextFieldWithPopup<>(project);
		inputField.setPreferredSize(new Dimension(240, -1));


		inputField.createValuesListPopup(createValuesProvider(), parameter, true);
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
			public String getName() {
				return "Value History";
			}

			@Override
			public List<String> getValues() {
				DBJavaParameter parameter = getParameter();
                if (parameter == null) return emptyList();

                JavaExecutionInput executionInput = getParentForm().getExecutionInput();
                return executionInput.getInputValueHistory(parameter);

            }

			@Override
			public List<String> getSecondaryValues() {
				DBJavaParameter parameter = getParameter();
                if (parameter == null) return emptyList();

                ConnectionHandler connection = parameter.getConnection();
                ConnectionId connectionId = connection.getConnectionId();
                MethodExecutionManager executionManager = MethodExecutionManager.getInstance(parameter.getProject());
                MethodExecutionArgumentValueHistory valuesHistory = executionManager.getArgumentValuesHistory();
                MethodExecutionArgumentValue argumentValue = valuesHistory.getArgumentValue(connectionId, parameter.getName(), false);
                if (argumentValue != null) {
                    List<String> cachedValues = new ArrayList<>(argumentValue.getValueHistory());
                    cachedValues.removeAll(getValues());
                    return cachedValues;
                }
                return emptyList();
			}
		};
	}

	public DBJavaParameter getParameter() {
		return DBObjectRef.get(parameter);
	}

	@NotNull
	@Override
	public JPanel getMainComponent() {
		return mainPanel;
	}

	public void updateExecutionInput() {
		DBJavaParameter parameter = getParameter();
        if (parameter == null) {
            return;
        }
        JavaExecutionInput executionInput = getParentForm().getExecutionInput();
        if (userValueHolder != null) {
            String value = userValueHolder.getUserValue();
            executionInput.setInputValue(parameter, value);
        } else {
            String value = Commons.nullIfEmpty(inputTextField == null ? null : inputTextField.getText());
            executionInput.setInputValue(parameter, value);
        }
    }

	protected int[] getMetrics(int[] metrics) {
		return new int[]{
				Math.max(metrics[0], (int) parameterLabel.getPreferredSize().getWidth()),
				Math.max(metrics[1], (int) inputFieldPanel.getPreferredSize().getWidth()),
				Math.max(metrics[2], (int) parameterTypeLabel.getPreferredSize().getWidth())};
	}

	protected void adjustMetrics(int[] metrics) {
		parameterLabel.setPreferredSize(new Dimension(metrics[0], parameterLabel.getHeight()));
		inputFieldPanel.setPreferredSize(new Dimension(metrics[1], inputFieldPanel.getHeight()));
		parameterTypeLabel.setPreferredSize(new Dimension(metrics[2], parameterTypeLabel.getHeight()));
	}

	public void addDocumentListener(DocumentListener documentListener) {
		TextFields.addDocumentListener(inputTextField, documentListener);
	}

	public int getScrollUnitIncrement() {
		return (int) mainPanel.getPreferredSize().getHeight();
	}
}

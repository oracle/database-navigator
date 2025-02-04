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
import com.dbn.common.string.StringDeBuilder;
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

public class JavaExecutionInputFieldForm extends DBNFormBase implements ComponentAligner.Form{
	private JPanel mainPanel;
	private JLabel fieldLabel;
	private JLabel fieldTypeLabel;
	private JPanel fieldsPanel;
	private JPanel inputFieldPanel;

	private JTextField inputTextField;
	private UserValueHolderImpl<String> userValueHolder;

	private final DBObjectRef<DBJavaField> field;
	private final List<JavaExecutionInputFieldForm> fieldForms = DisposableContainers.list(this);

	private final String fieldPath;

	JavaExecutionInputFieldForm(DBNForm parentForm, DBJavaField field) {
		super(parentForm);
		this.field = DBObjectRef.of(field);
		fieldLabel.setText(field.getName());
		//fieldLabel.setIcon(field.getIcon());
		fieldLabel.setBorder(Borders.insetBorder(4, computeIndent(), 4, 0));
		fieldPath = buildFieldPath();

		if (field.isPlainValue()) {
			initPlainField();
		} else {
			initClassField();
		}
	}

	private int computeIndent() {
		// compute the indentation depending on the nesting level of the field
		int indent = 40;
		DBNForm parentForm = getParentComponent();
		while (parentForm instanceof JavaExecutionInputFieldForm) {
			indent += 20;
			parentForm = parentForm.getParentComponent();
		}

		return indent;
	}

	private String buildFieldPath() {
		StringDeBuilder builder = new StringDeBuilder();
		builder.append(getFieldName());

		DBNForm parentForm = getParentComponent();
		while (parentForm instanceof JavaExecutionInputFieldForm) {
			JavaExecutionInputFieldForm fieldForm = (JavaExecutionInputFieldForm) parentForm;
			builder.prepend(".");
			builder.prepend(fieldForm.getFieldName());

			parentForm = parentForm.getParentComponent();
		}

		if (parentForm instanceof JavaExecutionInputParameterForm) {
			JavaExecutionInputParameterForm parameterForm = (JavaExecutionInputParameterForm) parentForm;
			builder.prepend(".");
			builder.prepend(parameterForm.getParameterName());
		}

		return builder.toString();
	}

	private void initPlainField() {
		DBJavaField field = getField();

		String javaClassName = field.getJavaClassName();
		if (field.isClass()) {
			String className = getCanonicalName(javaClassName);
			fieldTypeLabel.setText(className);
			fieldTypeLabel.setIcon(/*field.getFieldClass().getIcon()*/Icons.DBO_JAVA_CLASS); // TODO do not force loading the field class
		} else {
			fieldTypeLabel.setText(javaClassName);
		}

		fieldTypeLabel.setForeground(UIUtil.getInactiveTextColor());
		fieldsPanel.setVisible(false);

		Project project = field.getProject();
		JavaExecutionInput executionInput = getExecutionInput();
		String value = executionInput.ensureInputValue(fieldPath);

		TextFieldWithPopup<?> inputField = new TextFieldWithPopup<>(project);
		inputField.setPreferredSize(new Dimension(240, -1));


		inputField.createValuesListPopup(createValuesProvider(), field, true);
		inputTextField = inputField.getTextField();
		inputTextField.setText(value);
		inputFieldPanel.add(inputField, BorderLayout.CENTER);

		inputTextField.setDisabledTextColor(inputTextField.getForeground());
	}

	private void initClassField() {
		DBJavaField field = getField();
		String className = field.getJavaClassName();

		DBJavaClass javaClass = field.getJavaClass();
		fieldTypeLabel.setText("");
		fieldTypeLabel.setVisible(false);

		JLabel classLabel = new JLabel(getCanonicalName(className));
		classLabel.setIcon(javaClass == null ? Icons.DBO_JAVA_CLASS : javaClass.getIcon());
		classLabel.setForeground(UIUtil.getInactiveTextColor());
		inputFieldPanel.add(classLabel, BorderLayout.WEST);

		verticalBoxLayout(fieldsPanel);
		List<DBJavaField> fields = javaClass == null ? emptyList() : javaClass.getFields();
		fields.forEach(f -> addFieldPanel(f));
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
				DBJavaField field = getField();
                if (field == null) return emptyList();

                JavaExecutionInput executionInput = getExecutionInput();
                return executionInput.getInputValueHistory(fieldPath);
            }

			@Override
			public List<String> getSecondaryValues() {
				DBJavaField field = getField();
                if (field == null) return emptyList();

                ConnectionHandler connection = field.getConnection();
                ConnectionId connectionId = connection.getConnectionId();
				JavaExecutionManager executionManager = JavaExecutionManager.getInstance(field.getProject());
                ExecutionVariableHistory valuesHistory = executionManager.getInputValuesHistory();
				ExecutionVariable argumentValue = valuesHistory.getExecutionVariable(connectionId, field.getName(), false);
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


	public DBJavaField getField() {
		return DBObjectRef.get(field);
	}

	public String getFieldName() {
		return field.getObjectName();
	}

	@NotNull
	@Override
	public JPanel getMainComponent() {
		return mainPanel;
	}

	public void updateExecutionInput() {
		DBJavaField field = getField();
        if (field == null) return;

		if (fieldForms.isEmpty()) {
			JavaExecutionInput executionInput = getExecutionInput();
			if (userValueHolder != null) {
				String value = userValueHolder.getUserValue();
				executionInput.setInputValue(fieldPath, value);
			} else {
				String value = Commons.nullIfEmpty(inputTextField == null ? null : inputTextField.getText());
				executionInput.setInputValue(fieldPath, value);
			}
		} else {
			fieldForms.forEach(f -> f.updateExecutionInput());
		}
    }

	public void addDocumentListener(DocumentListener documentListener) {
		TextFields.addDocumentListener(inputTextField, documentListener);
	}

	@NotNull
	JavaExecutionInput getExecutionInput() {
		JavaExecutionInputForm executionInputForm = getParentFrom(JavaExecutionInputForm.class);
		return nd(executionInputForm).getExecutionInput();
	}

	/*********************************************************************
	 *                      {@link ComponentAligner}
	 *********************************************************************/
	@Override
	public Component[] getAlignableComponents() {
		return new Component[] {fieldLabel, inputFieldPanel, fieldTypeLabel};
	}

	@Override
	public List<? extends ComponentAligner.Form> getAlignableForms() {
		return fieldForms;
	}

	public int countFields() {
		return 1 + fieldForms.stream().mapToInt(f -> f.countFields()).sum();
	}
}

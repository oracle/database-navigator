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
import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.TextFields;
import com.dbn.common.util.Commons;
import com.dbn.data.editor.ui.ListPopupValuesProvider;
import com.dbn.data.editor.ui.TextFieldWithPopup;
import com.dbn.data.editor.ui.UserValueHolderImpl;
import com.dbn.execution.java.JavaExecutionInput;
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
import java.awt.Dimension;
import java.util.List;

import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.ui.Layouts.verticalBoxLayout;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.util.Lists.sortedCopy;
import static com.dbn.execution.java.ui.JavaExecutionInputUtil.setupSingleDimArrayEditor;
import static com.dbn.object.DBOrderedObject.POSITION_COMPARATOR;
import static com.dbn.object.lookup.DBJavaNameCache.getCanonicalName;
import static java.util.Collections.emptyList;


public class JavaExecutionInputParameterForm extends DBNFormBase {
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
		if (parameter.isScalar()) {
			initPlainField();
		} else {
			initClassField();
		}
	}

	@Override
	protected void initFieldAlignment() {
		FieldAlignerData alignerData = getFieldAlignerData();
		alignerData.registerForms(() -> fieldForms);
		alignerData.registerFieldGroup(parameterLabel, inputFieldPanel, parameterTypeLabel);
	}

	private void initPlainField() {
		DBJavaParameter parameter = getParameter();
		Project project = parameter.getProject();
		JavaExecutionInput executionInput = getExecutionInput();
		String value = executionInput.ensureInputValue(parameter.getName());

		TextFieldWithPopup<?> inputField = new TextFieldWithPopup<>(project);
		inputField.setPreferredSize(new Dimension(240, -1));
		DBObjectRef<DBJavaClass> javaClass = parameter.getJavaClassRef();
		parameterTypeLabel.setText(getCanonicalName(javaClass));
		if (parameter.isClass()) {
			parameterTypeLabel.setIcon(/*parameter.getParameterClass().getIcon()*/Icons.DBO_JAVA_CLASS); // TODO performance issue (do not force loading the field class)
		}

		inputTextField = inputField.getTextField();
		inputTextField.setText(value);
		inputFieldPanel.add(inputField);

		inputTextField.setDisabledTextColor(inputTextField.getForeground());
		fieldsPanel.setVisible(false);

        if (parameter.getArrayDepth() == 1) {
            setupSingleDimArrayEditor(inputField, parameter);
        }
        inputField.createValuesListPopup(createValuesProvider(), parameter, true);
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
		fields = sortedCopy(fields, POSITION_COMPARATOR);
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
			if (userValueHolder != null) {
				String value = userValueHolder.getUserValue();
				executionInput.setInputValue(parameter, value);
			} else {
				String value = Commons.nullIfEmpty(getText(inputTextField));
				executionInput.setInputValue(parameter, value);
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

	public int countFields() {
		return 1 + fieldForms.stream().mapToInt(f -> f.countFields()).sum();
	}
}

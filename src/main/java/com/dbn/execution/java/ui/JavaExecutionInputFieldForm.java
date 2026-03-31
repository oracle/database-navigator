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
import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.TextFields;
import com.dbn.common.util.Commons;
import com.dbn.data.editor.ui.ListPopupValuesProvider;
import com.dbn.data.editor.ui.TextFieldWithPopup;
import com.dbn.execution.ExecutionInputMode;
import com.dbn.execution.java.JavaExecutionInput;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaField;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentListener;
import java.awt.Dimension;
import java.util.List;
import java.util.Objects;

import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.ui.Layouts.verticalBoxLayout;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.util.Lists.filter;
import static com.dbn.common.util.Lists.sortedCopy;
import static com.dbn.execution.java.ui.JavaExecutionInputUtil.setupSingleDimArrayEditor;
import static com.dbn.object.DBOrderedObject.POSITION_COMPARATOR;
import static com.dbn.object.lookup.DBJavaNameCache.getCanonicalName;
import static java.util.Collections.emptyList;

public class JavaExecutionInputFieldForm extends DBNFormBase {
	private JPanel mainPanel;
	private JLabel fieldLabel;
	private JLabel fieldTypeLabel;
	private JPanel fieldsPanel;
	private JPanel inputFieldPanel;

	private JTextField inputTextField;
	private final DBObjectRef<DBJavaField> field;
	private final List<JavaExecutionInputFieldForm> fieldForms = DisposableContainers.list(this);

	private final String fieldPath;

	JavaExecutionInputFieldForm(DBNForm parentForm, DBJavaField field) {
		super(parentForm);
		this.field = DBObjectRef.of(field);
		this.fieldPath = buildFieldPath();

		intiFieldLabel();
		initFieldTypeLabel();

		initPlainField();
		initClassField();
	}

	private void intiFieldLabel() {
		DBJavaField field = getField();
		fieldLabel.setText(field.getName());
		//fieldLabel.setIcon(field.getIcon());
		fieldLabel.setBorder(Borders.insetBorder(4, computeIndent(), 4, 0));
	}

	private void initFieldTypeLabel() {
		DBJavaField field = getField();

		if (field.isClass()) {
			DBObjectRef<DBJavaClass> javaClass = field.getJavaClassRef();
			String className = getCanonicalName(javaClass);
			fieldTypeLabel.setText(className);
			fieldTypeLabel.setIcon(/*field.getFieldClass().getIcon()*/Icons.DBO_JAVA_CLASS); // TODO do not force loading the field class
		} else {
			String javaClassName = field.getJavaClassName();
			fieldTypeLabel.setText(javaClassName);
		}

		fieldTypeLabel.setForeground(UIUtil.getInactiveTextColor());
	}

	@Override
	protected void initFieldAlignment() {
		FieldAlignerData alignerData = getFieldAlignerData();
		alignerData.registerForms(() -> fieldForms);
		alignerData.registerFieldGroup(fieldLabel, inputFieldPanel, fieldTypeLabel);
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
		while (parentForm instanceof JavaExecutionInputFieldForm fieldForm) {
            builder.prepend(".");
			builder.prepend(fieldForm.getFieldName());

			parentForm = parentForm.getParentComponent();
		}

		if (parentForm instanceof JavaExecutionInputParameterForm parameterForm) {
            builder.prepend(".");
			builder.prepend(parameterForm.getParameterName());
		}

		return builder.toString();
	}

	private void initPlainField() {
		DBJavaField field = getField();
		if (!field.isScalar()) return;

		fieldsPanel.setVisible(false);

		Project project = field.getProject();
		JavaExecutionInput executionInput = getExecutionInput();
		String value = executionInput.getInputValue(fieldPath, ExecutionInputMode.FIELDS);

		TextFieldWithPopup<?> inputField = new TextFieldWithPopup<>(project);
		inputField.setPreferredSize(new Dimension(240, -1));

		inputTextField = inputField.getTextField();
		inputTextField.setText(value);
		inputFieldPanel.add(inputField);
		inputTextField.setDisabledTextColor(inputTextField.getForeground());

        if (field.getArrayDepth() == 1) {
            setupSingleDimArrayEditor(inputField, field);
        }
        inputField.createValuesListPopup(createValuesProvider(), field, true);
	}

	private void initClassField() {
		DBJavaField field = getField();
		if (field.isScalar()) return;

		moveTypeLabel();

		verticalBoxLayout(fieldsPanel);
		DBJavaClass javaClass = field.getJavaClass();
		List<DBJavaField> fields = javaClass == null ? emptyList() : javaClass.getFields();
		fields = sortedCopy(fields, POSITION_COMPARATOR);

		// prevent cascading endlessly if field type matches the parent field type
		// (e.g. a "Node" class having itself reference to a parent of type "Node")
		// TODO what about indirect reference chains? (e.g. Node references Path, while Path references Node)
		fields = filter(fields, f -> !Objects.equals(f.getJavaClass(), javaClass));
		fields.forEach(f -> addFieldPanel(f));
	}

	private void moveTypeLabel() {
		// alternative location of field-type label for non-scalar fields
		JLabel typeLabel = new JLabel(
				fieldTypeLabel.getText(),
				fieldTypeLabel.getIcon(),
				SwingConstants.LEFT);
		typeLabel.setForeground(fieldTypeLabel.getForeground());
		typeLabel.setBorder(Borders.insetBorder(4, 0, 4, 0));
		inputFieldPanel.add(typeLabel);

		fieldTypeLabel.setVisible(false);
		//fieldTypeLabel.setText("");
		//fieldTypeLabel.setIcon(null);
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
			String value = Commons.nullIfEmpty(getText(inputTextField));
			executionInput.setInputValue(fieldPath, value);
		} else {
			fieldForms.forEach(f -> f.updateExecutionInput());
		}
    }

	public void removeExecutionInput() {
		if (fieldForms.isEmpty()) {
			JavaExecutionInput executionInput = getExecutionInput();
			executionInput.removeInput(fieldPath);
		} else{
			fieldForms.forEach(f -> f.removeExecutionInput());
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

	public int countFields() {
		return 1 + fieldForms.stream().mapToInt(f -> f.countFields()).sum();
	}
}

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
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Commons;
import com.dbn.data.editor.ui.ListPopupValuesProvider;
import com.dbn.data.editor.ui.TextFieldWithPopup;
import com.dbn.execution.ExecutionInputMode;
import com.dbn.execution.common.input.CodeBlock;
import com.dbn.execution.common.input.ExecutionVariable;
import com.dbn.execution.java.JavaExecutionInput;
import com.dbn.execution.java.ui.JavaExecutionInputUtil.UiSuitability;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaField;
import com.dbn.object.DBJavaParameter;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentListener;
import java.awt.Dimension;
import java.util.List;
import java.util.Set;

import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.ui.Layouts.verticalBoxLayout;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.util.Lists.sortedCopy;
import static com.dbn.execution.java.ui.JavaExecutionInputUtil.classifyForUi;
import static com.dbn.execution.java.ui.JavaExecutionInputUtil.setupSingleDimArrayEditor;
import static com.dbn.execution.java.wrapper.WrapperStatementBuilder.arrayBrackets;
import static com.dbn.object.DBOrderedObject.POSITION_COMPARATOR;
import static java.util.Collections.emptyList;


public class JavaExecutionInputParameterForm extends DBNFormBase {
	private JPanel mainPanel;
	private JLabel parameterLabel;
	private JLabel parameterTypeLabel;
	private JPanel inputFieldsPanel;
	private JPanel inputFieldPanel;
	private JPanel inputCodePanel;
	private JComboBox<ExecutionInputMode> inputModeComboBox;
	private final Set<ExecutionInputMode> inputModes;
	private JavaCodeEditorPanel inputCodeEditor;

	private JTextField inputTextField;
	private final DBObjectRef<DBJavaParameter> parameter;
	private final List<JavaExecutionInputFieldForm> fieldForms = DisposableContainers.list(this);

	JavaExecutionInputParameterForm(DBNForm parentForm, DBJavaParameter parameter) {
		super(parentForm);
		this.parameter = DBObjectRef.of(parameter);
		this.inputModes = resolveInputModes();

		initParameterLabel();
		initParameterTypeLabel();
		initInputModes();

		initPlainFields();
		initClassFields();
		initCodeFields();

		updateInputFields();
	}

	@Override
	protected void initFieldAlignment() {
		FieldAlignerData alignerData = getFieldAlignerData();
		alignerData.registerForms(() -> fieldForms);
		alignerData.registerFieldGroup(parameterLabel, inputFieldPanel, parameterTypeLabel);
	}

	private void initInputModes() {
		initComboBox(inputModeComboBox, inputModes);

		inputModeComboBox.setEnabled(inputModes.size() > 1);
		inputModeComboBox.setVisible(!getParameter().isScalar());

		JavaExecutionInput executionInput = getExecutionInput();
		ExecutionVariable executionVariable = executionInput.getExecutionVariable(getParameterName());
		ExecutionInputMode inputMode = executionVariable.getMode();
		if (!inputModes.contains(inputMode)) {
			inputMode = inputModes.iterator().next();
			executionVariable.setMode(inputMode);
		}
		setSelection(inputModeComboBox, inputMode);

		if (inputModes.size() > 1) {
			inputModeComboBox.addActionListener(e -> updateInputFields());
		}
	}

	private Set<ExecutionInputMode> resolveInputModes() {
		DBJavaParameter parameter = getParameter();
		if (parameter.isScalar()) return Set.of(ExecutionInputMode.FIELDS);

		UiSuitability suitability = classifyForUi(parameter, getExecutionInput().getWrapperSupportData());
		if (suitability == UiSuitability.UI_NOT_SUPPORTED) return Set.of(ExecutionInputMode.CODE);

		return Set.of(ExecutionInputMode.FIELDS, ExecutionInputMode.CODE);
	}

	private void initParameterLabel() {
		DBJavaParameter parameter = getParameter();
		parameterLabel.setText(parameter.getName());
		parameterLabel.setIcon(parameter.getIcon());
		parameterLabel.setBorder(Borders.insetBorder(4, 0, 4, 0));
	}

	private void initParameterTypeLabel() {
		DBJavaParameter parameter = getParameter();
		parameterTypeLabel.setForeground(UIUtil.getInactiveTextColor());
		parameterTypeLabel.setText(parameter.getJavaClassName());
		if (parameter.isClass()) {
			parameterTypeLabel.setIcon(/*parameter.getParameterClass().getIcon()*/Icons.DBO_JAVA_CLASS); // TODO performance issue (do not force loading the field class)
		}
	}

	private void initPlainFields() {
		DBJavaParameter parameter = getParameter();
		if (!parameter.isScalar()) return;

		Project project = parameter.getProject();
		JavaExecutionInput executionInput = getExecutionInput();
		String value = executionInput.getInputValue(getParameterName(), ExecutionInputMode.FIELDS);

		TextFieldWithPopup<?> inputField = new TextFieldWithPopup<>(project);
		inputField.setPreferredSize(new Dimension(240, -1));

		inputTextField = inputField.getTextField();
		inputTextField.setText(value);
		inputFieldPanel.add(inputField);

		inputTextField.setDisabledTextColor(inputTextField.getForeground());
		inputFieldsPanel.setVisible(false);

        if (parameter.getArrayDepth() == 1) {
            setupSingleDimArrayEditor(inputField, parameter);
        }
        inputField.createValuesListPopup(createValuesProvider(), parameter, true);
	}

	private void initClassFields() {
		DBJavaParameter parameter = getParameter();
		if (parameter.isScalar()) return;

		moveTypeLabel();
		if (!isValueSupported()) return;

		verticalBoxLayout(inputFieldsPanel);
		DBJavaClass javaClass = parameter.getJavaClass();
		List<DBJavaField> fields = javaClass.getFields();
		fields = sortedCopy(fields, POSITION_COMPARATOR);
		fields.forEach(f -> addFieldPanel(f));

		JPanel spacer = new JPanel();
		spacer.setPreferredSize(new Dimension(-1, 8));
		inputFieldsPanel.add(spacer);
	}

	private void moveTypeLabel() {
		// alternative location of parameter-type label for non-scalar parameters
		JLabel typeLabel = new JLabel(
				parameterTypeLabel.getText(),
				parameterTypeLabel.getIcon(),
				SwingConstants.LEFT);
		typeLabel.setForeground(parameterTypeLabel.getForeground());
		typeLabel.setBorder(Borders.insetBorder(4, 0, 4, 0));
		inputFieldPanel.add(typeLabel);

		parameterTypeLabel.setVisible(false);
		//parameterTypeLabel.setText("");
		//parameterTypeLabel.setIcon(null);
	}

	private void initCodeFields() {
		if (!isCodeSupported()) return;
		inputCodeEditor = new JavaCodeEditorPanel(this, getProject());
		inputCodePanel.add(inputCodeEditor);

		JavaExecutionInput executionInput = getExecutionInput();
		String codeBlock = executionInput.getInputValue(getParameterName(), ExecutionInputMode.CODE);
		String code = codeBlock == null ?
				getJavaTypeDeclaration() :
				CodeBlock.deserialize(codeBlock).getContent();

		inputCodeEditor.setText(code);
	}

	private void updateInputFields() {
		ExecutionInputMode inputMode = getInputMode();
		JavaExecutionInput executionInput = getExecutionInput();
		ExecutionVariable executionVariable = executionInput.getExecutionVariable(getParameterName());
		executionVariable.setMode(inputMode);

		inputFieldsPanel.setVisible(inputMode == ExecutionInputMode.FIELDS);
		inputCodePanel.setVisible(inputMode == ExecutionInputMode.CODE);

		recalibrateInputForm();
	}

	private void recalibrateInputForm() {
		JScrollPane scrollPane = UserInterface.getParentOfType(mainPanel, JScrollPane.class);
		if (scrollPane != null) {
			Dimension preferredSize = scrollPane.getViewport().getPreferredSize();
			scrollPane.setPreferredSize(preferredSize);
			getParentForm().revalidateForm();
		}
	}

	private @Nullable ExecutionInputMode getInputMode() {
		return getSelection(inputModeComboBox);
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
		inputFieldsPanel.add(argumentComponent.getComponent());
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

		JavaExecutionInput executionInput = getExecutionInput();
		ExecutionInputMode inputMode = getInputMode();
		if (inputMode == ExecutionInputMode.FIELDS) {
			if (fieldForms.isEmpty()) {
				String value = Commons.nullIfEmpty(getText(inputTextField));
				executionInput.setInputValue(parameter, value);
			} else {
				fieldForms.forEach(f -> f.updateExecutionInput());
			}
		} else {
			String code = inputCodeEditor.getText();
			CodeBlock codeBlock = new CodeBlock(code, CodeBlock.Language.JAVA);
			executionInput.setInputValue(parameter, codeBlock.serialize());
			fieldForms.forEach(f -> f.removeExecutionInput());
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

	private boolean isCodeSupported() {
		return inputModes.contains(ExecutionInputMode.CODE);
	}

	private boolean isValueSupported() {
		return inputModes.contains(ExecutionInputMode.FIELDS);
	}

	@NonNls
	private String getJavaTypeDeclaration() {
		DBJavaParameter parameter  = getParameter();

        return "%s%s param%s = null;\n".formatted(
				parameter.getJavaClassName(),
				arrayBrackets(parameter.getArrayDepth()),
				parameter.getPosition());
	}

}

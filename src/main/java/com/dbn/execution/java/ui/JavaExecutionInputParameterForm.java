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

import com.dbn.common.color.Colors;
import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.Presentable;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.ComponentAligner;
import com.dbn.common.ui.util.TextFields;
import com.dbn.common.util.Commons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.data.editor.ui.ListPopupValuesProvider;
import com.dbn.data.editor.ui.TextFieldWithPopup;
import com.dbn.execution.common.input.CodeBlock;
import com.dbn.execution.common.input.ExecutionVariable;
import com.dbn.execution.common.input.ExecutionVariableHistory;
import com.dbn.execution.java.ui.JavaExecutionInputUtil.UiSuitability;
import com.dbn.execution.java.JavaExecutionInput;
import com.dbn.execution.java.JavaExecutionManager;
import com.dbn.execution.java.wrapper.WrapperStatementBuilder;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaField;
import com.dbn.object.DBJavaParameter;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTextArea;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentListener;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.ui.Layouts.verticalBoxLayout;
import static com.dbn.common.util.Lists.sortedCopy;
import static com.dbn.execution.common.input.CodeBlock.isCodeBlock;
import static com.dbn.execution.java.ui.JavaExecutionInputUtil.setupSingleDimArrayEditor;
import static com.dbn.execution.java.ui.JavaExecutionInputUtil.classifyForUi;
import static com.dbn.object.DBOrderedObject.POSITION_COMPARATOR;
import static com.dbn.object.lookup.DBJavaNameCache.getCanonicalName;
import static java.util.Collections.emptyList;


public class JavaExecutionInputParameterForm extends DBNFormBase implements ComponentAligner.Form {
	private JPanel mainPanel;
	private JLabel parameterLabel;
	private JLabel parameterTypeLabel;
	private JPanel fieldsPanel;
	private JPanel inputFieldPanel;
	private DBNComboBox<InitializationMode> initializationModeComboBox;
	private DocumentListener documentListener;

	private final DBObjectRef<DBJavaParameter> parameter;
	private final List<JavaExecutionInputFieldForm> fieldForms = DisposableContainers.list(this);

	private final UiSuitability uiSuitability;
	private final boolean fieldsRequired;

	private JBTextArea codeInputTextField;
	private TextFieldWithPopup<?> plainInputTextField;

	private enum InitializationMode implements Presentable {
		CODE("Code"),
		FORM("Form");

		private final String label;
		InitializationMode(String label) {
			this.label = label;
		}
		@Override
		public String toString() {
			return label;
		}
		@Override
		public @NotNull String getName() {
			return label;
		}
	}

	JavaExecutionInputParameterForm(DBNForm parentForm, DBJavaParameter parameter) {
		super(parentForm);
		this.parameter = DBObjectRef.of(parameter);
		parameterLabel.setText(parameter.getName());
		parameterLabel.setIcon(parameter.getIcon());
		parameterLabel.setBorder(Borders.insetBorder(4, 0, 4, 0));

		uiSuitability = classifyForUi(getParameter(), getExecutionInput().getWrapperComplianceCache());
		fieldsRequired = !(parameter.isScalar()) && (uiSuitability != UiSuitability.UI_NOT_SUPPORTED);

        String displayClassName = getCanonicalName(parameter.getJavaClassRef())
								+ "[]".repeat(parameter.getArrayDepth());

        parameterTypeLabel.setForeground(UIUtil.getInactiveTextColor());
        parameterTypeLabel.setText(displayClassName);
        if (parameter.isClass()) {
            parameterTypeLabel.setIcon(Icons.DBO_JAVA_CLASS);
        }

        initializationModeComboBox.setVisible(false);
        if (!parameter.isScalar()) {
            mainPanel.remove(inputFieldPanel);
            initInputModeComboBox();
        } else {
            mainPanel.remove(initializationModeComboBox);
            for (Component component : mainPanel.getComponents()) {
                if (component instanceof Spacer) {
                    mainPanel.remove(component);
                }
            }
            initPlainTextField();
        }
	}

	private void initInputModeComboBox() {
		if(uiSuitability != UiSuitability.UI_NOT_SUPPORTED) {
			initializationModeComboBox.addItem(InitializationMode.FORM);
            initializationModeComboBox.setVisible(true);
		}
		initializationModeComboBox.addItem(InitializationMode.CODE);
        initializationModeComboBox.setSelectedIndex(0);

		updateInitializationMode(initializationModeComboBox.getItemAt(0));
		initializationModeComboBox.addListener((oldValue, newValue) -> updateInitializationMode(newValue));
	}

	private InitializationMode getInitializationMode() {
		if(getParameter().isScalar()) {return InitializationMode.FORM;}
		return (InitializationMode) initializationModeComboBox.getSelectedItem();
	}

	private void updateInitializationMode(InitializationMode initializationMode) {
        resetFieldsPanel();
        if (initializationMode == InitializationMode.FORM) {
			setFormInput();
		} else {
			addTextAreaField();
		}
	}

    private void resetFieldsPanel() {
        fieldsPanel.removeAll();
    }

    private String getJavaTypeDeclaration(DBJavaParameter parameter) {
		StringBuilder declaration = new StringBuilder(getCanonicalName(parameter.getJavaClassRef()));
		if (parameter.isArray())
			declaration.append("[]".repeat(Math.max(0, parameter.getArrayDepth())));
		declaration.append(" ");

		declaration.append(new WrapperStatementBuilder(parameter.getProject())
				.getJavaInitializedArgumentName(parameter.getPosition()));
		declaration.append(" = null;" + System.lineSeparator());

		return declaration.toString();
	}

	private void setFormInput() {
		if (fieldsRequired) {
            initFieldForm();
		}
	}

	private void addTextAreaField() {
		if (codeInputTextField == null) {
            initCodeAreaTextField();
        }
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));
        fieldsPanel.add(codeInputTextField);
	}

	private void initPlainTextField() {
		DBJavaParameter parameter = getParameter();
		Project project = parameter.getProject();
		JavaExecutionInput executionInput = getExecutionInput();
		String value = executionInput.ensureInputValue(parameter.getName());

		plainInputTextField = new TextFieldWithPopup<>(project);
		plainInputTextField.setPreferredSize(new Dimension(240, -1));

		JTextField inputTextField = plainInputTextField.getTextField();
		inputTextField.setText(value);
		if(documentListener!=null){TextFields.addDocumentListener(inputTextField, documentListener);}

		if (parameter.getArrayDepth() == 1) {
			setupSingleDimArrayEditor(plainInputTextField, parameter);
		}
		plainInputTextField.createValuesListPopup(createFormValuesProvider(), parameter, true);
		inputFieldPanel.add(plainInputTextField);
	}

	private void initCodeAreaTextField() {
		DBJavaParameter parameter = getParameter();

		//TODO
		//Use this to show history of values
		ListPopupValuesProvider codeValuesHistory = createCodeValuesProvider();
		String initialText;
		if(codeValuesHistory.getValues()!=null &&
				!codeValuesHistory.getValues().isEmpty()){
			initialText = codeValuesHistory.getValues().get(0);
		} else {
			initialText = getJavaTypeDeclaration(parameter);
		}

		codeInputTextField = new JBTextArea();
        codeInputTextField.setRows(5);
        codeInputTextField.setBorder(JBUI.Borders.compound(
                Borders.lineBorder(Colors.getOutlineColor()),
                Borders.TEXT_FIELD_INSETS
        ));
        codeInputTextField.setText(initialText);
	}

	private void initFieldForm() {
        DBJavaParameter parameter = getParameter();
		DBJavaClass javaClass = parameter.getJavaClass();
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
	private ListPopupValuesProvider createFormValuesProvider() {
		return createValuesProvider(false);
	}

	@NotNull
	private ListPopupValuesProvider createCodeValuesProvider() {
		return createValuesProvider(true);
	}

	@NotNull
	private ListPopupValuesProvider createValuesProvider(boolean codeHistory) {
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
				String parameterName = parameter.getName();
				List<String> allValues = executionInput.getInputValueHistory(parameterName);
				if(allValues!=null){
					return filterAndExtractValues(allValues, codeHistory);
				}
                return new ArrayList<>();

            }

			@Override
			public List<String> getSecondaryValues() {
				DBJavaParameter parameter = getParameter();
                if (parameter == null) return emptyList();

                ConnectionHandler connection = parameter.getConnection();
                ConnectionId connectionId = connection.getConnectionId();
                JavaExecutionManager executionManager = JavaExecutionManager.getInstance(parameter.getProject());
                ExecutionVariableHistory valuesHistory = executionManager.getInputValuesHistory();
				String parameterName = parameter.getName();
                ExecutionVariable argumentValue = valuesHistory.getExecutionVariable(connectionId, parameterName, false);
                if (argumentValue == null) return emptyList();

                List<String> cachedValues = filterAndExtractValues(argumentValue.getValueHistory(), codeHistory);
                cachedValues.removeAll(getValues());
                return cachedValues;
            }

			private List<String> filterAndExtractValues(
					List<String> allValues,
					boolean codeHistory
			) {
				return allValues.stream()
						.filter(Objects::nonNull)
						.filter(value -> {
							if (codeHistory) {
								if (!isCodeBlock(value)) return false;
								CodeBlock codeBlock = CodeBlock.deserialize(value);
								return codeBlock != null && codeBlock.getLanguage() == CodeBlock.Language.JAVA;
							} else {
								return !isCodeBlock(value);
							}
						})
						.map(value -> {
							if (codeHistory) {
								CodeBlock codeBlock = CodeBlock.deserialize(value);
								return codeBlock != null ? codeBlock.getContent() : null;
							} else {
								return value;
							}
						})
						.filter(Objects::nonNull)
						.collect(Collectors.toList());
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
		InitializationMode initializationMode = getInitializationMode();
		if(initializationMode == InitializationMode.FORM) {
			updateExecutionFormInput();
		} else{
			updateExecutionCodeInput();
		}
    }

	private void updateExecutionFormInput() {
		DBJavaParameter parameter = getParameter();
		String parameterName = parameter.getName();
		JavaExecutionInput executionInput = getParentForm().getExecutionInput();
		if(!fieldsRequired) {
			JTextField inputTextField = plainInputTextField.getTextField();
			String value = Commons.nullIfEmpty(inputTextField == null ? null : inputTextField.getText());
			executionInput.setInputValue(parameterName, value);
		} else {
			executionInput.removeInput(parameterName);
			fieldForms.forEach(f -> f.updateExecutionInput());
		}
	}

	private void updateExecutionCodeInput() {
		DBJavaParameter parameter = getParameter();
		JavaExecutionInput executionInput = getParentForm().getExecutionInput();
		String value = codeInputTextField.getText();
		CodeBlock codeBlock = new CodeBlock(value, CodeBlock.Language.JAVA);
		executionInput.setInputValue(parameter.getName(), codeBlock.serialize());
		if(fieldsRequired && !fieldForms.isEmpty()) {
			fieldForms.forEach(f -> f.removeExecutionInput());
		}
	}

	public void addDocumentListener(DocumentListener documentListener) {
		this.documentListener = documentListener;
		if(codeInputTextField != null) {
			TextFields.addDocumentListener(codeInputTextField, documentListener);
		}
		if(plainInputTextField != null) {
			TextFields.addDocumentListener(plainInputTextField.getTextField(), documentListener);
		}
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

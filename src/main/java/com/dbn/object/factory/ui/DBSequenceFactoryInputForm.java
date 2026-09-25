/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.object.factory.ui;

import com.dbn.common.state.StateAttributes;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.info.DBNInfoLabel;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.database.DatabaseIdentifierCase;
import com.dbn.object.factory.ObjectFactoryManager;
import com.dbn.object.factory.model.DBObjectSpec;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import java.math.BigInteger;

import static com.dbn.common.ui.form.DBNFormState.initPersistence;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.installNumericFilter;
import static com.dbn.common.ui.util.TextFields.setText;
import static com.dbn.common.util.Strings.isEmptyOrSpaces;
import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;
import static com.dbn.common.util.Strings.isWord;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.factory.model.DBObjectAttributeType.SEQUENCE_CACHE_SIZE;
import static com.dbn.object.factory.model.DBObjectAttributeType.SEQUENCE_CYCLE;
import static com.dbn.object.factory.model.DBObjectAttributeType.SEQUENCE_INCREMENT_BY;
import static com.dbn.object.factory.model.DBObjectAttributeType.SEQUENCE_MAX_VALUE;
import static com.dbn.object.factory.model.DBObjectAttributeType.SEQUENCE_MIN_VALUE;
import static com.dbn.object.factory.model.DBObjectAttributeType.SEQUENCE_START_WITH;

public class DBSequenceFactoryInputForm extends DBSchemaObjectFactoryInputForm {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private DBNComboBox<ConnectionHandler> connectionComboBox;
    private DBNComboBox<SchemaId> schemaComboBox;
    private JTextField nameTextField;
    private JTextField startWithTextField;
    private JTextField incrementByTextField;
    private JTextField minValueTextField;
    private JTextField maxValueTextField;
    private JTextField cacheSizeTextField;
    private JCheckBox cycleCheckBox;
    private JCheckBox preserveCaseCheckBox;
    private DBNInfoLabel preserveCaseInfoLabel;

    public DBSequenceFactoryInputForm(@NotNull DBNComponent parent, DBObjectSpec input) {
        super(parent, input);

        initContextComponents();
        initHeaderForm();
        initPreserveCaseFields();
        initNumericFields();
        resetFormChanges();
    }

    private void initNumericFields() {
        installNumericFilter(startWithTextField, true);
        installNumericFilter(incrementByTextField, true);
        installNumericFilter(minValueTextField, true);
        installNumericFilter(maxValueTextField, true);
        installNumericFilter(cacheSizeTextField, false);
    }

    private void initPreserveCaseFields() {
        preserveCaseInfoLabel.setContent(getPreserveCaseInfoText());
    }

    @Override
    protected void initStatePersistence() {
        StateAttributes state = ObjectFactoryManager.getInstance(ensureProject()).getState(getObjectType());
        initPersistence(preserveCaseCheckBox, state, "preserve-identifier-case");
    }

    @Override
    protected void initValidation() {
        addTextValidation(nameTextField,
                n -> isNotEmptyOrSpaces(n.trim()),
                txt("msg.objects.error.ObjectNameRequired", getObjectType().getDisplayName()));
        addTextValidation(nameTextField,
                n -> isEmptyOrSpaces(n) || isWord(n.trim()),
                txt("msg.objects.error.ValidObjectNameRequired", getObjectType().getDisplayName()));

        addIntegerValidation(startWithTextField, "app.object.label.SequenceStartWith");
        addIntegerValidation(incrementByTextField, "app.object.label.SequenceIncrementBy");
        addIntegerValidation(minValueTextField, "app.object.label.SequenceMinValue");
        addIntegerValidation(maxValueTextField, "app.object.label.SequenceMaxValue");
        addIntegerValidation(cacheSizeTextField, "app.object.label.SequenceCacheSize");
        addTextValidation(incrementByTextField,
                n -> isEmptyOrSpaces(n) || !isInteger(n) || !BigInteger.ZERO.equals(new BigInteger(n.trim())),
                txt("msg.objects.error.SequenceIncrementInvalid"));
        addTextValidation(cacheSizeTextField,
                n -> isEmptyOrSpaces(n) || !isInteger(n) || new BigInteger(n.trim()).signum() > 0,
                txt("msg.objects.error.SequenceCacheInvalid"));
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        super.applyFormChanges();
        input.setAttributeValue(SEQUENCE_START_WITH, getText(startWithTextField));
        input.setAttributeValue(SEQUENCE_INCREMENT_BY, getText(incrementByTextField));
        input.setAttributeValue(SEQUENCE_MIN_VALUE, getText(minValueTextField));
        input.setAttributeValue(SEQUENCE_MAX_VALUE, getText(maxValueTextField));
        input.setAttributeValue(SEQUENCE_CACHE_SIZE, getText(cacheSizeTextField));
        input.setAttributeValue(SEQUENCE_CYCLE, cycleCheckBox.isSelected());
        input.setIdentifierCase(getSelectedIdentifierCase());
    }

    private void addIntegerValidation(JTextField field, String labelKey) {
        addTextValidation(field,
                n -> isEmptyOrSpaces(n) || isInteger(n),
                txt("msg.objects.error.SequenceValueInvalid", txt(labelKey)));
    }

    private static boolean isInteger(String value) {
        return !isEmptyOrSpaces(value) && value.trim().matches("-?\\d+");
    }

    @Override
    protected DatabaseIdentifierCase getSelectedIdentifierCase() {
        return preserveCaseCheckBox.isSelected() ?
                DatabaseIdentifierCase.PRESERVE :
                getDefaultIdentifierCase();
    }

    @Override
    protected DBNComboBox<ConnectionHandler> getConnectionComboBox() {
        return connectionComboBox;
    }

    @Override
    protected DBNComboBox<SchemaId> getSchemaComboBox() {
        return schemaComboBox;
    }

    @Override
    protected JPanel getHeaderPanel() {
        return headerPanel;
    }

    @Override
    protected JTextField getNameTextField() {
        return nameTextField;
    }

    @Override
    public void resetFormChanges() {
        super.resetFormChanges();
        setText(startWithTextField, SEQUENCE_START_WITH.of(input));
        setText(incrementByTextField, SEQUENCE_INCREMENT_BY.of(input));
        setText(minValueTextField, SEQUENCE_MIN_VALUE.of(input));
        setText(maxValueTextField, SEQUENCE_MAX_VALUE.of(input));
        setText(cacheSizeTextField, SEQUENCE_CACHE_SIZE.of(input));
        cycleCheckBox.setSelected(SEQUENCE_CYCLE.is(input));
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}

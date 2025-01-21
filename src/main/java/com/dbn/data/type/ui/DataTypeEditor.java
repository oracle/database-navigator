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

package com.dbn.data.type.ui;

import com.dbn.code.common.style.options.CodeStyleCaseOption;
import com.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dbn.code.psql.style.PSQLCodeStyle;
import com.dbn.connection.ConnectionHandler;
import com.dbn.data.editor.ui.BasicListPopupValuesProvider;
import com.dbn.data.editor.ui.TextFieldWithPopup;
import com.dbn.data.type.DataTypeDefinition;

import javax.swing.JTextField;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

public class DataTypeEditor extends TextFieldWithPopup {
    public DataTypeEditor(ConnectionHandler connection) {
        super(connection.getProject());
        CodeStyleCaseSettings caseSettings = PSQLCodeStyle.caseSettings(getProject());
        CodeStyleCaseOption caseOption = caseSettings.getObjectCaseOption();

        List<DataTypeDefinition> nativeDataTypes = connection.getInterfaces().getNativeDataTypes().list();
        List<String> nativeDataTypeNames = new ArrayList<>();
        for (DataTypeDefinition nativeDataType : nativeDataTypes) {
            String typeName = nativeDataType.getName();
            typeName = caseOption.format(typeName);
            nativeDataTypeNames.add(typeName);
        }
        BasicListPopupValuesProvider valuesProvider = new BasicListPopupValuesProvider("Native Data Types", nativeDataTypeNames);
        createValuesListPopup(valuesProvider, null, true);
    }


    public String getDataTypeRepresentation() {
        return getTextField().getText();
    }

    @Override
    public void customizeTextField(JTextField textField) {
        Dimension preferredSize = textField.getPreferredSize();
        preferredSize = new Dimension(120, preferredSize.height);
        getTextField().setPreferredSize(preferredSize);
    }
}

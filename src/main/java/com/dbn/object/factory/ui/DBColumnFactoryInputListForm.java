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

package com.dbn.object.factory.ui;

import com.dbn.code.common.style.options.CodeStyleCaseOption;
import com.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dbn.code.psql.style.PSQLCodeStyle;
import com.dbn.common.ui.Presentable;
import com.dbn.common.util.Lists;
import com.dbn.data.type.DataTypeDefinition;
import com.dbn.object.factory.model.DBObjectAttribute;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.factory.model.DBObjectSpecList;
import com.dbn.object.factory.ui.common.DBObjectFactoryInputForm;
import com.dbn.object.factory.ui.common.DBObjectFactoryInputListForm;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.dbn.object.type.DBObjectType.COLUMN;

public class DBColumnFactoryInputListForm extends DBObjectFactoryInputListForm {
    @Getter(lazy = true)
    private final List<Presentable> objectDetailOptions = initObjectDetailOptions();

    public DBColumnFactoryInputListForm(DBTableFactoryInputForm parentForm) {
        super(parentForm);
    }

    @Override
    protected DBObjectSpecList<DBObjectSpec> getChildInputs() {
        return getTableInput().getChildren(COLUMN);
    }

    @Override
    protected DBObjectSpec createChildInput(Presentable detail) {
        DBObjectSpec tableSpec = getTableInput();

        DBObjectSpec columnSpec = new DBObjectSpec(COLUMN);
        columnSpec.setParent(tableSpec);

        String dataType = detail == null ? null : detail.getName();
        columnSpec.setAttribute(DBObjectAttribute.DATA_TYPE, dataType);
        return columnSpec;
    }

    private DBObjectSpec getTableInput() {
        DBTableFactoryInputForm tableInputForm = ensureParentComponent();
        return tableInputForm.getInput();
    }

    @Override
    public DBObjectFactoryInputForm<DBObjectSpec> createChildInputForm(DBObjectSpec input) {
        return new DBColumnFactoryInputForm(this, input);
    }

    @Override
    public DBObjectType getObjectType() {
        return COLUMN;
    }

    private @NotNull List<Presentable> initObjectDetailOptions() {
        List<DataTypeDefinition> nativeDataTypes = getConnection().getInterfaces().getNativeDataTypes().list();

        CodeStyleCaseSettings caseSettings = PSQLCodeStyle.caseSettings(getProject());
        CodeStyleCaseOption caseOption = caseSettings.getObjectCaseOption();

        return Lists.convert(nativeDataTypes, d -> Presentable.basic(caseOption.format(d.getName())));
    }
}

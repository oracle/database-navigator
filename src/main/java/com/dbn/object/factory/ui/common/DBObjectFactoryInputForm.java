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

package com.dbn.object.factory.ui.common;

import com.dbn.common.color.Colors;
import com.dbn.common.environment.options.EnvironmentSettings;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.object.factory.model.DBObjectAttribute;
import com.dbn.object.factory.model.DBObjectAttributeType;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JPanel;
import java.awt.Color;

import static com.dbn.nls.NlsResources.txt;

@Getter
@Setter
public abstract class DBObjectFactoryInputForm extends DBNFormBase {
    protected DBObjectSpec input;

    protected DBObjectFactoryInputForm(@NotNull DBNComponent parent, DBObjectSpec input) {
        super(parent);
        this.input = input;
    }

    public int getIndex() {
        return input.getIndex();
    }

    public DBObjectType getObjectType() {
        return input.getObjectType();
    }

    public boolean isReadonlyInput() {
        return input.isReadonly();
    }

    public boolean isReadonlyAttribute(DBObjectAttributeType<?> type) {
        if (isReadonlyInput()) return true;

        DBObjectAttribute<?> attribute = input.getAttribute(type);
        return attribute == null || attribute.isReadonly();
    }

    @NotNull
    @Override
    public abstract JPanel getMainComponent();

    @NotNull
    protected DBNHeaderForm createHeaderForm() {
        DBObjectType objectType = getObjectType();
        ConnectionHandler connection = getConnection();
        String headerTitle = buildHeaderTitle();
        Icon headerIcon = objectType.getIcon();

        Color headerBackground = Colors.getPanelBackground();
        EnvironmentSettings environmentSettings = getEnvironmentSettings(connection.getProject());
        if (environmentSettings.getVisibilitySettings().getDialogHeaders().value()) {
            headerBackground = connection.getEnvironmentType().getColor();
        }

        return new DBNHeaderForm(
                this,
                headerTitle,
                headerIcon,
                headerBackground);
    }

    protected String buildHeaderTitle() {
        String objectName = getObjectName();
        if (Strings.isEmpty(objectName)) {
            objectName = txt("app.object.placeholder.New");
        }
        return getSchemaName() + "." + objectName;
    }

    protected abstract String getSchemaName();

    protected abstract String getObjectName();

    @NotNull
    public ConnectionHandler getConnection() {
        return input.getConnection();
    }

    public abstract void focus();
}

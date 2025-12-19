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
import com.dbn.object.factory.model.DBObjectFactoryInput;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JPanel;
import java.awt.Color;

@Getter
@Setter
public abstract class ObjectFactoryInputForm<T extends DBObjectFactoryInput> extends DBNFormBase {
    protected T factoryInput;

    protected ObjectFactoryInputForm(@NotNull DBNComponent parent, T factoryInput) {
        super(parent);
        this.factoryInput = factoryInput;
    }

    public int getIndex() {
        return factoryInput.getIndex();
    }

    public void setIndex(int index) {
        factoryInput.setIndex(index);
    }

    public DBObjectType getObjectType() {
        return factoryInput.getObjectType();
    }

    public boolean isReadonly() {
        return factoryInput.isReadonly();
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
            objectName = "[new]";
        }
        return getSchemaName() + "." + objectName;
    }

    protected abstract String getSchemaName();

    protected abstract String getObjectName();

    @NotNull
    public ConnectionHandler getConnection() {
        return factoryInput.getConnection();
    }

    public final void restoreUserInput(@Nullable T input) {
        setFactoryInput(input);
        resetFormChanges();
        updateFieldAvailability();
        updateFieldAlignment();
    }

    public abstract void focus();
}

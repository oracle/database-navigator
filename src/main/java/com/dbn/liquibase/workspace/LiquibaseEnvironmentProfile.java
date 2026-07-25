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

package com.dbn.liquibase.workspace;

import com.dbn.common.environment.EnvironmentTypeId;
import com.dbn.common.index.Identifiable;
import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Cloneable;
import com.dbn.common.util.UUIDs;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.setBooleanAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Strings.isEmpty;

/**
 * Named Liquibase execution policy associated with a DBN environment type.
 */
@Getter
@Setter
public class LiquibaseEnvironmentProfile implements PersistentStateElement, Cloneable<LiquibaseEnvironmentProfile>, Identifiable<String> {
    private String id = UUIDs.regular();
    private String name;
    private EnvironmentTypeId environmentTypeId;
    private String contexts;
    private String labels;
    private boolean requireSqlPreview;
    private boolean allowDestructiveOperations = true;
    private boolean requireConfirmation = true;

    public LiquibaseEnvironmentProfile(@NotNull String name, @NotNull EnvironmentTypeId environmentTypeId) {
        this.name = name;
        this.environmentTypeId = environmentTypeId;
    }

    @Override
    public void readState(@NotNull Element element) {
        id = stringAttribute(element, "id", id);
        name = stringAttribute(element, "name", name);
        String environmentType = stringAttribute(element, "environment-type");
        if (!isEmpty(environmentType)) environmentTypeId = EnvironmentTypeId.get(environmentType);
        contexts = stringAttribute(element, "contexts", contexts);
        labels = stringAttribute(element, "labels", labels);
        requireSqlPreview = booleanAttribute(element, "require-sql-preview", requireSqlPreview);
        allowDestructiveOperations = booleanAttribute(element, "allow-destructive-operations", allowDestructiveOperations);
        requireConfirmation = booleanAttribute(element, "require-confirmation", requireConfirmation);
    }

    @Override
    public void writeState(@NotNull Element element) {
        setStringAttribute(element, "id", id);
        setStringAttribute(element, "name", name);
        setStringAttribute(element, "environment-type", environmentTypeId == null ? EnvironmentTypeId.DEFAULT.id() : environmentTypeId.id());
        setStringAttribute(element, "contexts", contexts);
        setStringAttribute(element, "labels", labels);
        setBooleanAttribute(element, "require-sql-preview", requireSqlPreview);
        setBooleanAttribute(element, "allow-destructive-operations", allowDestructiveOperations);
        setBooleanAttribute(element, "require-confirmation", requireConfirmation);
    }

    @Override
    @SneakyThrows
    public LiquibaseEnvironmentProfile clone() {
        return (LiquibaseEnvironmentProfile) super.clone();
    }
}

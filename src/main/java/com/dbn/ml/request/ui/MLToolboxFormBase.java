/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.ml.request.ui;

import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.connection.SchemaId;
import com.dbn.ml.request.MLRequest;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public abstract class MLToolboxFormBase extends DBNFormBase {
    private static final Pattern INTERNAL_SOURCE_TABLE_NAME = Pattern.compile(
            "(?:DM\\$.*|VECTOR\\$.*|ML_(?:STAGING|EXT|TRAIN|TEST|SETTINGS|APPLY|CM|ROC|LIFT|AI)_\\d{8}_\\d{6}(?:_(?:ACC|AUC))?)",
            Pattern.CASE_INSENSITIVE);

    public MLToolboxFormBase(@NotNull DBNComponent parent) {
        super(parent);
    }

    protected MLRequest getMLRequest() {
        MLToolboxForm rootForm = getToolboxForm();
        return rootForm.getMLRequest();
    }

    protected MLToolboxForm getToolboxForm() {
        return ensureParentFrom(MLToolboxForm.class);
    }

    protected static String getSelectedObjectName(DBNComboBox<? extends DBObject> comboBox, String defaultName) {
        DBObject selection = ComboBoxes.getSelection(comboBox);
        String objectName = getObjectName(selection);
        return objectName == null ? defaultName : objectName;
    }

    protected static @Nullable String getObjectName(@Nullable DBObject object) {
        return object == null ? null : object.getName();
    }

    protected List<DBSchema> loadSchemas() {
        DBObjectBundle objectBundle = requireConnection().getObjectBundle();
        return objectBundle.getUserSchemas();
    }

    protected List<DBTable> loadTables(DBSchema schema) {
        return schema == null ?
                Collections.emptyList() :
                schema.getTables().stream()
                        .filter(table -> !INTERNAL_SOURCE_TABLE_NAME.matcher(table.getName()).matches())
                        .toList();
    }

    public DBSchema getSelectedSchema() {
        return null;
    }

    public SchemaId getSelectedSchemaId() {
        DBSchema schema = getSelectedSchema();
        return schema == null ? null : schema.getSchemaId();
    }
}

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

package com.dbn.vector.search;

import com.dbn.common.options.setting.Settings;
import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Cloneable;
import com.dbn.data.model.sortable.SortableDataModelState;
import com.dbn.object.type.DBVectorDistanceMetric;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.object.type.DBVectorDistanceMetric.COSINE;

@Getter
@Setter
public class VectorSearchConsoleState implements FileEditorState, PersistentStateElement, Cloneable<VectorSearchConsoleState> {
    private SortableDataModelState modelState = new  SortableDataModelState();
    private String schemaName;
    private String tableName;
    private String searchText;
    private DBVectorDistanceMetric distanceMetric = COSINE;

    @Override
    public boolean canBeMergedWith(@NotNull FileEditorState fileEditorState, @NotNull FileEditorStateLevel fileEditorStateLevel) {
        return fileEditorState instanceof VectorSearchConsoleState && fileEditorStateLevel == FileEditorStateLevel.FULL;
    }

    @Override
    public void readState(@NotNull Element element) {
        schemaName = stringAttribute(element, "schema");
        tableName = stringAttribute(element, "table");
        distanceMetric = enumAttribute(element, "metric", COSINE);

        Element searchTextElement = element.getChild("search-text");
        searchText = Settings.readCdata(searchTextElement);

        Element resultModelElement = element.getChild("result-model");
        modelState.readState(resultModelElement);
    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "schema", schemaName);
        setStringAttribute(element, "table", tableName);
        setEnumAttribute(element, "metric", distanceMetric);

        Element searchTextElement = newElement(element, "search-text");
        Settings.writeCdata(searchTextElement, searchText);

        Element resultModelElement = newElement(element, "result-model");
        modelState.writeState(resultModelElement);
    }

    @Override
    @SneakyThrows
    public VectorSearchConsoleState clone() {
        VectorSearchConsoleState clone = cast(super.clone());
        clone.modelState = Cloneable.clone(modelState);

        return clone;
    }
}
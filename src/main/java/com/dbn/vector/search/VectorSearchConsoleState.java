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
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.util.Unsafe.cast;

@Getter
@Setter
public class VectorSearchConsoleState implements FileEditorState, PersistentStateElement, Cloneable<VectorSearchConsoleState> {
    private SortableDataModelState modelState = new  SortableDataModelState();
    private String searchText;

    @Override
    public boolean canBeMergedWith(@NotNull FileEditorState fileEditorState, @NotNull FileEditorStateLevel fileEditorStateLevel) {
        return fileEditorState instanceof VectorSearchConsoleState && fileEditorStateLevel == FileEditorStateLevel.FULL;
    }

    @Override
    public void readState(@NotNull Element element) {
        Element searchTextElement = element.getChild("search-text");
        searchText = Settings.readCdata(searchTextElement);

        Element resultModelElement = element.getChild("result-model");
        modelState.readState(resultModelElement);
/*
        rowCount = integerAttribute(element, "row-count", 100);
        readonly = booleanAttribute(element, "readonly", false);
        editorVisible = booleanAttribute(element, "editor-visible", false);
*/
    }

    @Override
    public void writeState(Element element) {
        Element searchTextElement = newElement(element, "search-text");
        Settings.writeCdata(searchTextElement, searchText);

        Element resultModelElement = newElement(element, "result-model");
        modelState.writeState(resultModelElement);
/*
        setIntegerAttribute(element, "row-count", rowCount);
        setBooleanAttribute(element, "readonly", readonly);
        setBooleanAttribute(element, "editor-visible", editorVisible);
*/
    }

    @Override
    @SneakyThrows
    public VectorSearchConsoleState clone() {
        VectorSearchConsoleState clone = cast(super.clone());
        clone.modelState = Cloneable.clone(modelState);

        return clone;
    }
}
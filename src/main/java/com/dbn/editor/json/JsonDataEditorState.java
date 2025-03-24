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

package com.dbn.editor.json;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Cloneable;
import com.dbn.data.model.sortable.SortableDataModelState;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.integerAttribute;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class JsonDataEditorState extends SortableDataModelState implements FileEditorState, PersistentStateElement, Cloneable<JsonDataEditorState> {
    public static final JsonDataEditorState VOID = new JsonDataEditorState();

    private boolean readonly;
    private int rowCount;

    @Override
    public boolean canBeMergedWith(@NotNull FileEditorState fileEditorState, @NotNull FileEditorStateLevel fileEditorStateLevel) {
        return fileEditorState instanceof JsonDataEditorState && fileEditorStateLevel == FileEditorStateLevel.FULL;
    }

    @Override
    public void readState(@NotNull Element element) {
        setRowCount(integerAttribute(element, "row-count", 100));
        setReadonly(booleanAttribute(element, "readonly", false));
    }

    @Override
    public void writeState(Element element) {
        element.setAttribute("row-count", Integer.toString(getRowCount()));
        element.setAttribute("readonly", Boolean.toString(isReadonly()));
    }

    @Override
    public JsonDataEditorState clone() {
        JsonDataEditorState clone = new JsonDataEditorState();
        clone.setReadonly(isReadonly());
        clone.setRowCount(getRowCount());
        return clone;
    }
}
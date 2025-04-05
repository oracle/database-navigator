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
import static com.dbn.common.options.setting.Settings.setBooleanAttribute;
import static com.dbn.common.options.setting.Settings.setIntegerAttribute;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class JsonDataEditorState extends SortableDataModelState implements FileEditorState, PersistentStateElement, Cloneable<JsonDataEditorState> {
    public static final JsonDataEditorState VOID = new JsonDataEditorState();

    private int rowCount;
    private boolean readonly;
    private boolean editorVisible;

    @Override
    public boolean canBeMergedWith(@NotNull FileEditorState fileEditorState, @NotNull FileEditorStateLevel fileEditorStateLevel) {
        return fileEditorState instanceof JsonDataEditorState && fileEditorStateLevel == FileEditorStateLevel.FULL;
    }

    @Override
    public void readState(@NotNull Element element) {
        rowCount = integerAttribute(element, "row-count", 100);
        readonly = booleanAttribute(element, "readonly", false);
        editorVisible = booleanAttribute(element, "editor-visible", false);
    }

    @Override
    public void writeState(Element element) {
        setIntegerAttribute(element, "row-count", rowCount);
        setBooleanAttribute(element, "readonly", readonly);
        setBooleanAttribute(element, "editor-visible", editorVisible);
    }

    @Override
    public JsonDataEditorState clone() {
        JsonDataEditorState clone = new JsonDataEditorState();
        clone.setReadonly(readonly);
        clone.setEditorVisible(editorVisible);
        clone.setRowCount(rowCount);
        return clone;
    }
}
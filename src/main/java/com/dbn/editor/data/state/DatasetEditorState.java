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

package com.dbn.editor.data.state;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Cloneable;
import com.dbn.data.model.sortable.SortableDataModelState;
import com.dbn.editor.data.state.column.DatasetColumnSetup;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.integerAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.stringAttribute;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class DatasetEditorState extends SortableDataModelState implements FileEditorState, PersistentStateElement, Cloneable<DatasetEditorState> {
    public static final DatasetEditorState VOID = new DatasetEditorState();

    private DatasetColumnSetup columnSetup = new DatasetColumnSetup();

    @Override
    public boolean canBeMergedWith(@NotNull FileEditorState fileEditorState, @NotNull FileEditorStateLevel fileEditorStateLevel) {
        return fileEditorState instanceof DatasetEditorState && fileEditorStateLevel == FileEditorStateLevel.FULL;
    }

    @Override
    public void readState(@NotNull Element element) {
        setRowCount(integerAttribute(element, "row-count", 100));
        setReadonly(booleanAttribute(element, "readonly", false));

        Element columnsElement = element.getChild("columns");
        columnSetup.readState(columnsElement);

        Element sortingElement = element.getChild("sorting");
        sortingState.readState(sortingElement);


        Element contentTypesElement = element.getChild("content-types");
        if (contentTypesElement != null) {
            for (Element child : contentTypesElement.getChildren()) {
                String columnName = stringAttribute(child, "column-name");
                String contentTypeName = stringAttribute(child, "type-name");
                setTextContentType(columnName, contentTypeName);
            }
        }
    }

    @Override
    public void writeState(Element element) {
        element.setAttribute("row-count", Integer.toString(getRowCount()));
        element.setAttribute("readonly", Boolean.toString(isReadonly()));

        Element columnsElement = newElement(element, "columns");
        columnSetup.writeState(columnsElement);

        Element sortingElement = newElement(element, "sorting");
        sortingState.writeState(sortingElement);

        Element contentTypesElement = newElement(element, "content-types");
        if (contentTypesMap != null && !contentTypesMap.isEmpty()) {
            for (val entry : contentTypesMap.entrySet()) {
                String columnName = entry.getKey();
                String contentTypeName = entry.getValue();

                Element contentTypeElement = newElement(contentTypesElement, "content-type");
                contentTypeElement.setAttribute("column-name", columnName);
                contentTypeElement.setAttribute("type-name", contentTypeName);
            }
        }
    }

    @Override
    public DatasetEditorState clone() {
        DatasetEditorState clone = new DatasetEditorState();
        clone.setReadonly(isReadonly());
        clone.setRowCount(getRowCount());
        clone.setSortingState(getSortingState());
        clone.columnSetup = columnSetup.clone();
        if (contentTypesMap != null) {
            clone.contentTypesMap = new HashMap<>(contentTypesMap);
        }

        return clone;
    }
}
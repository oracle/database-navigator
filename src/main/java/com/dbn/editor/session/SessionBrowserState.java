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

package com.dbn.editor.session;

import com.dbn.common.options.setting.Settings;
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

import java.util.HashMap;

import static com.dbn.common.options.setting.Settings.newElement;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class SessionBrowserState extends SortableDataModelState implements FileEditorState, PersistentStateElement, Cloneable<SessionBrowserState> {
    public static final SessionBrowserState VOID = new SessionBrowserState();

    private SessionBrowserFilter filterState = new SessionBrowserFilter();
    private int refreshInterval = 0;

    @Override
    public boolean canBeMergedWith(@NotNull FileEditorState fileEditorState, @NotNull FileEditorStateLevel fileEditorStateLevel) {
        return false;
    }

    @Override
    public void readState(@NotNull Element element) {
        refreshInterval = Settings.getInteger(element, "refresh-interval", refreshInterval);

        Element sortingElement = element.getChild("sorting");
        sortingState.readState(sortingElement);

        Element filterElement = element.getChild("filter");
        if (filterElement != null) {
            filterState.setFilterValue(SessionBrowserFilterType.USER, Settings.getString(filterElement, "user", null));
            filterState.setFilterValue(SessionBrowserFilterType.HOST, Settings.getString(filterElement, "host", null));
            filterState.setFilterValue(SessionBrowserFilterType.STATUS, Settings.getString(filterElement, "status", null));
        }
    }

    @Override
    public void writeState(Element element) {
        Settings.setInteger(element, "refresh-interval", refreshInterval);

        Element sortingElement = newElement(element, "sorting");
        sortingState.writeState(sortingElement);

        Element filterElement = newElement(element, "filter");
        Settings.setString(filterElement, "user", filterState.getFilterValue(SessionBrowserFilterType.USER));
        Settings.setString(filterElement, "host", filterState.getFilterValue(SessionBrowserFilterType.HOST));
        Settings.setString(filterElement, "status", filterState.getFilterValue(SessionBrowserFilterType.STATUS));
    }

    @Override
    public SessionBrowserState clone() {
        SessionBrowserState clone = new SessionBrowserState();
        clone.refreshInterval = refreshInterval;
        clone.setReadonly(isReadonly());
        clone.setRowCount(getRowCount());
        clone.setSortingState(getSortingState().clone());
        clone.filterState = filterState.clone();
        if (contentTypesMap != null) {
            clone.contentTypesMap = new HashMap<>(contentTypesMap);
        }

        return clone;
    }
}
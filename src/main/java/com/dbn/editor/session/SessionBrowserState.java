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
import lombok.SneakyThrows;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.util.Unsafe.cast;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class SessionBrowserState extends SortableDataModelState<SessionBrowserState> implements FileEditorState, PersistentStateElement {
    private SessionBrowserFilter filterState = new SessionBrowserFilter();
    private int refreshInterval = 0;

    @Override
    public boolean canBeMergedWith(@NotNull FileEditorState fileEditorState, @NotNull FileEditorStateLevel fileEditorStateLevel) {
        return false;
    }

    @Override
    public void readState(@NotNull Element element) {
        super.readState(element);
        refreshInterval = Settings.getInteger(element, "refresh-interval", refreshInterval);

        Element filterElement = element.getChild("filter");
        if (filterElement != null) {
            filterState.setFilterValue(SessionBrowserFilterType.USER, Settings.getString(filterElement, "user", null));
            filterState.setFilterValue(SessionBrowserFilterType.HOST, Settings.getString(filterElement, "host", null));
            filterState.setFilterValue(SessionBrowserFilterType.STATUS, Settings.getString(filterElement, "status", null));
        }
    }

    @Override
    public void writeState(Element element) {
        super.writeState(element);
        Settings.setInteger(element, "refresh-interval", refreshInterval);

        Element filterElement = newElement(element, "filter");
        Settings.setString(filterElement, "user", filterState.getFilterValue(SessionBrowserFilterType.USER));
        Settings.setString(filterElement, "host", filterState.getFilterValue(SessionBrowserFilterType.HOST));
        Settings.setString(filterElement, "status", filterState.getFilterValue(SessionBrowserFilterType.STATUS));
    }

    @Override
    @SneakyThrows
    public SessionBrowserState clone() {
        SessionBrowserState clone = cast(super.clone());
        clone.filterState = Cloneable.clone(filterState);
        clone.contentTypes = new HashMap<>(contentTypes);
        return clone;
    }
}
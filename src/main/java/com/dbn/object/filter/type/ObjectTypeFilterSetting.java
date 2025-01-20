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

package com.dbn.object.filter.type;

import com.dbn.common.ui.list.Selectable;
import com.dbn.object.type.DBObjectType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

import static com.dbn.common.util.Strings.cachedUpperCase;

@Getter
@Setter
@EqualsAndHashCode
public class ObjectTypeFilterSetting implements Selectable<ObjectTypeFilterSetting> {
    private transient ObjectTypeFilterSettings parent;

    private final DBObjectType objectType;
    private boolean selected = true;

    ObjectTypeFilterSetting(ObjectTypeFilterSettings parent, DBObjectType objectType) {
        this.parent = parent;
        this.objectType = objectType;
    }

    ObjectTypeFilterSetting(ObjectTypeFilterSettings parent, DBObjectType objectType, boolean selected) {
        this.parent = parent;
        this.objectType = objectType;
        this.selected = selected;
    }

    @Override
    public Icon getIcon() {
        return objectType.getIcon();
    }

    @Override
    public @NotNull String getName() {
        return cachedUpperCase(objectType.getName());
    }

    @Override
    public int compareTo(@NotNull ObjectTypeFilterSetting o) {
        return 0;
    }
}

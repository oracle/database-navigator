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

package com.dbn.vfs.file.status;

import com.dbn.common.property.Property;
import com.dbn.common.property.PropertyGroup;

public enum DBFileStatus implements Property.IntBase {
    LATEST(Group.CODE, true),
    MERGED(Group.CODE, false),
    OUTDATED(Group.CODE, false),

    MODIFIED,

    LOADING,
    SAVING,
    REFRESHING;

    public static final DBFileStatus[] VALUES = values();

    private final IntMasks masks = new IntMasks(this);
    private final boolean implicit;
    private final PropertyGroup group;

    DBFileStatus(PropertyGroup group, boolean implicit) {
        this.implicit = implicit;
        this.group = group;
    }

    DBFileStatus() {
        this(null, false);
    }

    @Override
    public IntMasks masks() {
        return masks;
    }

    @Override
    public PropertyGroup group() {
        return group;
    }

    @Override
    public boolean implicit() {
        return implicit;
    }

    public enum Group implements PropertyGroup{
        CODE
    }
}

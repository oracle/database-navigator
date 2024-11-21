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

package com.dbn.object.type;

import com.dbn.common.content.DynamicContentType;
import lombok.Getter;

@Getter
public enum DBObjectRelationType implements DynamicContentType<DBObjectRelationType> {
    CONSTRAINT_COLUMN(DBObjectType.CONSTRAINT, DBObjectType.COLUMN),
    INDEX_COLUMN(DBObjectType.INDEX, DBObjectType.COLUMN),
    USER_ROLE(DBObjectType.USER, DBObjectType.GRANTED_ROLE),
    USER_PRIVILEGE(DBObjectType.USER, DBObjectType.GRANTED_PRIVILEGE),
    ROLE_ROLE(DBObjectType.ROLE, DBObjectType.GRANTED_ROLE),
    ROLE_PRIVILEGE(DBObjectType.ROLE, DBObjectType.GRANTED_PRIVILEGE);

    private final DBObjectType sourceType;
    private final DBObjectType targetType;

    DBObjectRelationType(DBObjectType sourceType, DBObjectType targetType) {
        this.sourceType = sourceType;
        this.targetType = targetType;
    }

    @Override
    public boolean matches(DBObjectRelationType contentType) {
        return
            sourceType.matches(contentType.sourceType) &&
            targetType.matches(contentType.targetType);
    }
}

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

package com.dbn.common.operation;

import com.dbn.common.constant.Constant;
import lombok.Getter;

import static com.dbn.nls.NlsResources.txt;

@Getter
public enum DatabaseOperation implements Constant<DatabaseOperation> {

    CHANGE_JAVA_CODE(
            txt("app.shared.const.Operation_CHANGE_JAVA_CODE"),
            txt("msg.prerequisites.warning.MissingPrerequisites_CHANGE_JAVA_CODE")),

    EXECUTE_JAVA_CODE(
            txt("app.shared.const.Operation_EXECUTE_JAVA_CODE"),
            txt("msg.prerequisites.warning.MissingPrerequisites_EXECUTE_JAVA_CODE")),

    DEBUG_JAVA_CODE(
            txt("app.shared.const.Operation_DEBUG_JAVA_CODE"),
            txt("msg.prerequisites.warning.MissingPrerequisites_DEBUG_JAVA_CODE")),

    DEBUG_PLSQL_CODE_JDBC(
            txt("app.shared.const.Operation_DEBUG_PLSQL_CODE_JDBC"),
            txt("msg.prerequisites.warning.MissingPrerequisites_DEBUG_PLSQL_CODE_JDBC")),

    DEBUG_PLSQL_CODE_JDWP(
            txt("app.shared.const.Operation_DEBUG_PLSQL_CODE_JDWP"),
            txt("msg.prerequisites.warning.MissingPrerequisites_DEBUG_PLSQL_CODE_JDWP")),

    ENABLE_DATABASE_CHANGE_NOTIFICATION(
            txt("app.shared.const.Operation_ENABLE_DATABASE_CHANGE_NOTIFICATION"),
            txt("msg.prerequisites.warning.MissingPrerequisites_ENABLE_DATABASE_CHANGE_NOTIFICATION")),

    ;

    private final String name;
    //private final String description;
    private final String missingPrerequisiteMessage;

    DatabaseOperation(String name, /*String description, */String missingPrerequisiteMessage) {
        this.name = name;
        //this.description = description;
        this.missingPrerequisiteMessage = missingPrerequisiteMessage;
    }

    @Override
    public String toString() {
        return name;
    }
}

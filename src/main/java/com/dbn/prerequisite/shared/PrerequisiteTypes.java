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

package com.dbn.prerequisite.shared;

import com.dbn.prerequisite.model.PrerequisiteType;

public class PrerequisiteTypes {

    // shared prerequisites
    public static final PrerequisiteType CREATE_TABLE = PrerequisiteType.get("CREATE_TABLE");
    public static final PrerequisiteType CREATE_PROCEDURE = PrerequisiteType.get("CREATE_PROCEDURE");
    public static final PrerequisiteType CREATE_TYPE = PrerequisiteType.get("CREATE_TYPE");
    public static final PrerequisiteType CREATE_ANY_TABLE = PrerequisiteType.get("CREATE_ANY_TABLE");
    public static final PrerequisiteType CREATE_ANY_PROCEDURE = PrerequisiteType.get("CREATE_ANY_PROCEDURE");
    public static final PrerequisiteType CREATE_ANY_TYPE = PrerequisiteType.get("CREATE_ANY_TYPE");
    public static final PrerequisiteType TABLESPACE_QUOTA = PrerequisiteType.get("TABLESPACE_QUOTA");
    public static final PrerequisiteType TABLESPACE_QUOTA_UNLIMITED = PrerequisiteType.get("TABLESPACE_QUOTA_UNLIMITED");

    // debugger prerequisites
    public static final PrerequisiteType EXECUTE_DBMS_DEBUG = PrerequisiteType.get("EXECUTE_DBMS_DEBUG");
    public static final PrerequisiteType EXECUTE_DBMS_DEBUG_JDWP = PrerequisiteType.get("EXECUTE_DBMS_DEBUG_JDWP");
    public static final PrerequisiteType DEBUG_ANY_PROCEDURE = PrerequisiteType.get("DEBUG_ANY_PROCEDURE");
    public static final PrerequisiteType DEBUG_CONNECT_SESSION = PrerequisiteType.get("DEBUG_CONNECT_SESSION");
    public static final PrerequisiteType HOST_ACE_JDWP = PrerequisiteType.get("HOST_ACE_JDWP");

    // change notification prerequisites
    public static final PrerequisiteType EXECUTE_DBMS_CHANGE_NOTIFICATION = PrerequisiteType.get("EXECUTE_DBMS_CHANGE_NOTIFICATION");
    public static final PrerequisiteType CHANGE_NOTIFICATION = PrerequisiteType.get("CHANGE_NOTIFICATION");
}

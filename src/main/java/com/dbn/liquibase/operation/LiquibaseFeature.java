/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.liquibase.operation;

/** Describes a capability or required input of a Liquibase operation. */
public enum LiquibaseFeature {
    SOURCE_SCHEMA,
    TARGET_SCHEMA,
    WORKSPACE,
    WORKSPACE_CREATION,
    SNAPSHOT_ITEMS,
    CHANGELOG_AUTHOR,
    DATABASE_TAG,
    CHANGELOG_TAG,
    CHECKPOINT_TAG,
    UPDATE_INSTRUCTION,
    ROLLBACK_TAG,
    ROLLBACK,
    CHANGESET_ITEMS,
    LOCK_ITEMS,
    RERUN_ON_SUCCESS,
    SQL_OUTPUT,
    COMPARISON_ITEMS,
    TRACKING_TABLES,
    DISTINCT_SCHEMAS
}

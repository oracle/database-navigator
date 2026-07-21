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

package com.dbn.liquibase.execution;

import liquibase.command.core.CalculateChecksumCommandStep;
import liquibase.command.core.ChangelogSyncCommandStep;
import liquibase.command.core.ChangelogSyncSqlCommandStep;
import liquibase.command.core.ClearChecksumsCommandStep;
import liquibase.command.core.DiffChangelogCommandStep;
import liquibase.command.core.DropAllCommandStep;
import liquibase.command.core.FutureRollbackSqlCommandStep;
import liquibase.command.core.GenerateChangelogCommandStep;
import liquibase.command.core.HistoryCommandStep;
import liquibase.command.core.ListLocksCommandStep;
import liquibase.command.core.MarkNextChangesetRanCommandStep;
import liquibase.command.core.ReleaseLocksCommandStep;
import liquibase.command.core.RollbackCommandStep;
import liquibase.command.core.RollbackCountCommandStep;
import liquibase.command.core.RollbackCountSqlCommandStep;
import liquibase.command.core.RollbackSqlCommandStep;
import liquibase.command.core.RollbackToDateCommandStep;
import liquibase.command.core.RollbackToDateSqlCommandStep;
import liquibase.command.core.SnapshotCommandStep;
import liquibase.command.core.StatusCommandStep;
import liquibase.command.core.TagCommandStep;
import liquibase.command.core.UnexpectedChangesetsCommandStep;
import liquibase.command.core.UpdateCommandStep;
import liquibase.command.core.UpdateCountCommandStep;
import liquibase.command.core.UpdateSqlCommandStep;
import liquibase.command.core.UpdateToTagCommandStep;
import liquibase.command.core.ValidateCommandStep;
import lombok.experimental.UtilityClass;

/** CommandScope names exposed by the Liquibase command-step implementations. */
@UtilityClass
public class LiquibaseCommands {
    public static final Object NULL_ARGUMENT = new Object();
    public static final String GENERATE_CHANGELOG = GenerateChangelogCommandStep.COMMAND_NAME[0];
    public static final String SNAPSHOT_DATABASE = SnapshotCommandStep.COMMAND_NAME[0];
    public static final String GENERATE_DIFF_CHANGELOG = DiffChangelogCommandStep.COMMAND_NAME[0];
    public static final String VALIDATE_CHANGELOG = ValidateCommandStep.COMMAND_NAME[0];
    public static final String SHOW_CHANGELOG_STATUS = StatusCommandStep.COMMAND_NAME[0];
    public static final String SHOW_CHANGELOG_HISTORY = HistoryCommandStep.COMMAND_NAME[0];
    public static final String UNEXPECTED_CHANGESETS = UnexpectedChangesetsCommandStep.COMMAND_NAME[0];
    public static final String TAG = TagCommandStep.COMMAND_NAME[0];
    public static final String MARK_NEXT_CHANGESET_RAN = MarkNextChangesetRanCommandStep.COMMAND_NAME[0];
    public static final String RELEASE_LOCKS = ReleaseLocksCommandStep.COMMAND_NAME[0];
    public static final String CLEAR_CHECKSUMS = ClearChecksumsCommandStep.COMMAND_NAME[0];
    public static final String LIST_LOCKS = ListLocksCommandStep.COMMAND_NAME[0];
    public static final String CALCULATE_CHECKSUM = CalculateChecksumCommandStep.COMMAND_NAME[0];
    public static final String DROP_ALL = DropAllCommandStep.COMMAND_NAME[0];
    public static final String UPDATE_DATABASE = UpdateCommandStep.COMMAND_NAME[0];
    public static final String UPDATE_COUNT = UpdateCountCommandStep.COMMAND_NAME[0];
    public static final String UPDATE_TO_TAG = UpdateToTagCommandStep.COMMAND_NAME[0];
    public static final String UPDATE_SQL = UpdateSqlCommandStep.COMMAND_NAME[0];
    public static final String SYNCHRONIZE_CHANGELOG = ChangelogSyncCommandStep.COMMAND_NAME[0];
    public static final String SYNCHRONIZE_CHANGELOG_SQL = ChangelogSyncSqlCommandStep.COMMAND_NAME[0];
    public static final String ROLLBACK_COUNT = RollbackCountCommandStep.COMMAND_NAME[0];
    public static final String ROLLBACK_TAG = RollbackCommandStep.COMMAND_NAME[0];
    public static final String ROLLBACK_DATE = RollbackToDateCommandStep.COMMAND_NAME[0];
    public static final String ROLLBACK_COUNT_SQL = RollbackCountSqlCommandStep.COMMAND_NAME[0];
    public static final String ROLLBACK_TAG_SQL = RollbackSqlCommandStep.COMMAND_NAME[0];
    public static final String ROLLBACK_DATE_SQL = RollbackToDateSqlCommandStep.COMMAND_NAME[0];
    public static final String FUTURE_ROLLBACK_SQL = FutureRollbackSqlCommandStep.COMMAND_NAME[0];
}

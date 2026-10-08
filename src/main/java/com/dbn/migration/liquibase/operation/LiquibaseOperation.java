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

package com.dbn.migration.liquibase.operation;

import com.dbn.common.constant.Constant;
import com.dbn.common.icon.Icons;
import com.dbn.migration.liquibase.task.LiquibaseTask;
import com.dbn.migration.shared.operation.DatabaseMigrationOperation;
import com.intellij.icons.AllIcons;
import lombok.Getter;
import lombok.experimental.Delegate;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import static com.dbn.migration.liquibase.operation.LiquibaseOperationCategory.CHANGELOG;
import static com.dbn.migration.liquibase.operation.LiquibaseOperationCategory.DEPLOYMENT;
import static com.dbn.migration.liquibase.operation.LiquibaseOperationCategory.INSPECTION;
import static com.dbn.migration.liquibase.operation.LiquibaseOperationCategory.MAINTENANCE;
import static com.dbn.migration.liquibase.operation.LiquibaseOperationCategory.OTHER;
import static com.dbn.migration.liquibase.operation.LiquibaseOperationCategory.SQL_PREVIEW;
import static com.dbn.nls.NlsResources.txt;

/**
 * Liquibase operation represented in the DBN execution console.
 *
 * <p>Each operation defines its category, input requirements, safety classification, and
 * presentation metadata.</p>
 */
@Getter
public enum LiquibaseOperation implements Constant<LiquibaseOperation>, LiquibaseTask, DatabaseMigrationOperation {
    GENERATE_CHANGELOG(CHANGELOG),
    GENERATE_DATABASE_DOCUMENTATION(INSPECTION),
    SNAPSHOT_DATABASE(INSPECTION),
    VALIDATE_CHANGELOG(CHANGELOG),
    COMPARE_SCHEMAS(INSPECTION),
    GENERATE_DIFF_CHANGELOG(CHANGELOG),
    SHOW_CHANGELOG_STATUS(INSPECTION),
    SHOW_CHANGELOG_HISTORY(INSPECTION),
    UNEXPECTED_CHANGESETS(INSPECTION),
    SYNCHRONIZE_CHANGELOG(MAINTENANCE),
    SYNCHRONIZE_CHANGELOG_TO_TAG(MAINTENANCE),
    SYNCHRONIZE_CHANGELOG_SQL(SQL_PREVIEW),
    UPDATE_DATABASE(DEPLOYMENT),
    UPDATE_TESTING_ROLLBACK(DEPLOYMENT),
    UPDATE_SQL(SQL_PREVIEW),
    FUTURE_ROLLBACK(SQL_PREVIEW),
    TAG_DATABASE(DEPLOYMENT),
    MARK_NEXT_CHANGESET_RAN(MAINTENANCE),
    RELEASE_LOCKS(MAINTENANCE),
    CLEAR_CHECKSUMS(MAINTENANCE),
    LIST_LOCKS(MAINTENANCE),
    CALCULATE_CHECKSUMS(MAINTENANCE),
    DROP_ALL(OTHER),
    ROLLBACK_CHANGESETS(DEPLOYMENT),
    ROLLBACK_SQL(SQL_PREVIEW);

    @Getter
    private final LiquibaseOperationCategory category;
    @Delegate
    private final LiquibaseOperationSupport support = new LiquibaseOperationSupport(this);

    LiquibaseOperation(LiquibaseOperationCategory category) {
        this.category = category;
    }

    public String getName() {
        return txt("app.liquibase.const.Operation_" + name());
    }

    public String getDescription() {
        return txt("app.liquibase.text.OperationDescription_" + name());
    }

    public boolean isDestructive() {
        return isOneOf(
                DROP_ALL,
                ROLLBACK_CHANGESETS,
                UPDATE_TESTING_ROLLBACK);
    }

    public boolean isMutating() {
        return isMutatingSchema() || isMutatingChangelog();
    }

    public boolean isMutatingSchema() {
        return isOneOf(
                DROP_ALL,
                UPDATE_DATABASE,
                UPDATE_TESTING_ROLLBACK,
                ROLLBACK_CHANGESETS);
    }

    public boolean isMutatingChangelog() {
        return isOneOf(
                TAG_DATABASE,
                MARK_NEXT_CHANGESET_RAN,
                RELEASE_LOCKS,
                CLEAR_CHECKSUMS,
                CALCULATE_CHECKSUMS,
                SYNCHRONIZE_CHANGELOG,
                SYNCHRONIZE_CHANGELOG_TO_TAG);
    }

    public String getHint() {
        return txt("app.liquibase.hint.Operation_" + name());
    }

    @Override
    public String getDashboardName() {
        return getName();
    }

    @Override
    public String getDashboardDescription() {
        return getHint();
    }

    public String getDocumentationUrl() {
        return txt("app.liquibase.url.Operation_" + name());
    }

    @Override
    public String getDashboardDocumentationUrl() {
        return getDocumentationUrl();
    }

    @Nullable
    public Icon getActionIcon() {
        return switch (this) {
            case GENERATE_CHANGELOG -> Icons.ACTION_DOWNLOAD;
            case GENERATE_DATABASE_DOCUMENTATION -> AllIcons.Toolwindows.Documentation;
            case GENERATE_DIFF_CHANGELOG -> AllIcons.Actions.Diff;
            case UPDATE_DATABASE -> Icons.ACTION_UPLOAD;
            case ROLLBACK_CHANGESETS -> Icons.ACTION_REVERT;
            case DROP_ALL -> Icons.ACTION_DELETE;
            default -> null;
        };
    }
}

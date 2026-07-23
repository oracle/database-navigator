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

import com.dbn.common.constant.Constant;
import com.dbn.common.icon.Icons;
import com.dbn.liquibase.LiquibaseDashboardItem;
import com.intellij.icons.AllIcons;
import lombok.Getter;
import lombok.experimental.Delegate;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import static com.dbn.nls.NlsResources.txt;

/** Liquibase operation represented in the DBN execution console. */
@Getter
public enum LiquibaseOperation implements Constant<LiquibaseOperation>, LiquibaseDashboardItem {
    GENERATE_CHANGELOG,
    GENERATE_DATABASE_DOCUMENTATION,
    SNAPSHOT_DATABASE,
    VALIDATE_CHANGELOG,
    COMPARE_SCHEMAS,
    GENERATE_DIFF_CHANGELOG,
    SHOW_CHANGELOG_STATUS,
    SHOW_CHANGELOG_HISTORY,
    UNEXPECTED_CHANGESETS,
    SYNCHRONIZE_CHANGELOG,
    SYNCHRONIZE_CHANGELOG_TO_TAG,
    SYNCHRONIZE_CHANGELOG_SQL,
    UPDATE_DATABASE,
    UPDATE_TESTING_ROLLBACK,
    UPDATE_SQL,
    FUTURE_ROLLBACK,
    TAG_DATABASE,
    MARK_NEXT_CHANGESET_RAN,
    RELEASE_LOCKS,
    CLEAR_CHECKSUMS,
    LIST_LOCKS,
    CALCULATE_CHECKSUMS,
    DROP_ALL,
    ROLLBACK_CHANGESETS,
    ROLLBACK_SQL;

    @Delegate
    private final LiquibaseOperationSupport support = new LiquibaseOperationSupport(this);

    public String getName() {
        return txt("app.liquibase.const.Operation_" + name());
    }

    public String getDescription() {
        return txt("app.liquibase.text.OperationDescription_" + name());
    }

    public String getHint() {
        return /*txt("app.liquibase.title.Operation_" + name()) + "\n\n" +*/ txt("app.liquibase.hint.Operation_" + name());
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

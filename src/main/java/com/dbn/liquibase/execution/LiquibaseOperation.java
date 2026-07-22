package com.dbn.liquibase.execution;

import com.dbn.common.constant.Constant;
import com.dbn.common.icon.Icons;
import com.intellij.icons.AllIcons;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import static com.dbn.nls.NlsResources.txt;

/** Liquibase operation represented in the DBN execution console. */
@Getter
public enum LiquibaseOperation implements Constant<LiquibaseOperation> {
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

    private final LiquibaseOperationSupport support = new LiquibaseOperationSupport(this);

    public String getName() {
        return txt("cfg.liquibase.const.Operation_" + name());
    }

    public String getDescription() {
        return txt("cfg.liquibase.text.OperationDescription_" + name());
    }

    public String getHint() {
        return /*txt("cfg.liquibase.title.Operation_" + name()) + "\n\n" +*/ txt("cfg.liquibase.hint.Operation_" + name());
    }

    public String getDocumentationUrl() {
        return txt("app.liquibase.url.Operation_" + name());
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

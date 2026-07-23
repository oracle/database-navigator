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

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

import static com.dbn.liquibase.execution.LiquibaseCommands.ROLLBACK_COUNT;
import static com.dbn.liquibase.execution.LiquibaseCommands.ROLLBACK_COUNT_SQL;
import static com.dbn.liquibase.execution.LiquibaseCommands.ROLLBACK_DATE;
import static com.dbn.liquibase.execution.LiquibaseCommands.ROLLBACK_DATE_SQL;
import static com.dbn.liquibase.execution.LiquibaseCommands.ROLLBACK_TAG;
import static com.dbn.liquibase.execution.LiquibaseCommands.ROLLBACK_TAG_SQL;

/** User-selected criterion and value for a Liquibase rollback operation. */
@Getter
@Setter
public class LiquibaseRollbackInstruction {
    private LiquibaseRollbackType type = LiquibaseRollbackType.COUNT;
    private int count = 1;
    private String tag;
    private Date date;

    @NotNull
    public String getCommand() {
        return switch (type) {
            case COUNT -> ROLLBACK_COUNT;
            case TAG -> ROLLBACK_TAG;
            case DATE -> ROLLBACK_DATE;
        };
    }

    @NotNull
    public String getSqlCommand() {
        return switch (type) {
            case COUNT -> ROLLBACK_COUNT_SQL;
            case TAG -> ROLLBACK_TAG_SQL;
            case DATE -> ROLLBACK_DATE_SQL;
        };
    }

    @NotNull
    public String getParameter() {
        return switch (type) {
            case COUNT -> "count";
            case TAG -> "tag";
            case DATE -> "date";
        };
    }

    @NotNull
    public Object getValue() {
        return switch (type) {
            case COUNT -> count;
            case TAG -> tag;
            case DATE -> date;
        };
    }

    public void copyFrom(@NotNull LiquibaseRollbackInstruction instruction) {
        type = instruction.type;
        count = instruction.count;
        tag = instruction.tag;
        date = instruction.date == null ? null : (Date) instruction.date.clone();
    }
}

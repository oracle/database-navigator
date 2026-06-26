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

package com.dbn.connection.config.datasource.prerequisite;

import com.dbn.common.operation.DatabaseOperation;
import com.dbn.prerequisite.evaluation.PrerequisiteRequirementEvaluatorBase;
import com.dbn.prerequisite.model.PrerequisiteMandate;

import java.util.List;

import static com.dbn.common.operation.DatabaseOperation.MANAGE_DATA_SOURCE_CONFIG_ENTRIES;
import static com.dbn.prerequisite.shared.PrerequisiteTypes.CREATE_DATA_SOURCE_CONFIG;
import static com.dbn.prerequisite.shared.PrerequisiteTypes.DATABASE_VERSION_26_0;
import static com.dbn.prerequisite.shared.PrerequisiteTypes.EXECUTE_DBMS_DATA_SOURCE_CONFIG;

public class DataSourceConfigPrerequisitesEvaluator extends PrerequisiteRequirementEvaluatorBase {

    @Override
    public boolean supports(DatabaseOperation operation) {
        return operation == MANAGE_DATA_SOURCE_CONFIG_ENTRIES;
    }

    @Override
    protected void createMandates(List<PrerequisiteMandate> mandates, DatabaseOperation operation) {
        createMandate(mandates, DATABASE_VERSION_26_0, "Configuration entries require Oracle Database 26.0 or later");
        createMandate(mandates, CREATE_DATA_SOURCE_CONFIG, "Requires CREATE DATA SOURCE CONFIG, or CREATE ANY DATA SOURCE CONFIG for administrative users");
        createMandate(mandates, EXECUTE_DBMS_DATA_SOURCE_CONFIG, "Allows the user to create, update, and delete configuration entries through SYS.DBMS_DATA_SOURCE_CONFIG");
    }
}

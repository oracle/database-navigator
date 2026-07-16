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

import static com.dbn.common.operation.DatabaseOperation.CREATE_DATASOURCE_CONFIG;
import static com.dbn.common.operation.DatabaseOperation.MANAGE_DATASOURCE_CONFIGS;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.prerequisite.shared.PrerequisiteTypes.CREATE_DATA_SOURCE_CONFIG;
import static com.dbn.prerequisite.shared.PrerequisiteTypes.DATABASE_VERSION_26_0;
import static com.dbn.prerequisite.shared.PrerequisiteTypes.EXECUTE_DBMS_DATA_SOURCE_CONFIG;

public class DatasourceConfigPrerequisitesEvaluator extends PrerequisiteRequirementEvaluatorBase {

    @Override
    public boolean supports(DatabaseOperation operation) {
        return operation == CREATE_DATASOURCE_CONFIG || operation == MANAGE_DATASOURCE_CONFIGS;
    }

    @Override
    protected void createMandates(List<PrerequisiteMandate> mandates, DatabaseOperation operation) {
        createMandate(mandates, DATABASE_VERSION_26_0, txt("msg.prerequisite.text.Reason_DATABASE_VERSION_26_0"));
        createMandate(mandates, EXECUTE_DBMS_DATA_SOURCE_CONFIG, txt("msg.prerequisite.text.Reason_EXECUTE_DBMS_DATA_SOURCE_CONFIG"));

        if (operation == CREATE_DATASOURCE_CONFIG) {
            createMandate(mandates, CREATE_DATA_SOURCE_CONFIG, txt("msg.prerequisite.text.Reason_CREATE_DATA_SOURCE_CONFIG"));
        }
    }
}

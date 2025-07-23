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

package com.dbn.event.prerequisite;

import com.dbn.common.operation.DatabaseOperation;
import com.dbn.connection.context.DatabaseContext;
import com.dbn.prerequisite.evaluation.PrerequisiteRequirementEvaluator;
import com.dbn.prerequisite.model.PrerequisiteMandate;
import com.dbn.prerequisite.model.PrerequisiteType;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.operation.DatabaseOperation.ENABLE_DATABASE_CHANGE_NOTIFICATION;
import static com.dbn.event.prerequisite.ChangeNotificationPrerequisiteTypes.CHANGE_NOTIFICATION;
import static com.dbn.event.prerequisite.ChangeNotificationPrerequisiteTypes.EXECUTE_DBMS_CHANGE_NOTIFICATION;

public class ChangeNotificationPrerequisitesEvaluator implements PrerequisiteRequirementEvaluator {
    @Override
    public boolean supports(DatabaseOperation operation) {
        return operation == ENABLE_DATABASE_CHANGE_NOTIFICATION;
    }

    @Override
    public List<PrerequisiteMandate> resolvePrerequisites(DatabaseContext context, DatabaseOperation operation) {
        List<PrerequisiteMandate> mandates = new ArrayList<>();

        createMandate(mandates, CHANGE_NOTIFICATION, "Allows the user to receive database change notifications");
        createMandate(mandates, EXECUTE_DBMS_CHANGE_NOTIFICATION, "Allows the user to execute procedures and functions of the SYS.DBMS_CHANGE_NOTIFICATION package, which provides ability to enable and disable database change notifications");

        return mandates;
    }

    private static void createMandate(List<PrerequisiteMandate> mandates, PrerequisiteType type, String reason) {
        mandates.add(new PrerequisiteMandate(type, reason));
    }
}

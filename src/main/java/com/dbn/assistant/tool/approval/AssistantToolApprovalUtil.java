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

package com.dbn.assistant.tool.approval;

import com.dbn.common.ui.misc.DBNToggleButton;
import lombok.experimental.UtilityClass;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.dbn.common.ui.misc.DBNToggleButton.getDefaultForeground;
import static com.dbn.common.ui.misc.DBNToggleButton.getErrorForeground;
import static com.dbn.common.ui.misc.DBNToggleButton.getSuccessForeground;

@UtilityClass
public class AssistantToolApprovalUtil {

    public static void initStatusToggle(
            DBNToggleButton<AssistantToolApprovalStatus> statusToggle,
            AssistantToolApprovalStatus[] approvalStatuses,
            Supplier<AssistantToolApprovalStatus> current,
            Consumer<AssistantToolApprovalStatus> selected) {

        statusToggle.setTextColor(s ->
                switch (s) {
                    case PROMPTED -> getDefaultForeground();
                    case APPROVED -> getSuccessForeground();
                    case BLOCKED -> getErrorForeground();
                });

        statusToggle.setValues(approvalStatuses);
        statusToggle.setSelectedValue(current.get());
        statusToggle.addListener((os, ns) -> selected.accept(ns));
    }
}

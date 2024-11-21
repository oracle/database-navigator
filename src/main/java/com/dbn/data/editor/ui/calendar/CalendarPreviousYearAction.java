/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.data.editor.ui.calendar;

import com.dbn.common.icon.Icons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class CalendarPreviousYearAction extends CalendarPopupAction {
    CalendarPreviousYearAction() {
        super("Previous Year", null, Icons.CALENDAR_PREVIOUS_YEAR);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        CalendarTableModel model = getCalendarTableModel(e);
        if (model == null) return;

        model.rollYear(-1);
    }
}

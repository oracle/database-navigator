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

import java.util.Calendar;
import java.util.GregorianCalendar;

import static com.dbn.nls.NlsResources.txt;

class CalendarResetTimeAction extends CalendarPopupAction {
    CalendarResetTimeAction() {
        super(txt("app.data.action.ResetTime"), null, Icons.CALENDAR_CLEAR_TIME);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        CalendarPopupProviderForm form = getCalendarForm(e);
        if (form == null) return;

        Calendar calendar = new GregorianCalendar(2000, Calendar.JANUARY, 1, 0, 0, 0);
        String timeString = form.getFormatter().formatTime(calendar.getTime());
        form.setTimeText(timeString);
    }
}

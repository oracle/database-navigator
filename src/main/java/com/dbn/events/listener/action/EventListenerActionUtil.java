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

package com.dbn.events.listener.action;

import com.dbn.common.action.DataKeys;
import com.dbn.events.listener.ui.EventListenersForm;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class EventListenerActionUtil {
    @Nullable
    static EventListenersForm getListenersForm(DataContext dataContext) {
        return DataKeys.EVENT_LISTENERS_FORM.getData(dataContext);
    }

    @Nullable
    static EventListenersForm getListenersForm(AnActionEvent e) {
        return getListenersForm(e.getDataContext());
    }
}

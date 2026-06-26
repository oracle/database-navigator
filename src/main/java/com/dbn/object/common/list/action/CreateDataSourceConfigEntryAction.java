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

package com.dbn.object.common.list.action;

import com.dbn.common.action.BasicAction;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.config.datasource.ui.CreateDataSourceConfigEntryDialog;
import com.dbn.object.common.list.DBObjectList;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.operation.DatabaseOperation.MANAGE_DATA_SOURCE_CONFIG_ENTRIES;
import static com.dbn.nls.NlsResources.txt;

public class CreateDataSourceConfigEntryAction extends BasicAction {
    private final WeakRef<DBObjectList> objectList;

    CreateDataSourceConfigEntryAction(DBObjectList objectList) {
        super(txt("app.objects.action.NewObject", objectList.getObjectType().getTitleCasedDisplayName()));
        this.objectList = WeakRef.of(objectList);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        DBObjectList objectList = getObjectList();
        MANAGE_DATA_SOURCE_CONFIG_ENTRIES.start(
                objectList.getConnection(),
                () -> Dialogs.show(() -> new CreateDataSourceConfigEntryDialog(objectList.getConnection())));
    }

    private @NotNull DBObjectList getObjectList() {
        return Failsafe.nn(WeakRef.get(objectList));
    }
}

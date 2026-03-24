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

package com.dbn.object.action;

import com.dbn.common.action.BasicAction;
import com.dbn.common.util.Dialogs;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.ui.DBObjectSelectionDialog;
import com.dbn.object.common.ui.DBObjectSelectionInput;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class ObjectSelectAction<T extends DBObject> extends BasicAction {
    private final DBObjectSelectionInput<T> input;
    private final Consumer<T> callback;

    public ObjectSelectAction(String text, DBObjectSelectionInput<T> input, Consumer<T> callback) {
        super(text);
        this.input = input;
        this.callback = callback;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Dialogs.show(() -> new DBObjectSelectionDialog<T>(input, callback));
    }
}

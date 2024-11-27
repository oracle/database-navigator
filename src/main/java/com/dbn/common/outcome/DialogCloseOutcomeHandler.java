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

package com.dbn.common.outcome;

import com.dbn.common.Priority;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.thread.Dispatch;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

/**
 * Generic implementation of an {@link OutcomeHandler} that closes a given dialog with exit code 0
 * (holds a soft reference to the dialog to avoid memory leaks if not disposed properly)
 *
 * @author Dan Cioca (Oracle)
 */
public class DialogCloseOutcomeHandler implements OutcomeHandler {
    private final WeakRef<DialogWrapper> dialog;

    private DialogCloseOutcomeHandler(DialogWrapper dialog) {
        this.dialog = WeakRef.of(dialog);
    }

    public static OutcomeHandler create(DialogWrapper dialog) {
        return new DialogCloseOutcomeHandler(dialog);
    }

    @Override
    public void handle(Outcome outcome) {
        DialogWrapper dialog = getDialog();
        if (dialog == null) return;

        Dispatch.run(true, () -> dialog.close(0));
    }

    @Override
    public Priority getPriority() {
        return Priority.HIGH;
    }

    @Nullable
    private DialogWrapper getDialog() {
        return WeakRef.get(dialog);
    }
}

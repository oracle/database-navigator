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

package com.dbn.common.util;

import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.dialog.DBNDialog;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.ui.progress.ProgressDialogHandler.closeProgressDialogs;

@UtilityClass
public class Dialogs {

    public static <T extends DBNDialog<?>> void show(@NotNull Supplier<T> builder) {
        show(builder, null);
    }

    public static <T extends DBNDialog<?>> void show(@NotNull Supplier<T> builder, @Nullable DialogCallback<T> callback) {
        Dispatch.run(true, () -> {
            closeProgressDialogs();
            T dialog = builder.get();
            dialog.setDialogCallback(callback);
            dialog.show();
        });
    }

    public static <T extends DBNDialog> void close(@Nullable T dialog, int exitCode) {
        if (isNotValid(dialog)) return;
        Dispatch.run(true, () -> dialog.close(exitCode));
    }

    @FunctionalInterface
    public interface DialogCallback<T extends DBNDialog<?>> {
        void call(T dialog, int exitCode);
    }

}

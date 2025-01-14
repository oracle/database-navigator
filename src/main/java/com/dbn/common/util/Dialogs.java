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

import com.dbn.common.Reflection;
import com.dbn.common.compatibility.Compatibility;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.dialog.DBNDialog;
import com.intellij.ui.PopupBorder;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JRootPane;
import javax.swing.border.Border;
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

    /**
     * Removes decorations from a given {@link DBNDialog} by disabling its default window decorations
     * and applying a custom border. Optionally enables rounded corners for the dialog.
     *
     * @param dialog the {@link DBNDialog} instance to be undecorated
     * @param rounded a boolean indicating whether to apply rounded corners to the dialog
     */
    @Compatibility
    public static void undecorate(DBNDialog<?> dialog, boolean rounded) {
        dialog.setUndecorated(true);
        Border border = PopupBorder.Factory.create(true, true);

        JRootPane rootPane = dialog.getRootPane();
        rootPane.setWindowDecorationStyle(JRootPane.NONE);
        rootPane.setBorder(border);

        if (rounded) {
            //WindowRoundedCornersManager.configure(this);
            Reflection.invokeMethod("com.intellij.ui.WindowRoundedCornersManager", "configure", dialog);
        }
    }

    @FunctionalInterface
    public interface DialogCallback<T extends DBNDialog<?>> {
        void call(T dialog, int exitCode);
    }

}

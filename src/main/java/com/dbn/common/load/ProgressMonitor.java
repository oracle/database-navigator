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

package com.dbn.common.load;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.util.Commons.nvl;

@UtilityClass
public final class ProgressMonitor {

    @Nullable
    public static ProgressIndicator getProgressIndicator() {
        Application application = ApplicationManager.getApplication();
        if (application == null) return null;

        return ProgressManager.getInstance().getProgressIndicator();
    }

    @NotNull
    public static ProgressIndicator ensureProgressIndicator() {
        return nvl(getProgressIndicator(), DevNullProgressIndicator.INSTANCE);
    }

    private static ProgressIndicator progress() {
        ProgressIndicator progress = getProgressIndicator();
        return progress == null ? DevNullProgressIndicator.INSTANCE : progress;
    }

    public static void checkCancelled() {
        ProgressManager.checkCanceled();
    }

    public static boolean isProgressCancelled() {
        return progress().isCanceled();
    }

    public static boolean isProgressThread() {
        return getProgressIndicator() != null;
    }

    public static void setProgressIndeterminate(boolean indeterminate) {
        progress().setIndeterminate(indeterminate);
    }

    public static void setProgressFraction(double fraction) {
        progress().setFraction(fraction);
    }

    public static void setProgressText(String text) {
        progress().setText(text);
    }

    public static void setProgressDetail(String subtext) {
        progress().setText2(subtext);
    }

    public static boolean isProgressModal() {
        return progress().isModal();
    }

    public static boolean isProgress() {
        return progress() != DevNullProgressIndicator.INSTANCE;
    }
}

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

package com.dbn.common.thread;

import com.dbn.common.dispose.Failsafe;
import com.dbn.common.util.Measured;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ThrowableComputable;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class Write {

    public static void run(Runnable runnable) {
        run(null, runnable);
    }

    public static void run(Project project, Runnable runnable) {
        Application application = ApplicationManager.getApplication();
        if (application.isWriteAccessAllowed()) {
            if (project == null) {
                Measured.run("executing Write action", () -> Failsafe.guarded(runnable, r -> r.run()));
            } else {
                Measured.run("executing Write action", () -> Failsafe.guarded(() -> WriteCommandAction.writeCommandAction(Failsafe.nd(project)).run(() -> runnable.run())));
            }

        } else if (application.isDispatchThread()) {
            application.runWriteAction(() -> run(project, runnable));

        } else if (application.isReadAccessAllowed()){
            // write action invoked from within read action
            Background.run(() -> {
                ModalityState modalityState = ModalityState.defaultModalityState();
                application.invokeAndWait(() -> run(project, runnable), modalityState);
            });
        } else {
            Dispatch.run(() -> run(project, runnable));
        }
    }

    public static <T, E extends Throwable> T compute(ThrowableComputable<T, E> computable) throws E {
        return WriteAction.compute(computable);
    }
}

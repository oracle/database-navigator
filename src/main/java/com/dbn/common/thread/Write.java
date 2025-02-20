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

import com.dbn.common.util.Measured;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ThrowableComputable;
import lombok.experimental.UtilityClass;

import static com.dbn.common.dispose.Failsafe.guarded;
import static com.intellij.openapi.application.ApplicationManager.getApplication;
import static com.intellij.openapi.command.WriteCommandAction.writeCommandAction;

@UtilityClass
public final class Write {

    /**
     * FIRE write action and FORGET
     * @param runnable the code to be executed in write action
     */
    public static void run(Runnable runnable) {
        run(null, runnable);
    }

    /**
     * FIRE write action and FORGET
     * @param project optional {@link Project} to surround write action with an "undo" command wrapper
     * @param runnable the code to be executed in write action
     */
    public static void run(Project project, Runnable runnable) {
        Application application = getApplication();
        if (application.isWriteAccessAllowed()) {
            if (project == null) {
                Measured.run("executing Write action", () -> guarded(runnable, r -> r.run()));
            } else {
                Measured.run("executing Write action", () -> writeCommandAction(project).run(() -> guarded(runnable, r -> r.run())));
            }
            return;
        }

        if (!application.isDispatchThread() && application.isReadAccessAllowed()){
            // write action invoked from within read action (defer through background thread)
            // TODO can we not simply fallback on the dispatch thread invocation below?
            Background.run(() -> run(project, runnable));
            return;
        }

        // determine the current modality state ("any" is not allowed here)
        // (see com.intellij.openapi.application.TransactionGuard)
        ModalityState modalityState = Dispatch.getCurrentModalityState();
        Dispatch.execute(modalityState, () -> application.runWriteAction(() -> run(project, runnable)));
    }

    /**
     * FIRE write action and WAIT for computation result
     * @param computable the computation code to be executed in write action
     * @return the result of the computation
     * @param <T> the type of the computation result
     * @param <E> the type of exception thrown by the computable
     * @throws E any exception caused by the given computable
     */
    public static <T, E extends Throwable> T compute(ThrowableComputable<T, E> computable) throws E {
        Application application = getApplication();
        return application.runWriteAction(computable);
    }
}

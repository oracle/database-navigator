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
            Background.run(project, () -> {
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

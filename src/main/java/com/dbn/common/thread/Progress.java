package com.dbn.common.thread;

import com.dbn.common.dispose.Checks;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.load.ProgressMonitor;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.ui.progress.ProgressDialogHandler;
import com.dbn.common.util.Titles;
import com.dbn.connection.context.DatabaseContext;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.Task.Backgroundable;
import com.intellij.openapi.project.Project;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.Timer;
import java.util.TimerTask;

import static com.intellij.openapi.progress.PerformInBackgroundOption.ALWAYS_BACKGROUND;

@UtilityClass
public final class Progress {
    public static void background(Project project, DatabaseContext context, boolean cancellable, String title, String text, ProgressRunnable runnable) {
        if (Checks.isNotValid(project)) return;
        title = Titles.suffixed(title, context);

        ThreadInfo invoker = ThreadInfo.copy();
        schedule(new Backgroundable(project, title, cancellable, ALWAYS_BACKGROUND) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                execute(indicator, ThreadProperty.PROGRESS, project, invoker, text, runnable);
            }
        });
    }


    public static void prompt(Project project, DatabaseContext context, boolean cancellable, String title, String text, ProgressRunnable runnable) {
        if (Checks.isNotValid(project)) return;
        title = Titles.suffixed(title, context);

        ProgressDialogHandler handler = new ProgressDialogHandler(project, title, text);
        ThreadInfo invoker = ThreadInfo.copy();
        schedule(new Task.Backgroundable(project, title, cancellable, ALWAYS_BACKGROUND) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    handler.init(indicator);
                    execute(indicator, ThreadProperty.PROGRESS, project, invoker, text, runnable);
                } finally {
                    handler.release();
                }
            }
        });
        handler.trigger();
    }


    public static void modal(Project project, DatabaseContext context, boolean cancellable, String title, String text, ProgressRunnable runnable) {
        if (Checks.isNotValid(project)) return;
        title = Titles.suffixed(title, context);

        ThreadInfo invoker = ThreadInfo.copy();
        schedule(new Task.Modal(project, title, cancellable) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                execute(indicator, ThreadProperty.MODAL, project, invoker, text, runnable);
            }
        });
    }

    private static void execute(ProgressIndicator indicator, ThreadProperty threadProperty, Project project, ThreadInfo invoker, String text, ProgressRunnable runnable) {
        ThreadMonitor.surround(project, invoker, threadProperty, () -> Failsafe.guarded(() -> {
            indicator.setText(text);
            runnable.run(indicator);
        }));
    }

    private static void schedule(Task task) {
        if (!Checks.allValid(task, task.getProject())) return;

        ProgressManager progressManager = ProgressManager.getInstance();
        Dispatch.run(ModalityState.any(), () -> progressManager.run(task));
    }

    public static double progressOf(int is, int should) {
        return ((double) is) / should;
    }

    /**
     * Creates a cancel callback to the current progress indicator (of the current thread is a background cancellable thread)
     * @param onCancel a {@link Runnable} to be invoked when progress is cancelled
     */
    public static void cancelCallback(Runnable onCancel) {
        ProgressIndicator progressIndicator = ProgressMonitor.getProgressIndicator();
        if (progressIndicator == null) return;
        if (progressIndicator.isCanceled()) return;
        if (!progressIndicator.isRunning()) return;
        WeakRef<ProgressIndicator> indicator = WeakRef.of(progressIndicator);

        // self-closing timer, monitoring the thread-local progress indicator and closing when progress is no longer running
        Timer timer = new Timer("DBN - Progress Cancel Monitor", true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                ProgressIndicator progressIndicator = indicator.get();
                if (progressIndicator == null || !progressIndicator.isRunning()) {
                    timer.cancel();
                    return;
                }
                if (progressIndicator.isCanceled()) onCancel.run();
            }
        }, 500, 500);
    }

}

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

package com.dbn.common.ui.dialog;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.TimeUtil;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.util.Timer;
import java.util.TimerTask;

import static com.dbn.common.dispose.Failsafe.guarded;

public abstract class DialogWithTimeout extends DBNDialog<DialogWithTimeoutForm>{
    private final Timer timeoutTimer;
    private int secondsLeft;

    protected DialogWithTimeout(Project project, String title, boolean canBeParent, int timeoutSeconds) {
        super(project, title, canBeParent);
        secondsLeft = timeoutSeconds;
        timeoutTimer = new Timer("DBN - Timeout Dialog Task [" + getProject().getName() + "]");
        timeoutTimer.schedule(new TimeoutTask(), TimeUtil.Millis.ONE_SECOND, TimeUtil.Millis.ONE_SECOND);
    }

    @NotNull
    @Override
    protected DialogWithTimeoutForm createForm() {
        return new DialogWithTimeoutForm(this, secondsLeft);
    }

    @Override
    protected void init() {
        getForm().setContentComponent(createContentComponent());
        super.init();
    }

    private class TimeoutTask extends TimerTask {
        @Override
        public void run() {
            guarded(DialogWithTimeout.this, d -> {
                if (d.secondsLeft > 0) {
                    d.secondsLeft = d.secondsLeft -1;
                    d.getForm().updateTimeLeft(secondsLeft);
                    if (d.secondsLeft == 0) {
                        Dispatch.run(() -> d.doDefaultAction());
                    }
                }
            });
        }
    }

    protected abstract JComponent createContentComponent();

    public abstract void doDefaultAction();

    @Override
    public void disposeInner() {
        Disposer.dispose(timeoutTimer);
    }

}

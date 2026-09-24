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

package com.dbn.debugger;

import com.dbn.common.thread.Dispatch;
import com.intellij.debugger.ui.DebuggerContentInfo;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.execution.ui.layout.LayoutViewOptions;
import com.intellij.ui.content.Content;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebugSessionListener;
import com.intellij.xdebugger.ui.XDebugTabLayouter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.intellij.debugger.ui.DebuggerContentInfo.CONSOLE_CONTENT;
import static com.intellij.debugger.ui.DebuggerContentInfo.FRAME_CONTENT;

public class DBDebugTabLayouter extends XDebugTabLayouter {
    private static final String BREAKPOINT_CONDITION = "breakpoint";
    private static final String FINISH_CONDITION = "finish";

    private final XDebugSession session;
    private final XDebugTabLayouter delegate;

    public DBDebugTabLayouter(@NotNull XDebugSession session) {
        this(session, null);
    }

    public DBDebugTabLayouter(@NotNull XDebugSession session, @Nullable XDebugTabLayouter delegate) {
        this.session = session;
        this.delegate = delegate;
    }

    @NotNull
    @Override
    public Content registerConsoleContent(@NotNull RunnerLayoutUi ui, @NotNull ExecutionConsole console) {
        Content consoleContent = delegate == null ?
                super.registerConsoleContent(ui, console) :
                delegate.registerConsoleContent(ui, console);

        ui.getDefaults()
                .initContentAttraction(CONSOLE_CONTENT, LayoutViewOptions.STARTUP)
                .initContentAttraction(CONSOLE_CONTENT, FINISH_CONDITION);

        // The combined Threads & Variables layout registers its own breakpoint attraction.
        // Register the equivalent default only when the legacy frame content is present.
        if (ui.findContent(FRAME_CONTENT) != null) {
            ui.getDefaults().initContentAttraction(FRAME_CONTENT, BREAKPOINT_CONDITION);
        }

        AtomicBoolean frameContentShown = new AtomicBoolean();
        session.addSessionListener(new XDebugSessionListener() {
            @Override
            public void sessionPaused() {
                if (session.getCurrentStackFrame() == null) return;
                if (!frameContentShown.compareAndSet(false, true)) return;

                Dispatch.run(ui.getComponent(), () -> {
                    if (ui.isDisposed() || !session.isSuspended()) return;
                    ui.attractBy(BREAKPOINT_CONDITION);
                });
            }
        });

        return consoleContent;
/*
        Content content = ui.createContent(DebuggerContentInfo.CONSOLE_CONTENT, console.getComponent(),
                XDebuggerBundle.message("debugger.session.tab.console.content.name"),
                AllIcons.Debugger.Console,
                console.getPreferredFocusableComponent());
        content.setCloseable(false);
        ui.addContent(content, 1, PlaceInGrid.center, false);
        ui.getDefaults().initFocusContent(DebuggerContentInfo.FRAME_CONTENT, LayoutViewOptions.STARTUP);
        return content;
*/
    }

    @Override
    public void registerAdditionalContent(@NotNull RunnerLayoutUi ui) {
        if (delegate == null) return;
        delegate.registerAdditionalContent(ui);
    }

}

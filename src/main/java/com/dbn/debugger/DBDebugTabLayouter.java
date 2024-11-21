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

import com.intellij.debugger.ui.DebuggerContentInfo;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.execution.ui.layout.LayoutViewOptions;
import com.intellij.ui.content.Content;
import com.intellij.xdebugger.ui.XDebugTabLayouter;
import org.jetbrains.annotations.NotNull;

public class DBDebugTabLayouter extends XDebugTabLayouter {
    @NotNull
    @Override
    public Content registerConsoleContent(@NotNull RunnerLayoutUi ui, @NotNull ExecutionConsole console) {
        Content consoleContent = super.registerConsoleContent(ui, console);
        ui.getDefaults().initContentAttraction(DebuggerContentInfo.FRAME_CONTENT, LayoutViewOptions.STARTUP);
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
}

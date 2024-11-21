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

package com.dbn.debugger.jdwp;

import com.dbn.common.compatibility.Compatibility;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.events.DebuggerCommandImpl;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public abstract class ManagedThreadCommand extends DebuggerCommandImpl{
    private final Priority priority;

    @Compatibility
    private ManagedThreadCommand(Priority priority) {
        //super(priority); // backward compatibility
        super();
        this.priority = priority;
    }

    @Override
    public Priority getPriority() {
        return super.getPriority();
    }

    public static void schedule(DebugProcess debugProcess, Priority priority, Runnable action) {
        ManagedThreadCommand command = create(priority, action);
        ((DebugProcessImpl) debugProcess).getManagerThread().schedule(command);
    }


    public static void invoke(DebugProcess debugProcess, Priority priority, Runnable action) {
        ManagedThreadCommand command = create(priority, action);
        ((DebugProcessImpl) debugProcess).getManagerThread().invoke(command);
    }

    @NotNull
    private static ManagedThreadCommand create(Priority priority, Runnable action) {
        return new ManagedThreadCommand(priority) {
            @Override
            protected void action() throws Exception {
                action.run();
            }
        };
    }
}

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

package com.dbn.debugger.options;

import com.dbn.common.option.InteractiveOption;
import com.dbn.debugger.DBDebuggerType;
import lombok.Getter;

import static com.dbn.nls.NlsResources.txt;

@Getter
public enum DebuggerTypeOption implements InteractiveOption {
    JDBC(txt("cfg.debugger.const.DebuggerTypeOption_JDBC"), DBDebuggerType.JDBC),
    JDWP(txt("cfg.debugger.const.DebuggerTypeOption_JDWP"), DBDebuggerType.JDWP),
    ASK(txt("cfg.debugger.const.DebuggerTypeOption_ASK")),
    CANCEL(txt("cfg.debugger.const.DebuggerTypeOption_CANCEL"));

    private final String name;
    private final DBDebuggerType debuggerType;

    DebuggerTypeOption(String name) {
        this.name = name;
        this.debuggerType = null;
    }

    DebuggerTypeOption(String name, DBDebuggerType debuggerType) {
        this.name = name;
        this.debuggerType = debuggerType;
    }

    @Override
    public boolean isCancel() {
        return this == CANCEL;
    }

    @Override
    public boolean isAsk() {
        return this == ASK;
    }
}

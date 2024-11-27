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

package com.dbn.execution.compiler;

import com.dbn.common.icon.Icons;
import com.dbn.common.option.InteractiveOption;
import lombok.Getter;

import javax.swing.Icon;

import static com.dbn.nls.NlsResources.txt;

@Getter
public enum CompileType implements InteractiveOption {
    NORMAL(txt("cfg.compiler.const.CompileType_NORMAL"), Icons.OBJECT_COMPILE, true),
    DEBUG(txt("cfg.compiler.const.CompileType_DEBUG"), Icons.OBJECT_COMPILE_DEBUG, true),
    KEEP(txt("cfg.compiler.const.CompileType_KEEP"), null/*Icons.OBEJCT_COMPILE_KEEP*/, true),
    ASK(txt("cfg.compiler.const.CompileType_ASK"), null/*Icons.OBEJCT_COMPILE_ASK*/, false);

    private final String name;
    private final Icon icon;
    private final boolean persistable;

    CompileType(String name, Icon icon, boolean persistable) {
        this.name = name;
        this.icon = icon;
        this.persistable = persistable;
    }

    @Override
    public boolean isCancel() {
        return false;
    }

    @Override
    public boolean isAsk() {
        return this == ASK;
    }
}

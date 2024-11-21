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

package com.dbn.execution;

import com.dbn.common.option.InteractiveOption;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.dbn.nls.NlsResources.txt;

@Getter
@Deprecated
@AllArgsConstructor
public enum TargetConnectionOption implements InteractiveOption{
    ASK(txt("cfg.execution.const.TargetConnectionOption_ASK")),
    MAIN(txt("cfg.execution.const.TargetConnectionOption_MAIN")),
    POOL(txt("cfg.execution.const.TargetConnectionOption_POOL")),
    CANCEL(txt("cfg.execution.const.TargetConnectionOption_CANCEL"));

    private final String name;

    @Override
    public boolean isCancel() {
        return this == CANCEL;
    }

    @Override
    public boolean isAsk() {
        return this == ASK;
    }
}

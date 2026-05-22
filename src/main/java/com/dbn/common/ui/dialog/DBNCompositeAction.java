/*
 * Copyright 2026 Oracle and/or its affiliates
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

import com.intellij.openapi.ui.OptionAction;
import lombok.experimental.Delegate;

import javax.swing.Action;
import java.util.Arrays;
import java.util.Objects;

class DBNCompositeAction implements OptionAction {
    private final @Delegate Action action;
    private final Action[] options;

    DBNCompositeAction(Action ... actions) {
        if (actions == null || actions.length == 0 || actions[0] == null) {
            throw new IllegalArgumentException("Main action is required");
        }

        this.action = actions[0];
        this.options = Arrays.stream(actions)
                .skip(1)
                .filter(Objects::nonNull)
                .peek(a -> a.putValue(DBNDialog.PARENT, this))
                .toArray(Action[]::new);

    }

    DBNCompositeAction(Action action, Action[] options) {
        this.action = action;
        this.options = options;
    }

    @Override
    public Action[] getOptions() {
        return options;
    }
}

/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.common.action;

import com.dbn.common.compatibility.Compatibility;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.NlsActions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A compatibility wrapper for {@link com.intellij.openapi.actionSystem.DefaultActionGroup}
 * Enhances the action with {@link ActionUpdateThread} awareness
 * (some intellij versions complain about the missing update thread specification).
 * @author Dan Cioca (Oracle)
 */
@Compatibility
public class DefaultActionGroup extends com.intellij.openapi.actionSystem.DefaultActionGroup implements BackgroundUpdateAware, DumbAware {

    public DefaultActionGroup() {
    }

    public DefaultActionGroup(@Nullable @NlsActions.ActionText String shortName, boolean popup) {
        super(shortName, popup);
    }

    //...
    @NotNull
    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return resolveActionUpdateThread();
    }
}

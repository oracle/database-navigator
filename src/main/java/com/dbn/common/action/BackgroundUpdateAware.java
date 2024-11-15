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

package com.dbn.common.action;

import com.dbn.common.Reflection;
import com.dbn.common.compatibility.Compatibility;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.UpdateInBackground;

/**
 * ActionUpdateThread decision stub, resolving teh {@link ActionUpdateThread} based on the {@link BackgroundUpdate} annotation
 * Actions that take long to update their presentation should be annotated accordingly
 * (i.e. actions that have intensive computational logic inside the {@link com.intellij.openapi.actionSystem.AnAction#update(AnActionEvent)})
 *
 * @author Dan Cioca (Oracle)
 */
public interface BackgroundUpdateAware extends UpdateInBackground {

    //@Override
    @Compatibility
    default boolean isUpdateInBackground() {
        return Reflection.hasAnnotation(getClass(), BackgroundUpdate.class);
    }
}

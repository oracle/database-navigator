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

package com.dbn.common.project;

import com.dbn.common.action.UserDataKeys;
import com.dbn.common.ref.WeakRef;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.dispose.Failsafe.nn;

public class ModuleRef extends WeakRef<Module> {
    private ModuleRef(Module module) {
        super(module);
    }

    public static ModuleRef of(Module module) {
        if (module == null) {
            return new ModuleRef(null);
        } else {
            ModuleRef moduleRef = module.getUserData(UserDataKeys.MODULE_REF);
            if (moduleRef == null) {
                moduleRef = new ModuleRef(module);
                module.putUserData(UserDataKeys.MODULE_REF, moduleRef);
            }
            return moduleRef;
        }
    }

    @NotNull
    @Override
    public Module ensure() {
        return nn(super.ensure());
    }
}

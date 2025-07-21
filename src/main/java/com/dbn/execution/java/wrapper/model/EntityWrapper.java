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

package com.dbn.execution.java.wrapper.model;

import com.dbn.common.ref.WeakRef;
import com.dbn.execution.java.wrapper.WrapperContext;
import com.dbn.execution.java.wrapper.WrapperModel;
import com.dbn.execution.java.wrapper.WrapperModelInput;
import com.dbn.execution.java.wrapper.naming.WrapperNamingProvider;

abstract class EntityWrapper {
    private final WeakRef<WrapperModel> model;

    public EntityWrapper(WrapperModel model) {
        this.model = WeakRef.of(model);
    }

    public WrapperModel getModel() {
        return model.ensure();
    }

    protected WrapperContext getContext() {
        return getModel().getContext();
    }

    protected WrapperModelInput getInput() {
        return getModel().getInput();
    }

    protected WrapperNamingProvider getNamingProvider() {
        return getContext().getNamingProvider();
    }
}

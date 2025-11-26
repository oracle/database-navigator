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

package com.dbn.common.option;

import com.dbn.common.Reflection;
import com.dbn.common.compatibility.Compatibility;
import com.dbn.common.compatibility.Workaround;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.DoNotAskOption;
import com.intellij.openapi.ui.DialogWrapper.DoNotAskOption;

import java.lang.reflect.Proxy;

import static com.dbn.common.util.Unsafe.cast;

/**
 * DoNotAskOption adapter
 * Motivated by various api changes in the versions ranging from 2020.1 to 2025.3.
 * (this ensures runtime compatibility throughout)
 * TODO review and decommission after discontinuing support for 2020-2022.x
 */
@Compatibility
public interface RememberOption extends DoNotAskOption {

    @Workaround
    static Class<DoNotAskOption> spec() {
        try {
            String className = DialogWrapper.class.getName() + "$DoNotAskOption";
            return cast(Class.forName(className));
        } catch (Throwable e) {
            return DoNotAskOption.class;
        }
    }

    @Workaround
    static DoNotAskOption wrap(DoNotAskOption option) {
        if (option == null) return null;

        Class<DoNotAskOption> specification = spec();
        if (DoNotAskOption.class.equals(specification)) return option;

        return cast(Proxy.newProxyInstance(
                specification.getClassLoader(),
                new Class[]{specification},
                (proxy, method, args) ->
                        Reflection.invokeMethod(option, method.getName(), args)));
    }

}

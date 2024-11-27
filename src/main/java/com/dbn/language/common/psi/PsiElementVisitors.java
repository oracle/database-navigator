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

package com.dbn.language.common.psi;

import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PsiElementVisitors {
    private final List<String> supportedNames;
    private final Map<Class, Boolean> supported = new ConcurrentHashMap<>();

    private PsiElementVisitors(String ... supportedNames) {
        this.supportedNames = Arrays.asList(supportedNames);
    }

    public boolean isSupported(@NotNull PsiElementVisitor visitor) {
        Boolean supported = this.supported.computeIfAbsent(visitor.getClass(), c -> evaluateSupported(c));
        return supported == Boolean.TRUE;
    }

    private Boolean evaluateSupported(Class c) {
        return supportedNames.stream().anyMatch(n -> c.getName().contains(n)) ? Boolean.TRUE : Boolean.FALSE;
    }

    public static PsiElementVisitors create(String ... supportedNames) {
        return new PsiElementVisitors(supportedNames);
    }
}

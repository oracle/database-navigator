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

package com.dbn.common.presentation;

import com.dbn.common.presentation.provider.DefaultPresentationProvider;
import lombok.experimental.UtilityClass;

import javax.swing.Icon;

@UtilityClass
public class Presentation {
    public static final DefaultPresentationProvider GENERIC_PROVIDER = new DefaultPresentationProvider();

    private static <T> PresentationProvider<T> getProvider(T object) {
        return PresentationProviders.get(object);
    }

    public static String presentableName(Object object) {
        if (object == null) return "Undefined";
        return getProvider(object).getName(object);
    }

    public static String presentableDetailedName(Object object) {
        if (object == null) return "Undefined";
        return getProvider(object).getDetailedName(object);
    }

    public static String presentableTypeName(Object object) {
        if (object == null) return "Undefined";
        return getProvider(object).getTypeName(object);
    }

    public static String presentableDescription(Object object) {
        if (object == null) return null;
        return getProvider(object).getDescription(object);
    }

    public static Icon presentableIcon(Object object) {
        if (object == null) return null;
        return getProvider(object).getIcon(object);
    }
}

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

package com.dbn.common.util;

import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import lombok.experimental.UtilityClass;

import static com.dbn.common.util.Commons.coalesce;
import static com.dbn.common.util.Commons.nvl;

@UtilityClass
public class Languages {
    public static Language getJavaLanguage() {
        return nvl(Language.findLanguageByID("JAVA"), PlainTextLanguage.INSTANCE);
    }

    public static Language getJsonLanguage() {
        return coalesce(
                () -> Language.findLanguageByID("JSON"),
                () -> Language.findLanguageByID("JSON5"),
                () -> Language.findLanguageByID("JavaScript"),
                () -> PlainTextLanguage.INSTANCE);
    }
}

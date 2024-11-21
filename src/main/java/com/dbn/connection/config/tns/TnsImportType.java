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

package com.dbn.connection.config.tns;

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.Presentable;
import com.dbn.common.util.Commons;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;

@Getter
@AllArgsConstructor
public enum TnsImportType implements Presentable {
    FIELDS(txt("cfg.connection.const.TnsImportType_FIELDS"), loadInfo("tns_import_type_fields.html")),
    PROFILE(txt("cfg.connection.const.TnsImportType_PROFILE"), loadInfo("tns_import_type_profile.html")),
    DESCRIPTOR(txt("cfg.connection.const.TnsImportType_DESCRIPTOR"), loadInfo("tns_import_type_descriptor.html"));

    private final String name;
    private final TextContent info;

    @NotNull
    @SneakyThrows
    private static TextContent loadInfo(String fileName) {
        String content = Commons.readInputStream(TnsImportType.class.getResourceAsStream(fileName));
        return TextContent.html(content);
    }
}

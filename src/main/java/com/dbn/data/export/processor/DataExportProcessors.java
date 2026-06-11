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

package com.dbn.data.export.processor;

import com.dbn.common.extension.ExtensionPointCache;
import com.dbn.data.export.DataExportFormat;

import java.util.List;

public class DataExportProcessors extends ExtensionPointCache<DataExportFormat, DataExportProcessor> {
    private static final DataExportProcessors INSTANCE = new DataExportProcessors();

    private DataExportProcessors() {
        super(DataExportProcessor.EP, p -> p.getFormat());
    }

    public static DataExportProcessor get(DataExportFormat format) {
        return INSTANCE.find(format);
    }

    public static List<DataExportProcessor> list() {
        return INSTANCE.all();
    }
}

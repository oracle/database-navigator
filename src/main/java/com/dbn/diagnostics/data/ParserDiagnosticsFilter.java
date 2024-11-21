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

package com.dbn.diagnostics.data;

import com.dbn.common.filter.Filter;
import com.dbn.common.util.Strings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParserDiagnosticsFilter implements Filter<ParserDiagnosticsEntry> {
    public static final ParserDiagnosticsFilter EMPTY = new ParserDiagnosticsFilter();

    private StateTransition.Category stateCategory;
    private String fileType;

    @Override
    public boolean accepts(ParserDiagnosticsEntry entry) {
        return matchesState(entry) && matchesFileType(entry);
    }

    private boolean matchesState(ParserDiagnosticsEntry object) {
        return stateCategory == null || stateCategory == object.getStateTransition().getCategory();
    }

    private boolean matchesFileType(ParserDiagnosticsEntry entry) {
        return Strings.isEmpty(fileType) || Strings.endsWithIgnoreCase(entry.getFilePath(), "." + fileType);
    }
}

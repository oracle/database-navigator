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


import com.dbn.common.list.FilteredList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


@Getter
public class ParserDiagnosticsDeltaResult {
    private ParserDiagnosticsFilter filter = ParserDiagnosticsFilter.EMPTY;
    private final List<ParserDiagnosticsEntry> entries = FilteredList.stateful(filter);

    private final ParserDiagnosticsResult previous;
    private final ParserDiagnosticsResult current;

    public ParserDiagnosticsDeltaResult(@Nullable ParserDiagnosticsResult previous, @NotNull ParserDiagnosticsResult current) {
        this.previous = previous;
        this.current = current;
        for (String s : current.getEntries().keySet()) {
            IssueCounter newIssues = current.getIssues(s);
            IssueCounter oldIssues = previous == null ? newIssues : previous.getIssues(s);
            addEntry(s, oldIssues, newIssues);
        }

        if (previous != null) {
            for (String file : previous.getFiles()) {
                if (!current.isPresent(file)) {
                    addEntry(file, previous.getIssues(file), null);
                }
            }
        }
    }

    public void setFilter(ParserDiagnosticsFilter filter) {
        this.filter = filter;
    }

    private void addEntry(String file, IssueCounter oldIssues, IssueCounter newIssues) {
        ParserDiagnosticsEntry diagnosticsCapture = new ParserDiagnosticsEntry(file, oldIssues, newIssues);
        entries.add(diagnosticsCapture);
    }

    public String getName() {
        if (previous == null) {
            return current.getName();
        } else {
            return current.getName() + " compared to " + previous.getName();
        }
    }

    public StateTransition getFilter() {
        IssueCounter oldIssues = previous == null ? current.getIssues() : previous.getIssues();
        IssueCounter newIssues = current.getIssues();

        return ParserDiagnosticsUtil.computeStateTransition(oldIssues, newIssues);
    }
}

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

import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

import static com.dbn.common.util.Commons.nvl;
import static com.dbn.diagnostics.data.ParserDiagnosticsUtil.computeStateTransition;

@Getter
public class ParserDiagnosticsEntry implements Comparable<ParserDiagnosticsEntry>{

    private final String filePath;
    private final IssueCounter oldIssues;
    private final IssueCounter newIssues;

    public ParserDiagnosticsEntry(String filePath, IssueCounter oldIssues, IssueCounter newIssues) {
        this.filePath = filePath;
        this.oldIssues = nvl(oldIssues, IssueCounter.EMPTY);
        this.newIssues = nvl(newIssues, IssueCounter.EMPTY);
    }

    @Nullable
    public VirtualFile getFile() {
        LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
        return localFileSystem.findFileByIoFile(new File(filePath));
    }

    public StateTransition getStateTransition() {
        return computeStateTransition(oldIssues, newIssues);
    }

    @Override
    public int compareTo(@NotNull ParserDiagnosticsEntry o) {
        return filePath.compareTo(o.getFilePath());
    }
}

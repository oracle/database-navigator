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

package com.dbn.connection.console;

import com.dbn.common.thread.Write;
import com.dbn.editor.code.content.GuardedBlockMarkers;
import com.dbn.editor.code.content.GuardedBlockType;
import com.dbn.vfs.file.DBConsoleVirtualFile;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.util.GuardedBlocks.createGuardedBlocks;
import static com.dbn.common.util.GuardedBlocks.removeGuardedBlocks;

public class DatabaseConsoleFileInitializer implements FileDocumentManagerListener {
    @Override
    public void fileContentLoaded(@NotNull VirtualFile file, @NotNull Document document) {
        if (file instanceof DBConsoleVirtualFile) {
            // restore guarded blocks after console file loaded
            DBConsoleVirtualFile consoleFile = (DBConsoleVirtualFile) file;
            GuardedBlockMarkers guardedBlocks = consoleFile.getContent().getOffsets().getGuardedBlocks();
            if (guardedBlocks.isEmpty()) return;

            Write.run(() -> {
                removeGuardedBlocks(document, GuardedBlockType.READONLY_DOCUMENT_SECTION);
                createGuardedBlocks(document, GuardedBlockType.READONLY_DOCUMENT_SECTION, guardedBlocks, null);
            });
        }
    }
}

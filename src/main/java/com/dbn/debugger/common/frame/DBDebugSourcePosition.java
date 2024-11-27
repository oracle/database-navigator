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

package com.dbn.debugger.common.frame;

import com.dbn.vfs.DatabaseOpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.XSourcePositionWrapper;
import com.intellij.xdebugger.impl.XSourcePositionImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DBDebugSourcePosition extends XSourcePositionWrapper {
    private DBDebugSourcePosition(@NotNull XSourcePosition position) {
        super(position);
    }

    @Nullable
    public static DBDebugSourcePosition create(@Nullable VirtualFile file, int line) {
        if (file == null) return null;

        XSourcePositionImpl sourcePosition = XSourcePositionImpl.create(file, line);
        return new DBDebugSourcePosition(sourcePosition);
    }

    @NotNull
    @Override
    public VirtualFile getFile() {
        VirtualFile file = super.getFile();
/*
        if (file instanceof DBSourceCodeVirtualFile) {
            DBSourceCodeVirtualFile sourceCodeVirtualFile = (DBSourceCodeVirtualFile) file;
            return sourceCodeVirtualFile.getMainDatabaseFile();
        }
*/
        return file;
    }

    @NotNull
    @Override
    public Navigatable createNavigatable(@NotNull Project project) {
        VirtualFile file = myPosition.getFile();
        return myPosition.getOffset() != -1
                ? new DatabaseOpenFileDescriptor(project, file, myPosition.getOffset())
                : new DatabaseOpenFileDescriptor(project, file, myPosition.getLine(), 0);

    }
}

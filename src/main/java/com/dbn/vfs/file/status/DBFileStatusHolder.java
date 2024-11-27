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

package com.dbn.vfs.file.status;

import com.dbn.common.event.ProjectEvents;
import com.dbn.common.property.PropertyHolderBase;
import com.dbn.common.ref.WeakRef;
import com.dbn.vfs.file.DBContentVirtualFile;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.dispose.Failsafe.guarded;

public class DBFileStatusHolder extends PropertyHolderBase.IntStore<DBFileStatus> {
    private final WeakRef<DBContentVirtualFile> file;

    public DBFileStatusHolder(DBContentVirtualFile file) {
        super();
        this.file = WeakRef.of(file);
    }

    @NotNull
    public DBContentVirtualFile getFile() {
        return file.ensure();
    }

    @NotNull
    private Project getProject() {
        return getFile().getProject();
    }

    @Override
    public boolean set(DBFileStatus property, boolean value) {
        return super.set(property, value);
    }

    @Override
    protected DBFileStatus[] properties() {
        return DBFileStatus.VALUES;
    }

    @Override
    protected void changed(DBFileStatus property, boolean value) {
        if (file == null) return; // not initialised yet
        guarded(this, h -> {
            ProjectEvents.notify(h.getProject(),
                    DBFileStatusListener.TOPIC,
                    (listener) -> listener.statusChanged(h.getFile(), property, value));;
        });
    }
}

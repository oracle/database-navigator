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

package com.dbn.common.file;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import lombok.Getter;

@Getter
public class FileMappingEvent<T> {
    private final T target;
    private final VirtualFile file;
    private final FileEventType eventType;

    public FileMappingEvent(T target, VirtualFile file, FileEventType eventType) {
        this.target = target;
        this.file = file;
        this.eventType = eventType;
    }

    public FileMappingEvent(T target, VFileEvent fileEvent) {
        this.target = target;
        this.file = fileEvent.getFile();
        if (fileEvent instanceof VFileDeleteEvent) {
            eventType = FileEventType.DELETED;
        } else if (fileEvent instanceof VFileMoveEvent) {
            eventType = FileEventType.MOVED;
        } else if (fileEvent instanceof VFileCreateEvent) {
            eventType = FileEventType.CREATED;
        } else if (fileEvent instanceof VFileContentChangeEvent) {
            eventType = FileEventType.MODIFIED;
        } else if (fileEvent instanceof VFilePropertyChangeEvent) {
            VFilePropertyChangeEvent propertyChangeEvent = (VFilePropertyChangeEvent) fileEvent;
            String propertyName = propertyChangeEvent.getPropertyName();
            if (VirtualFile.PROP_NAME.equals(propertyName)) {
                eventType = FileEventType.RENAMED;
            } else {
                eventType = FileEventType.MODIFIED;
            }
        } else {
            eventType = FileEventType.UNKNOWN;
        }

    }
}

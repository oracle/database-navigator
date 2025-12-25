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

package com.dbn.ml.model.source;

import com.dbn.common.state.PersistentStateElement;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import java.nio.file.Path;

import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

/**
 * Configuration for CSV file data source.
 * Follows VectorToolbox pattern (FileSystemSourceConfig).
 */
@Getter
@Setter
public class MLFileSourceConfig implements PersistentStateElement {
    private String filePath;
    private String delimiter = ",";
    private boolean hasHeader = true;

    public VirtualFile getFile() {
        if (filePath == null || filePath.isEmpty()) return null;
        VirtualFileManager fileManager = VirtualFileManager.getInstance();
        return fileManager.findFileByNioPath(Path.of(filePath));
    }

    @Override
    public void readState(Element element) {
        if (element == null) return;
        
        filePath = stringAttribute(element, "file-path");
        delimiter = stringAttribute(element, "delimiter", delimiter);
        hasHeader = Boolean.parseBoolean(stringAttribute(element, "has-header", "true"));
    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "file-path", filePath);
        setStringAttribute(element, "delimiter", delimiter);
        setStringAttribute(element, "has-header", String.valueOf(hasHeader));
    }
}

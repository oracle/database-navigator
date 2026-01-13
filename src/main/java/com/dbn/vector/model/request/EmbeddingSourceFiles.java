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

package com.dbn.vector.model.request;

import com.dbn.common.state.PersistentStateElement;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static java.util.Collections.emptyList;

@Setter
@Getter
public class EmbeddingSourceFiles implements PersistentStateElement {
    private List<String> filePaths = new ArrayList<>();

    public List<VirtualFile> getFiles() {
        if (filePaths == null) return emptyList();
        VirtualFileManager fileManager = VirtualFileManager.getInstance();
        return filePaths
                .stream()
                .map(p -> fileManager.findFileByNioPath(Path.of(p)))
                .filter(f -> f != null)
                .toList();
    }

    @Override
    public void readState(Element element) {
        Element filesElement = element.getChild("file-sources");
        List<Element> fileElements = childrenOf(filesElement, "file");
        for (Element fileElement : fileElements) {
            String path = stringAttribute(fileElement, "path");
            filePaths.add(path);
        }
    }

    @Override
    public void writeState(Element element) {
        Element filesElement = newElement(element, "file-sources");
        for (String filePath : filePaths) {
            Element fileElement = newElement(filesElement, "file");
            setStringAttribute(fileElement, "path", filePath);
        }
    }

    public int getFileCount() {
        return filePaths.size();
    }
}

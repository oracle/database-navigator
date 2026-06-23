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
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import java.util.List;
import java.util.function.Predicate;

import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Lists.anyMatch;
import static com.dbn.common.util.Lists.convert;
import static com.dbn.common.util.Lists.filter;

@Setter
@Getter
public class EmbeddingSourceFiles extends EmbeddingSourceList<EmbeddingFileSource> implements PersistentStateElement {
    public List<VirtualFile> getFileSources() {
        return getElements()
                .stream()
                .map(p -> p.getFile())
                .filter(f -> f != null)
                .toList();
    }

    @Override
    public void readState(Element element) {
        Element filesElement = element.getChild("file-sources");
        List<Element> fileElements = childrenOf(filesElement, "file");
        for (Element fileElement : fileElements) {
            String path = stringAttribute(fileElement, "path");
            addElement(new EmbeddingFileSource(path));
        }
    }

    @Override
    public void writeState(Element element) {
        Element filesElement = newElement(element, "file-sources");
        for (EmbeddingFileSource source : getElements()) {
            Element fileElement = newElement(filesElement, "file");
            setStringAttribute(fileElement, "path", source.getFilePath());
        }
    }

    public void setFilePaths(List<String> filePaths) {
        setFilePaths(filePaths, path -> false);
    }

    public void setFilePaths(List<String> filePaths, Predicate<String> uploadAuthorization) {
        clear();
        List<EmbeddingFileSource> sources = convert(filePaths, p -> new EmbeddingFileSource(p, uploadAuthorization.test(p)));
        addElements(sources);
    }

    public boolean hasUnauthorizedFileSources() {
        return anyMatch(getElements(), s -> !s.isUploadAuthorized());
    }

    public List<EmbeddingFileSource> getUnauthorizedFileSources() {
        return filter(getElements(), s -> !s.isUploadAuthorized());
    }

    public void authorizeFileUploads() {
        getUnauthorizedFileSources().forEach(EmbeddingFileSource::authorizeUpload);
    }
}

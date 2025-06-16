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

package com.dbn.editor.json;

import com.dbn.common.ref.WeakRefCache;
import com.dbn.common.util.Json;
import com.dbn.object.DBJsonView;
import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.testFramework.LightVirtualFile;
import lombok.experimental.UtilityClass;

import static com.dbn.language.common.psi.PsiUtil.getFileManager;

@UtilityClass
public class JsonFileCache {
    private static final WeakRefCache<DBJsonView, VirtualFile> jsonSchemaFileCache = WeakRefCache.weakKey();
    private static final WeakRefCache<DBJsonView, VirtualFile> jsonContentFileCache = WeakRefCache.weakKey();

    public static VirtualFile getJsonSchemaFile(DBJsonView jsonView) {
        return jsonSchemaFileCache.computeIfAbsent(jsonView, v -> initJsonFile(v, "SCHEMA", v.getJsonSchema()));
    }

    public static VirtualFile getJsonContentFile(DBJsonView jsonView) {
        return jsonContentFileCache.computeIfAbsent(jsonView, v -> initJsonFile(v, "CONTENT", ""));
    }

    public static PsiFile getJsonContentPsiFile(DBJsonView jsonView) {
        Project project = jsonView.getProject();
        VirtualFile contentFile = getJsonContentFile(jsonView);
        return getPsiFile(project, contentFile);
    }

    public PsiFile getJsonSchemaPsiFile(DBJsonView jsonView) {
        Project project = jsonView.getProject();
        VirtualFile contentFile = getJsonSchemaFile(jsonView);
        return getPsiFile(project, contentFile);
    }

    private static PsiFile getPsiFile(Project project, VirtualFile contentFile) {
        FileManager fileManager = getFileManager(project);
        return fileManager.getCachedPsiFile(contentFile);
    }

    private static VirtualFile initJsonFile(DBJsonView jsonView, String suffix, String text) {
        text = Json.formatJsonContent(text);
        FileType fileType = Json.resolveJsonFileType();
        Language language = Json.resolveJsonLanguage();

        String fileName = jsonView.getConnectionId() + "." + jsonView.getQualifiedName() + "_" + suffix + ".json";
        LightVirtualFile jsonFile = new LightVirtualFile(fileName, fileType, text);

        Project project = jsonView.getProject();
        FileManager fileManager = getFileManager(project);
        FileViewProvider viewProvider = fileManager.createFileViewProvider(jsonFile, true);

        viewProvider.getPsi(language);
        return jsonFile;
    }
}

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

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.ApplicationComponentBase;
import com.dbn.common.component.PersistentState;
import com.dbn.common.event.ApplicationEvents;
import com.dbn.common.thread.Write;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Strings;
import com.dbn.language.common.DBLanguageFileType;
import com.dbn.language.psql.PSQLFileType;
import com.dbn.language.sql.SQLFileType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileTypes.FileNameMatcher;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeEvent;
import com.intellij.openapi.fileTypes.FileTypeListener;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.UnknownFileType;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.component.Components.applicationService;
import static com.dbn.common.file.FileTypeService.COMPONENT_NAME;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Commons.coalesce;
import static com.dbn.common.util.Unsafe.silent;

@Slf4j
@Getter
@Setter
@State(
    name = COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class FileTypeService extends ApplicationComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Application.FileTypeService";

    private final Map<String, String> originalFileAssociations = new ConcurrentHashMap<>();
    private boolean silentFileTypeChange = false;

    private FileTypeService() {
        super(COMPONENT_NAME);
        ApplicationEvents.subscribe(this, FileTypeManager.TOPIC, snapshotFileTypeListener());
        ApplicationEvents.subscribe(this, FileTypeManager.TOPIC, toolbarsFileTypeListener());
    }

    public static FileTypeService getInstance() {
        return applicationService(FileTypeService.class);
    }

    private void withSilentContext(Runnable runnable) {
        boolean silent = silentFileTypeChange;
        try {
            silentFileTypeChange = true;
            runnable.run();
        } finally {
            silentFileTypeChange = silent;
        }
    }

    public final void associateExtension(@NotNull DBLanguageFileType fileType, @NotNull String extension) {
        associateFileNamePattern(fileType, "*." + extension);
    }

    public final void associateFileNamePattern(@NotNull DBLanguageFileType fileType, @NotNull String fileNamePattern) {
        try {
            FileType currentFileType = getCurrentFileType(fileNamePattern);
            if (currentFileType == fileType) return;

            if (!Commons.isOneOf(currentFileType,
                    UnknownFileType.INSTANCE,
                    SQLFileType.INSTANCE,
                    PSQLFileType.INSTANCE)) {

                originalFileAssociations.put(fileNamePattern, currentFileType.getName());
            }

            FileNameMatcher matcher = FileTypeManager.parseFromString(fileNamePattern);
            dissociate(currentFileType, matcher);
            associate(fileType, matcher);
        } catch (Throwable e) {
            log.error("Failed to associate file type {} for file name pattern {}", fileType, fileNamePattern, e);
        }
    }

    @NotNull
    private FileTypeListener snapshotFileTypeListener() {
        return new FileTypeListener() {
            @Override
            public void beforeFileTypesChanged(@NotNull FileTypeEvent event) {
                captureFileAssociations(SQLFileType.INSTANCE);
                captureFileAssociations(PSQLFileType.INSTANCE);
            }
        };
    }


    private FileTypeListener toolbarsFileTypeListener() {
        return new FileTypeListener() {
            @Override
            public void fileTypesChanged(@NotNull FileTypeEvent event) {
                // TODO plugin conflict resolution - show / hide toolbars
            }
        };
    }


    private void captureFileAssociations(DBLanguageFileType fileType) {
        String[] extensions = fileType.getSupportedExtensions();
        for (String extension : extensions) {
            String fileNamePattern = "*." + extension;
            FileType currentFileType = getCurrentFileType(fileNamePattern);
            if (Commons.isOneOf(currentFileType,
                    UnknownFileType.INSTANCE,
                    SQLFileType.INSTANCE,
                    PSQLFileType.INSTANCE)) continue;

            originalFileAssociations.put(fileNamePattern, currentFileType.getName());
        }
    }

    public void claimFileAssociations(DBLanguageFileType fileType) {
        withSilentContext(() -> {
            String[] extensions = fileType.getSupportedExtensions();
            for (String extension : extensions) {
                associateExtension(fileType, extension);
            }
        });
    }

    public void restoreFileAssociations() {
        withSilentContext(() -> {
            for (String fileNamePattern : originalFileAssociations.keySet()) {
                String fileTypeName = originalFileAssociations.get(fileNamePattern);
                FileType fileType = getFileType(fileTypeName);
                if (fileType == null) continue;

                associate(fileType, FileTypeManager.parseFromString(fileNamePattern));
            }

            FileType originalSqlFileType = getFileType("SQL");
            if (originalSqlFileType != null) {
                if (getCurrentFileType("*.sql") instanceof DBLanguageFileType) associate(originalSqlFileType, FileTypeManager.parseFromString("*.sql"));
                if (getCurrentFileType("*.ddl") instanceof DBLanguageFileType) associate(originalSqlFileType, FileTypeManager.parseFromString("*.ddl"));
            }
        });
    }

    private void associate(FileType fileType, @NonNls FileNameMatcher matcher) {
        FileType currentFileType = getCurrentFileType(matcher.getPresentableString());
        if (currentFileType == fileType) return;

        Write.run(() -> withSilentContext(() -> FileTypeManager.getInstance().associate(fileType, matcher)));
    }

    private void dissociate(FileType fileType, @NonNls FileNameMatcher matcher) {
        Write.run(() -> withSilentContext(() -> FileTypeManager.getInstance().removeAssociation(fileType, matcher)));
    }

    @NotNull
    public FileType getCurrentFileType(@NonNls String fileNamePattern) {
        return silent(UnknownFileType.INSTANCE, fileNamePattern, p -> {
            if (p.startsWith("*.") && p.indexOf('*', 1) < 0 && p.indexOf('.', 2) < 0) {
                return FileTypeManager.getInstance().getFileTypeByExtension(p.substring(2));
            }

            String fileName = p.contains("*") ? p.replace("*", "dbn") : p;
            return FileTypeManager.getInstance().getFileTypeByFileName(fileName);
        });
    }

    @Override
    public Element getComponentState() {
        Element element = newStateElement();
        Element mappingsElement = newElement(element, "original-file-types");

        for (String fileNamePattern : originalFileAssociations.keySet()) {
            String fileType = originalFileAssociations.get(fileNamePattern);
            Element mappingElement = newElement(mappingsElement, "mapping");
            mappingElement.setAttribute("file-name-pattern", fileNamePattern);
            mappingElement.setAttribute("file-type", fileType);
        }
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element stateElement) {
        Element mappingsElement = stateElement.getChild("original-file-types");
        if (mappingsElement == null) return;

        for (Element mappingElement : mappingsElement.getChildren()) {
            String fileNamePattern = stringAttribute(mappingElement, "file-name-pattern");
            if (Strings.isEmpty(fileNamePattern)) {
                String fileExtension = stringAttribute(mappingElement, "file-extension");
                fileNamePattern = fileExtension == null ? null : "*." + fileExtension;
            }
            String fileType = stringAttribute(mappingElement, "file-type");
            if (Strings.isEmpty(fileNamePattern)) continue;
            originalFileAssociations.put(fileNamePattern, fileType);
        }
    }

    @Nullable
    private static FileType getFileType(String fileTypeName) {
        return coalesce(
                () -> legacyFindFileType(fileTypeName),
                () -> findFileType(fileTypeName));
    }

    @Nullable
    private static FileType legacyFindFileType(String fileTypeName) {
        FileType[] registeredFileTypes = silent(FileType.EMPTY_ARRAY,
                () -> FileTypeManager.getInstance().getRegisteredFileTypes());

        return Arrays
                .stream(registeredFileTypes)
                .filter(ft -> Objects.equals(ft.getName(), fileTypeName))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    private static FileType findFileType(String fileTypeName) {
        return silent(null,
                () -> FileTypeManager.getInstance().findFileTypeByName(fileTypeName));
    }

    public List<FileNameMatcher> getAssociations(DBLanguageFileType fileType) {
        return FileTypeManager.getInstance().getAssociations(fileType);
    }
}

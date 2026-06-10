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

import org.jetbrains.annotations.Nullable;

import static com.dbn.nls.NlsResources.txt;

/**
 * Shared source-name extraction logic — eliminates duplication across
 * DBMSBackend, MLPipelineExecutor, and DatabaseMLManager.
 */
public final class MLSourceNames {

    private MLSourceNames() {}

    /**
     * Extract base name (no extension) suitable for model naming.
     * Returns {@code null} when no meaningful name is available.
     */
    public static @Nullable String extractBaseName(MLSourceConfig config) {
        MLSourceType sourceType = config.getSourceType();
        if (sourceType == MLSourceType.DATABASE_TABLE) {
            return config.getTableSourceConfig().getTableName();
        } else if (sourceType == MLSourceType.FILE_SYSTEM) {
            return extractFileName(config.getFileSourceConfig().getFilePath());
        } else if (sourceType == MLSourceType.OBJECT_STORAGE) {
            return extractObjectName(config.getCloudSourceConfig().getFileUri());
        }
        return null;
    }

    /**
     * Human-readable display name for UI labels (long URIs are truncated).
     */
    public static String getDisplayName(MLSourceConfig config) {
        MLSourceType sourceType = config.getSourceType();
        switch (sourceType) {
            case DATABASE_TABLE: {
                var tableConfig = config.getTableSourceConfig();
                return tableConfig.getSchemaName() + "." + tableConfig.getTableName();
            }
            case FILE_SYSTEM: {
                String path = config.getFileSourceConfig().getFilePath();
                if (path == null || path.isEmpty()) return txt("app.machineLearning.text.CsvFile");
                int lastSep = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
                return lastSep >= 0 ? path.substring(lastSep + 1) : path;
            }
            case OBJECT_STORAGE: {
                String uri = config.getCloudSourceConfig().getFileUri();
                if (uri == null || uri.isEmpty()) return txt("app.machineLearning.text.ObjectStorage");
                int lastSlash = uri.lastIndexOf('/');
                String objectName = lastSlash >= 0 ? uri.substring(lastSlash + 1) : uri;
                if (objectName.length() > 30) objectName = "..." + objectName.substring(objectName.length() - 27);
                return objectName;
            }
            default:
                return txt("app.machineLearning.text.UnknownSource");
        }
    }

    private static @Nullable String extractFileName(String path) {
        if (path == null || path.isEmpty()) return null;
        int lastSep = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        String fileName = lastSep >= 0 ? path.substring(lastSep + 1) : path;
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static @Nullable String extractObjectName(String uri) {
        if (uri == null || uri.isEmpty()) return null;
        int lastSlash = uri.lastIndexOf('/');
        String objectName = lastSlash >= 0 ? uri.substring(lastSlash + 1) : uri;
        int dot = objectName.lastIndexOf('.');
        return dot > 0 ? objectName.substring(0, dot) : objectName;
    }
}

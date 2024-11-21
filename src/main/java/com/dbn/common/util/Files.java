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

package com.dbn.common.util;

import com.dbn.DatabaseNavigator;
import com.dbn.language.common.DBLanguageFileType;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.vfs.file.DBConsoleVirtualFile;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.LightVirtualFile;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Objects;

@UtilityClass
public final class Files {
    // keep in sync with file type definitions in  plugin.xml
    public static final String[] SQL_FILE_EXTENSIONS = {"sql", "ddl", "vw"};
    public static final String[] PSQL_FILE_EXTENSIONS = {"psql", "plsql", "trg", "prc", "fnc", "pkg", "pks", "pkb", "tpe", "tps", "tpb"};

    public static String toRegexFileNamePattern(String fileNamePattern) {
        return "^(?i)" + fileNamePattern.replaceAll("\\*", "[a-z0-9_-]*") + "$";
    }

    public static File createFileByRelativePath(@NotNull final File absoluteBase, @NotNull final String relativeTail) {
        // assert absoluteBase.isAbsolute() && absoluteBase.isDirectory(); : assertion seem to be too costly

        File point = absoluteBase;
        final String[] parts = relativeTail.replace('\\', '/').split("/");
        // do not validate, just apply rules
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;
            if (Objects.equals(trimmed, ".")) continue;
            if (Objects.equals(trimmed, "..")) {
                point = point.getParentFile();
                if (point == null) return null;
                continue;
            }
            point = new File(point, trimmed);
        }
        return point;
    }

    public static String convertToRelativePath(Project project, String path) {
        if (Strings.isEmptyOrSpaces(path)) return path;

        VirtualFile baseDir = project.getBaseDir();
        if (baseDir == null) return path;

        File projectDir = new File(baseDir.getPath());
        String relativePath = com.intellij.openapi.util.io.FileUtil.getRelativePath(projectDir, new File(path));
        if (relativePath == null) return path;

        if (relativePath.lastIndexOf(".." + File.separatorChar) < 1) {
            return relativePath;
        }
        return path;
    }

    public static String convertToAbsolutePath(Project project, String path) {
        if (Strings.isEmptyOrSpaces(path)) return path;

        VirtualFile baseDir = project.getBaseDir();
        if (baseDir == null) return path;

        File projectDir = new File(baseDir.getPath());
        if (new File(path).isAbsolute()) return path;

        File file = Files.createFileByRelativePath(projectDir, path);
        return file == null ? null : file.getPath();
    }

    public static File findFileRecursively(File directory, String fileName) {
        File[] files = directory.listFiles();
        if (files == null) return null;
        for (File file : files) {
            if (Objects.equals(file.getName(), fileName)) {
                return file;
            }
        }

        File[] directories = directory.listFiles(f -> f.isDirectory());
        if (directories == null) return null;
        for (File dir : directories) {
            File file = findFileRecursively(dir, fileName);
            if (file != null) {
                return file;
            }
        }

        return null;
    }

    public static File getPluginDeploymentRoot() {
        IdeaPluginDescriptor pluginDescriptor = DatabaseNavigator.getPluginDescriptor();
        return pluginDescriptor.getPath();
    }

    public static boolean isLightVirtualFile(VirtualFile file) {
        return file instanceof LightVirtualFile;
    }

    public static boolean isDbLanguageFile(VirtualFile file) {
        return file.getFileType() instanceof DBLanguageFileType;
    }

    public static boolean isDbConsoleFile(VirtualFile file) {
        return file instanceof DBConsoleVirtualFile;
    }

    public static boolean isDbLanguagePsiFile(PsiFile psiFile) {
        return psiFile instanceof DBLanguagePsiFile;
    }


    public static boolean isDbEditableObjectFile(@NotNull VirtualFile file) {
        return file instanceof DBEditableObjectVirtualFile;
    }

    public static String getFileName(String path) {
        if (Strings.isEmpty(path)) return path;
        File file = new File(path);

        String name = file.getName();
        int index = name.lastIndexOf(".");
        if (index == -1) return name;
        return name.substring(0, index);
    }
}

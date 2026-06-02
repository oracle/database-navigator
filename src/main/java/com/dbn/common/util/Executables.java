/*
 * Copyright 2026 Oracle and/or its affiliates
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

import com.intellij.openapi.util.SystemInfo;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.util.Strings.isEmptyOrSpaces;

@UtilityClass
public class Executables {

    @Nullable
    public static File resolveExecutableFile(String executablePath) {
        if (isEmptyOrSpaces(executablePath)) return null;

        executablePath = executablePath.trim();
        File configuredFile = new File(executablePath);
        if (configuredFile.isFile()) return canonicalFile(configuredFile);
        if (configuredFile.getParentFile() != null) return null;

        String path = Environment.getVariable("PATH");
        if (isEmptyOrSpaces(path)) return null;

        for (String directoryPath : path.split(File.pathSeparator)) {
            if (isEmptyOrSpaces(directoryPath)) continue;

            File directory = new File(directoryPath);
            for (String executableName : executableNames(executablePath)) {
                File executableFile = new File(directory, executableName);
                if (executableFile.isFile()) return canonicalFile(executableFile);
            }
        }
        return null;
    }

    private static List<String> executableNames(String executablePath) {
        List<String> executableNames = new ArrayList<>();
        executableNames.add(executablePath);

        if (!SystemInfo.isWindows || executablePath.contains(".")) return executableNames;

        String pathExt = Environment.getVariable("PATHEXT");
        if (isEmptyOrSpaces(pathExt)) pathExt = ".EXE;.BAT;.CMD;.COM";

        for (String extension : pathExt.split(";")) {
            if (Strings.isNotEmptyOrSpaces(extension)) executableNames.add(executablePath + extension);
        }
        return executableNames;
    }

    private static File canonicalFile(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException e) {
            return file.getAbsoluteFile();
        }
    }
}

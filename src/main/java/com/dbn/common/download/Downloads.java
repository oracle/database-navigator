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

package com.dbn.common.download;

import com.dbn.common.util.Measured;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.platform.templates.github.DownloadUtil;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * Utility class providing methods for downloading tasks
 * This class is a wrapper around `DownloadUtil` and measures the performance of download tasks.
 *
 * @author Dan Cioca (Oracle)
 */
@UtilityClass
public class Downloads {

    public static void downloadAtomically(
            @Nullable ProgressIndicator progress,
            @NotNull String downloadUrl,
            @NotNull File outputFile) throws IOException {

        Measured.run("downloading " + downloadUrl, () -> DownloadUtil.downloadAtomically(progress, downloadUrl, outputFile));
    }

    public static void downloadContentToFile(
            @Nullable ProgressIndicator progress,
            @NotNull String downloadUrl,
            @NotNull File outputFile) throws IOException {

        Measured.run("downloading " + downloadUrl, () -> DownloadUtil.downloadContentToFile(progress, downloadUrl, outputFile));
    }
}

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

import com.dbn.common.compatibility.Compatibility;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.NlsContexts.DialogTitle;
import com.intellij.openapi.util.NlsContexts.Label;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Utility class providing helpers for file and folder selection using file chooser components.
 *
 * @author Dan Cioca (Oracle)
 */
@UtilityClass
public class FileChoosers {
    @Compatibility
    public static void addSingleFileChooser(
            @Nullable Project project,
            @NotNull TextFieldWithBrowseButton field,
            @Nullable @DialogTitle String title,
            @Nullable @Label String description) {

        field.addBrowseFolderListener(title, description, project, singleFile());
    }

    @Compatibility
    public static void addSingleFolderChooser(
            @Nullable Project project,
            @NotNull TextFieldWithBrowseButton field,
            @Nullable @DialogTitle String title,
            @Nullable @Label String description) {

        field.addBrowseFolderListener(title, description, project, singleFolder());
    }

    public static FileChooserDescriptor singleFile() {
        return new FileChooserDescriptor(true, false, false, false, false, false).withShowHiddenFiles(true);
    }

    public static FileChooserDescriptor singleFolder() {
        return new FileChooserDescriptor(false, true, false, false, false, false).withShowHiddenFiles(true);
    }

    public static FileChooserDescriptor singleFileOrFolder() {
        return new FileChooserDescriptor(true, true, false, false, false, false).withShowHiddenFiles(true);
    }
}

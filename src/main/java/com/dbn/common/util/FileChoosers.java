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
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.NlsContexts.DialogTitle;
import com.intellij.openapi.util.NlsContexts.Label;
import com.intellij.openapi.vfs.VirtualFile;
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
    public static boolean nativeFileChoosers = true;

    public static FileChooserDescriptor addSingleFileChooser(
            @Nullable Project project,
            @NotNull TextFieldWithBrowseButton field,
            @Nullable @DialogTitle String title,
            @Nullable @Label String description) {
        return addFileChooser(project, field, singleFile(), title, description);
    }

    public static FileChooserDescriptor addSingleFolderChooser(
            @Nullable Project project,
            @NotNull TextFieldWithBrowseButton field,
            @Nullable @DialogTitle String title,
            @Nullable @Label String description) {
        return addFileChooser(project, field, singleFolder(), title, description);
    }

    public static FileChooserDescriptor addFileChooser(
            @Nullable Project project,
            @NotNull TextFieldWithBrowseButton field,
            @NotNull FileChooserDescriptor descriptor,
            @Nullable @DialogTitle String title,
            @Nullable @Label String description) {
        descriptor = descriptor
                .withTitle(title)
                .withDescription(description);
        addFileChooser(project, field, descriptor);
        return descriptor;
    }

    @Compatibility
    public static FileChooserDescriptor addFileChooser(
            @Nullable Project project,
            @NotNull TextFieldWithBrowseButton field,
            @NotNull FileChooserDescriptor descriptor) {
        //field.addBrowseFolderListener(project, descriptor);
        field.addBrowseFolderListener(
                descriptor.getTitle(),
                descriptor.getDescription(),
                project,
                descriptor);
        return descriptor;
    }


    public static FileChooserDescriptor singleFile() {
        return adjustFileChooser(new FileChooserDescriptor(true, false, false, false, false, false).withShowHiddenFiles(true));
    }

    public static FileChooserDescriptor singleFolder() {
        return adjustFileChooser(new FileChooserDescriptor(false, true, false, false, false, false));
    }

    public static FileChooserDescriptor singleFolderOrJar() {
        return adjustFileChooser(new FileChooserDescriptor(false, true, true, true, false, false));
    }

    public static FileChooserDescriptor singleFileOrFolder() {
        return adjustFileChooser(new FileChooserDescriptor(true, true, false, false, false, false));
    }

    public static FileChooserDescriptor multipleFiles() {
        return adjustFileChooser(new FileChooserDescriptor(true, true, false, false, false, true));
    }


    public static Condition<? super VirtualFile> extensionFilter(String extension) {
        return (Condition<VirtualFile>) file -> Strings.equalsIgnoreCase(file.getExtension(), extension);
    }

    public static Condition<? super VirtualFile> extensionFilter(String ... extensions) {
        return (Condition<VirtualFile>) file -> {
            for (String extension : extensions) {
                if (Strings.equalsIgnoreCase(file.getExtension(), extension)) return true;
            }
            return false;
        };
    }

    private static FileChooserDescriptor adjustFileChooser(FileChooserDescriptor descriptor) {
        descriptor.setForcedToUseIdeaFileChooser(!nativeFileChoosers);
        return descriptor;
    }
}

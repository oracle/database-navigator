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

package com.dbn.connection.config.tns;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.ApplicationComponentBase;
import com.dbn.common.component.PersistentState;
import com.dbn.common.routine.Consumer;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.config.tns.ui.TnsNamesImportDialog;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.io.File;

import static com.dbn.common.component.Components.applicationService;
import static com.dbn.common.options.setting.Settings.getEnum;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setEnum;
import static com.dbn.connection.config.tns.TnsImportService.COMPONENT_NAME;

@Getter
@Setter
@State(
    name = COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class TnsImportService extends ApplicationComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Application.TnsImportService";
    private TnsImportType importType = TnsImportType.FIELDS;

    private TnsImportService() {
        super(COMPONENT_NAME);
    }

    public static TnsImportService getInstance() {
        return applicationService(TnsImportService.class);
    }


    public void importTnsNames(Project project, Consumer<TnsImportData> consumer) {
        VirtualFile[] virtualFiles = FileChooser.chooseFiles(TnsNamesParser.FILE_CHOOSER_DESCRIPTOR, project, null);
        if (virtualFiles.length != 1) return;

        File file = new File(virtualFiles[0].getPath());
        Dialogs.show(() -> new TnsNamesImportDialog(project, file), (dialog, exitCode) -> {
            if (exitCode != DialogWrapper.OK_EXIT_CODE) return;
            consumer.accept(dialog.getImportData());
        });
    }

    @Override
    public Element getComponentState() {
        Element element = new Element("state");
        Element optionsElement = newElement(element, "tns-import-options");

        setEnum(optionsElement, "import-type", importType);
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        Element optionsElement = element.getChild("tns-import-options");
        if (optionsElement == null) return;

        importType = getEnum(optionsElement, "import-type", importType);
    }
}

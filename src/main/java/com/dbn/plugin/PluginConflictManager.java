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

package com.dbn.plugin;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.ApplicationComponentBase;
import com.dbn.common.component.PersistentState;
import com.dbn.common.file.FileTypeService;
import com.dbn.common.util.Dialogs;
import com.dbn.language.common.DBLanguageFileType;
import com.dbn.language.psql.PSQLFileType;
import com.dbn.language.sql.SQLFileType;
import com.dbn.plugin.ui.PluginConflictResolutionDialog;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.component.Components.applicationService;
import static com.dbn.common.options.setting.Settings.getBoolean;
import static com.dbn.common.options.setting.Settings.setBoolean;
import static com.dbn.plugin.DBPluginStatus.ACTIVE;
import static com.dbn.plugin.DBPluginStatus.MISSING;
import static com.dbn.plugin.DBPluginStatus.PASSIVE;
import static com.dbn.plugin.PluginConflictManager.COMPONENT_NAME;

@Getter
@Setter
@State(
    name = COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class PluginConflictManager extends ApplicationComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Application.PluginConflictManager";

    private boolean fileTypesClaimed;
    private boolean conflictPrompted;

    public PluginConflictManager() {
        super(COMPONENT_NAME);
    }

    public static PluginConflictManager getInstance() {
        return applicationService(PluginConflictManager.class);
    }

    public void assesPluginConflict(Project project) {
        if (conflictPrompted) return;

        PluginStatusManager statusManager = PluginStatusManager.getInstance();
        DBPluginStatus sqlPluginStatus = statusManager.getSqlPluginStatus();
        DBPluginStatus dbnPluginStatus = statusManager.getDbnPluginStatus();

        if (dbnPluginStatus == ACTIVE)  {
            if (sqlPluginStatus == ACTIVE) {
                // both plugins in use, show resolution dialog
                showConflictResolutionDialog();
            } else {
                // missing or passive SQL plugin - favor DBN
                claimFileAssociations(false);
            }
        } else if (dbnPluginStatus == PASSIVE) {
            if (sqlPluginStatus == ACTIVE) {
                // SQL plugin user - restore default file associations
                restoreFileAssociations();
                
            } else if (sqlPluginStatus == PASSIVE) {
                // both plugins inactive - do nothing yet

                // ... or show conflict resolution?
                // showConflictResolutionDialog();
                
            } else if (sqlPluginStatus == MISSING) {
                // missing SQL plugin - favor DBN
                claimFileAssociations(false);
            }
        }
    }

    private void showConflictResolutionDialog() {
        conflictPrompted = true;
        Dialogs.show(() -> new PluginConflictResolutionDialog());
    }

    public void applyConflictResolution(PluginConflictResolution resolution) {
        switch (resolution) {
            case DISABLE_PLUGIN: disablePlugin(); return;
            case CONTINUE_FEATURED: claimFileAssociations(true); return;
            case CONTINUE_LIMITED: restoreFileAssociations(); return;
            case DECIDE_LATER: conflictPrompted = false; return;
            default:
        }
    }

    private void claimFileAssociations(boolean force) {
        // do not claim again if already claimed once (even if no longer associated with DBN)
        if (fileTypesClaimed && !force) return;

        FileTypeService fileTypeService = FileTypeService.getInstance();
        try {
            fileTypeService.claimFileAssociations(SQLFileType.INSTANCE);
            fileTypeService.claimFileAssociations(PSQLFileType.INSTANCE);
        } finally {
            if (force) fileTypesClaimed = true;
        }
    }

    private boolean areFileTypesClaimed() {
        FileTypeService fileTypeService = FileTypeService.getInstance();
        FileType fileType = fileTypeService.getCurrentFileType("sql");
        return fileType instanceof DBLanguageFileType;
    }

    private void restoreFileAssociations() {
        FileTypeService fileTypeService = FileTypeService.getInstance();
        fileTypeService.restoreFileAssociations();
    }

    private void disablePlugin() {
        // prompt again if needed on reinstall
        conflictPrompted = false;

        String pluginId = DatabaseNavigator.DBN_PLUGIN_ID.getIdString();
        PluginManager.disablePlugin(pluginId);
        ApplicationManagerEx.getApplicationEx().restart(true);
    }

    /**************************************************************************
     *                       PersistentStateComponent                         *
     **************************************************************************/

    @Override
    public Element getComponentState() {
        Element element = new Element("state");
        setBoolean(element, "plugin-conflict-prompted", conflictPrompted);
        setBoolean(element, "file-types-claimed", fileTypesClaimed);

        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        conflictPrompted = getBoolean(element, "plugin-conflict-prompted", conflictPrompted);
        fileTypesClaimed = getBoolean(element, "file-types-claimed", fileTypesClaimed);
    }


}

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

package com.dbn.mcp.build;

import com.dbn.common.util.Environment;
import com.dbn.common.util.Messages;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.PluginsAdvertiser;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.project.MavenProjectBundle;

import java.util.Set;

import static com.dbn.common.util.Conditional.when;
import static com.dbn.common.util.Messages.options;
import static com.dbn.common.util.Messages.showAcknowledgementDialog;
import static com.dbn.nls.NlsResources.txt;

@Slf4j
public final class McpMavenPluginSupport {
    private static final PluginId MAVEN_PLUGIN_ID = PluginId.getId("org.jetbrains.idea.maven");

    private McpMavenPluginSupport() {}

    public static void verifyMavenAvailability(@NotNull Project project) {
        if (!isMavenPluginAvailable()) {
            int option = Messages.showConfirmationDialog(project,
                    txt("msg.mcp.title.MavenPluginRequired"),
                    txt("msg.mcp.question.MavenPluginRequired"),
                    options(txt("msg.mcp.button.OpenPluginInstaller"), txt("msg.shared.button.Cancel")), 0);
            if (option == 0) {
                openMavenPluginInstaller(project);
            }
            throw new ProcessCanceledException();
        }

        if (!isMavenAvailable(project)) {
            int option = Messages.showConfirmationDialog(project,
                    txt("msg.mcp.title.MavenRuntimeRequired"),
                    txt("msg.mcp.question.MavenRuntimeRequired"),
                    options(txt("msg.mcp.button.OpenMavenSettings"), txt("msg.shared.button.Cancel")), 0);
            if (option == 0) {
                openMavenPluginSettings(project);
            }
            throw new ProcessCanceledException();
        }
    }

    public static boolean isMavenAvailable(@Nullable Project project) {
        if (project == null) return false;
        if (!isMavenPluginAvailable()) return false;

        try {
            McpMavenBuildManager mavenManager = McpMavenBuildManager.getInstance(project);
            return mavenManager != null && mavenManager.isRuntimeAvailable();
        } catch (Throwable e) {
            log.warn("Could not resolve Maven runtime", e);
            return false;
        }
    }

    public static boolean isMavenPluginAvailable() {
        return PluginManagerCore.isPluginInstalled(MAVEN_PLUGIN_ID) && !PluginManagerCore.isDisabled(MAVEN_PLUGIN_ID);
    }

    public static void openMavenPluginInstaller(@Nullable Project project) {
        PluginsAdvertiser.installAndEnable(
                project,
                Set.of(MAVEN_PLUGIN_ID),
                true,
                true,
                () -> {});
    }

    public static void openMavenPluginSettings(@Nullable Project project) {
        try {
            String mavenSettingsName = MavenProjectBundle.message("configurable.MavenSettings.display.name");
            ShowSettingsUtil.getInstance().showSettingsDialog(project, mavenSettingsName);
        } catch (Throwable e) {
            showAcknowledgementDialog(project,
                    txt("msg.mcp.title.MavenSettingsUnavailable"),
                    txt("msg.mcp.question.MavenSettingsUnavailable", Environment.getIdeName()),
                    options(txt("msg.mcp.button.RestartIde", Environment.getIdeName()), txt("msg.shared.button.Cancel")), 0, o -> when(o == 0, () -> {
                        ApplicationEx app = (ApplicationEx) ApplicationManager.getApplication();
                        app.restart(true);
                    }));
        }
    }
}

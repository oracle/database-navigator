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

package com.dbn.assistant.settings;

import com.dbn.assistant.credential.AssistantCredentialSettings;
import com.dbn.assistant.mcp.AssistantMcpServerSettings;
import com.dbn.assistant.profile.AssistantProfileSettings;
import com.dbn.assistant.settings.ui.AssistantSettingsForm;
import com.dbn.common.options.CompositeProjectConfiguration;
import com.dbn.common.options.Configuration;
import com.dbn.help.HelpTopic;
import com.dbn.options.ConfigId;
import com.dbn.options.ProjectSettings;
import com.dbn.options.TopLevelConfig;
import com.intellij.openapi.project.Project;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.util.Commons.array;
import static com.dbn.help.HelpTopic.ASSISTANT_CONFIG;

@Getter
@EqualsAndHashCode(callSuper = false)
public class AssistantSettings
    extends CompositeProjectConfiguration<ProjectSettings, AssistantSettingsForm>
    implements TopLevelConfig {

  private final AssistantCredentialSettings credentialSettings;
  private final AssistantProfileSettings profileSettings;
  private final AssistantMcpServerSettings mcpServerSettings;

  public AssistantSettings(ProjectSettings parent) {
    super(parent);
    this.credentialSettings = new AssistantCredentialSettings(this);
    this.profileSettings = new AssistantProfileSettings(this);
    this.mcpServerSettings = new AssistantMcpServerSettings(this);
  }

  @NotNull
  @Override
  public AssistantSettingsForm createConfigurationEditor() {
    return new AssistantSettingsForm(this);
  }

  public static AssistantSettings getInstance(@NotNull Project project) {
    return ProjectSettings.get(project).getAssistantSettings();
  }

  @NotNull
  @Override
  public String getId() {
    return "DBNavigator.Project.AssistantSettings";
  }

  @Override
  public String getDisplayName() {
    return txt("cfg.assistant.title.Assistant");
  }

  @Override
  public HelpTopic getConfigHelpTopic() {
    return ASSISTANT_CONFIG;
  }

  @Override
  public ConfigId getConfigId() {
    return ConfigId.ASSISTANT;
  }

  @NotNull
  @Override
  public AssistantSettings getOriginalSettings() {
    return getInstance(getProject());
  }

  /*********************************************************
   *                     Configuration                     *
   *********************************************************/

  @Override
  protected Configuration[] createConfigurations() {
    return array(
            credentialSettings,
            profileSettings,
            mcpServerSettings);
  }

  @Override
  public String getConfigElementName() {
    return "assistant-settings";
  }
}

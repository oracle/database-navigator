package com.dbn.oci;

import com.dbn.connection.config.ConnectionSettings;
import com.dbn.oci.actions.CreateConnectionDBNAction;
import com.dbn.oci.actions.OpenConnectionDBNAction;
import com.dbn.options.ProjectSettingsManager;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.project.Project;
import com.oracle.oci.intellij.api.ext.ContributeADBActions;
import com.oracle.oci.intellij.api.ext.UIModelContext;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public class ContributeADBActionsImpl implements ContributeADBActions {



  private @NotNull PluginDescriptor pluginDescriptor;

  @Override
  public void setPluginDescriptor(@NotNull PluginDescriptor pluginDescriptor) {
    this.pluginDescriptor = pluginDescriptor;
  }

  public List<ExtensionContextAction> getModelContextActions(final UIModelContext context) {
    List<ExtensionContextAction>  actions = new ArrayList<>();
    ConnectionData connectionData = ConnectionData.toConnectionSettings(context);
    addActions(connectionData,actions);
    return actions;
  }

  private void addActions(ConnectionData connectionData, List<ExtensionContextAction> actions) {
    actions.add(new CreateConnectionDBNAction(connectionData,"New Connection...", ExtensionContextAction.ActionType.NEW));
    DataContext dataContext = DataManager.getInstance().getDataContext();
    Project project =  dataContext.getData(CommonDataKeys.PROJECT);
    ProjectSettingsManager pManager = ProjectSettingsManager.getInstance(project);
    List<ConnectionSettings> connections = pManager.getConnectionSettings().getConnections();
    List<ConnectionSettings> connectionSettings =  connections.stream().filter((c)-> {
      c.getId();
      return Objects.equals(connectionData.getOcid(),c.getSourceId());
    }).collect(Collectors.toList());

    for (ConnectionSettings connectionSetting : connectionSettings) {
      actions.add(new OpenConnectionDBNAction(connectionSetting,connectionSetting.getDatabaseSettings().getDisplayName()));
    }

  }
}

package com.dbn.oci;

import com.dbn.connection.config.ConnectionSettings;
import com.dbn.oci.actions.CreateConnectionDBNAction;
import com.dbn.oci.actions.DBNSubMenuAction;
import com.dbn.oci.actions.OpenConnectionDBNAction;
import com.dbn.options.ProjectSettingsManager;
import com.intellij.ide.DataManager;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Version;
import com.oracle.oci.intellij.api.ext.ContributeADBActions;
import com.oracle.oci.intellij.api.ext.UIModelContext;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


public class ContributeADBActionsImpl implements ContributeADBActions {

  public static final String OCI_PLUGIN_ID = "com.oracle.ocidbtest";

  private static final Version OCI_PLUGIN_MIN_VERSION = Version.parseVersion("1.2.0");
  private @NotNull PluginDescriptor pluginDescriptor;

  @Override
  public void setPluginDescriptor(@NotNull PluginDescriptor pluginDescriptor) {
    this.pluginDescriptor = pluginDescriptor;
  }

  @Override
  public boolean isCompatible(Version frameworkVersion) {
    // >= 1.2
    return frameworkVersion.compareTo(OCI_PLUGIN_MIN_VERSION) >= 0;
  }

  @Override
  public Optional<PluginDescriptor> getPluginDescriptor() {
    return Optional.of(this.pluginDescriptor);
  }

  public List<ExtensionContextAction> getModelContextActions(final UIModelContext context) {
    List<ExtensionContextAction>  actions = new ArrayList<>();
    PluginId pluginId = PluginId.getId(OCI_PLUGIN_ID);
    IdeaPluginDescriptor ociPluginDesc = PluginManagerCore.getPlugin(pluginId);
    String versionStr = ociPluginDesc.getVersion();
    Version version = Version.parseVersion(versionStr);
    // ignore this extension if it's version is less than 1.2
    if (version.compareTo(OCI_PLUGIN_MIN_VERSION) >= 0) {
      ConnectionData connectionData = ConnectionData.toConnectionSettings(context);
      addActions(connectionData, actions);
    }
    return actions;
  }

  private void addActions(ConnectionData connectionData, List<ExtensionContextAction> actions) {
    List<ExtensionContextAction> subActions = new ArrayList<>();
    subActions.add(new CreateConnectionDBNAction(connectionData,"New Connection..."));
    DataContext dataContext = DataManager.getInstance().getDataContext();
    Project project =  dataContext.getData(CommonDataKeys.PROJECT);
    ProjectSettingsManager pManager = ProjectSettingsManager.getInstance(project);
    List<ConnectionSettings> connections = pManager.getConnectionSettings().getConnections();
    List<ConnectionSettings> connectionSettings =  connections.stream().filter((c)-> {
      c.getId();
      return Objects.equals(connectionData.getOcid(),c.getSourceId());
    }).collect(Collectors.toList());

    for (ConnectionSettings connectionSetting : connectionSettings) {
      subActions.add(new OpenConnectionDBNAction(connectionSetting,connectionSetting.getDatabaseSettings().getDisplayName()));
    }
    DBNSubMenuAction subMenuAction = new DBNSubMenuAction("DBN Connections", subActions);
    actions.add(subMenuAction);
  }
}

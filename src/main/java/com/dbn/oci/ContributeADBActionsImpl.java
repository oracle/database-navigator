package com.dbn.oci;

import com.dbn.oci.actions.CreateConnectionDBNAction;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.oracle.oci.intellij.api.ext.ContributeADBActions;
import com.oracle.oci.intellij.api.ext.UIModelContext;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.nls.NlsResources.txt;

public class ContributeADBActionsImpl implements ContributeADBActions {



  private @NotNull PluginDescriptor pluginDescriptor;

  @Override
  public void setPluginDescriptor(@NotNull PluginDescriptor pluginDescriptor) {
    this.pluginDescriptor = pluginDescriptor;
  }

  public List<ExtensionContextAction> getModelContextActions(final UIModelContext context) {
    List<ExtensionContextAction>  actions = new ArrayList<>();
    addActions(context,actions);
    return actions;
  }

  private void addActions(UIModelContext context, List<ExtensionContextAction> actions) {
    actions.add(new CreateConnectionDBNAction(context,"Create Connection in DBN"));
    //todo add quick connection to db
  }
}

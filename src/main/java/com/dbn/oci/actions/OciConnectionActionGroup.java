package com.dbn.oci.actions;

import com.oracle.oci.intellij.api.ext.ContributeADBActions;
import org.jetbrains.annotations.NotNull;

import java.awt.event.ActionEvent;
import java.util.List;

public class OciConnectionActionGroup extends ContributeADBActions.ExtensionContextAction {
    public OciConnectionActionGroup(String displayName, @NotNull List<ContributeADBActions.ExtensionContextAction> subMenu) {
        super(displayName, subMenu);
    }

    @Override
    protected void doAction(ActionEvent actionEvent) {
        // do nothing.  this is just an intermediate menu node
    }
}

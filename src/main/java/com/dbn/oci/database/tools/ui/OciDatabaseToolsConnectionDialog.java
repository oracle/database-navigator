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

package com.dbn.oci.database.tools.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.oci.config.OciAuthenticationConfig;
import com.dbn.oci.database.tools.OciDatabaseToolsConnectionInfo;
import com.intellij.openapi.project.Project;

import javax.swing.Action;

import static com.dbn.nls.NlsResources.txt;

public class OciDatabaseToolsConnectionDialog extends DBNDialog<OciDatabaseToolsConnectionForm> {
    private final OciAuthenticationConfig authentication;
    private OciDatabaseToolsConnectionInfo selectedConnection;

    public OciDatabaseToolsConnectionDialog(Project project, OciAuthenticationConfig authentication) {
        super(project, txt("msg.oci.title.SelectDatabaseToolsConnection"), true);
        this.authentication = authentication;
        setDefaultSize(720, 440);
        init();
        setOKActionEnabled(false);
    }

    @Override
    protected OciDatabaseToolsConnectionForm createForm() {
        return new OciDatabaseToolsConnectionForm(this, authentication);
    }

    @Override
    protected Action[] initializeActions() {
        renameAction(getOKAction(), txt("msg.shared.button.Select"));
        return actions(getOKAction(), getCancelAction());
    }

    void setSelectedConnection(OciDatabaseToolsConnectionInfo selectedConnection) {
        this.selectedConnection = selectedConnection;
        setOKActionEnabled(selectedConnection != null);
    }

    void acceptSelectedConnection() {
        if (selectedConnection != null) doOKAction();
    }

    public OciDatabaseToolsConnectionInfo getSelectedConnection() {
        return selectedConnection;
    }
}

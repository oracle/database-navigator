/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.oci.config.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.oci.config.OciConfig;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

import static com.dbn.nls.NlsResources.txt;

@Getter
public class OciConfigSelectionDialog extends DBNDialog<OciConfigSelectionForm> {
    private final OciConfig config = new OciConfig();

    public OciConfigSelectionDialog(Project project) {
        super(project, txt("msg.oci.title.SelectOciConfig"), false);

        init();
    }
    @Override
    protected @NotNull OciConfigSelectionForm createForm() {
        return new OciConfigSelectionForm(this, config);
    }

    @Override
    protected Action[] initializeActions() {
    renameAction(getOKAction(), txt("msg.shared.button.Select"));
        return actions(
                getOKAction(),
                getCancelAction());
    }

    @Override
    protected void doOKAction() {
        applyFormChanges();
        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        applyFormChanges(); // preserve input even if canceled
        super.doCancelAction();
    }
}

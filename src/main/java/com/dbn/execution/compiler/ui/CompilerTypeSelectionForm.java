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

package com.dbn.execution.compiler.ui;

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.object.common.DBSchemaObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.awt.BorderLayout;

import static com.dbn.common.text.TextContent.plain;

public class CompilerTypeSelectionForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JCheckBox rememberSelectionCheckBox;
    private JPanel hintPanel;

    CompilerTypeSelectionForm(final CompilerTypeSelectionDialog parent, @Nullable DBSchemaObject object) {
        super(parent);
        if (object == null) {
            headerPanel.setVisible(false);
        } else {
            DBNHeaderForm headerForm = new DBNHeaderForm(this, object);
            headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);
        }
        TextContent hintText = plain(
                "The compile option type \"Debug\" enables you to use the selected object(s) in debugging activities (i.e. pause/trace execution). " +
                        "For runtime performance reasons, it is recommended to use normal compile option, unless you plan to debug the selected element(s)." +
                        "\n\"Keep current\" will carry over the existing compile type.\n\n" +
                        "Please select your compile option.");
        DBNHintForm hintForm = new DBNHintForm(this, hintText, null, true);
        hintPanel.add(hintForm.getComponent(), BorderLayout.CENTER);

        parent.registerRememberSelectionCheckBox(rememberSelectionCheckBox);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

}

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

package com.dbn.event.registration.ui;

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.link.HyperLinkForm;
import com.dbn.object.DBTable;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

import static com.dbn.common.text.TextContent.plain;

public class EventRegistrationInputForm extends DBNFormBase {
    private JPanel mainPanel;
    private JCheckBox insertCheckBox;
    private JCheckBox updateCheckBox;
    private JCheckBox deleteCheckBox;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JPanel hyperlinkPanel;

    private final DBObjectRef<DBTable> table;

    public EventRegistrationInputForm(@Nullable Disposable parent, final DBTable table) {
        super(parent);
        this.table = DBObjectRef.of(table);

        initHeaderForm();
        initHintForm();
        initPoweredByPanel();
    }

    private void initHeaderForm() {
        DBNHeaderForm headerForm = new DBNHeaderForm(this, table);
        headerPanel.add(headerForm.getComponent());
    }

    private void initHintForm() {
        TextContent hintText = plain("Register a listener to receive notifications on data changes in the " + table.getObjectName(true) + " table.\n" +
                "You will get real-time updates on inserts, updates, or deletes (depending on which actions you select for registration).\n\n" +
                "Please select the actions you want to receive notifications for.");
        DBNHintForm hintForm = new DBNHintForm(this, hintText, null, true);
        hintPanel.add(hintForm.getComponent());

    }

    private void initPoweredByPanel() {
        HyperLinkForm hyperLinkForm = HyperLinkForm.create(
                "Powered by",
                "Oracle DCN",
                "https://docs.oracle.com/cd/B19306_01/B14251_01/adfns_dcn.htm");
        hyperlinkPanel.add(hyperLinkForm.getComponent(), BorderLayout.EAST);
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    public boolean isInsert() {
        return insertCheckBox.isSelected();
    }

    public boolean isUpdate() {
        return updateCheckBox.isSelected();
    }

    public boolean isDelete() {
        return deleteCheckBox.isSelected();
    }
}

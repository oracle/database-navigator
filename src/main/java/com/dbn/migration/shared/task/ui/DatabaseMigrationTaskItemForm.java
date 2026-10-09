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

package com.dbn.migration.shared.task.ui;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.info.DBNTextBlock;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.dbn.common.ui.util.Fonts;
import com.dbn.common.util.Strings;
import com.dbn.migration.shared.task.DatabaseMigrationTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import static com.dbn.common.ui.link.Hyperlinks.initHyperlink;
import static com.dbn.nls.NlsResources.txt;

/**
 * Dashboard item form presenting one migration task and its documentation link.
 */
public class DatabaseMigrationTaskItemForm extends DBNFormBase {
    private JPanel mainPanel;
    private JLabel nameLabel;
    private DBNTextBlock descriptionLabel;
    private JButton openButton;
    private DBNHyperlinkLabel moreHyperlinkLabel;

    public DatabaseMigrationTaskItemForm(
            @NotNull DBNFormBase parent,
            @NotNull DatabaseMigrationTask item,
            @NotNull Runnable action) {
        this(parent, item.getDashboardName(), item.getDashboardDescription(), item.getDashboardDocumentationUrl(), action);
    }

    private DatabaseMigrationTaskItemForm(
            @NotNull DBNFormBase parent,
            @NotNull String name,
            @NotNull String description,
            @Nullable String documentationUrl,
            @NotNull Runnable action) {
        super(parent);

        nameLabel.setText(name);
        descriptionLabel.setText(description);
        nameLabel.setFont(Fonts.regular(1));
        openButton.addActionListener(e -> action.run());

        if (Strings.isEmpty(documentationUrl)) {
            moreHyperlinkLabel.setVisible(false);
        } else {
            initHyperlink(moreHyperlinkLabel, txt("app.shared.link.ShowMore"), documentationUrl);
        }
    }

    public void setOperationAvailable(boolean available) {
        openButton.setEnabled(available);
    }

    @Override
    protected JPanel getMainComponent() {
        return mainPanel;
    }
}

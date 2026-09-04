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

package com.dbn.liquibase.operation.ui;

import com.dbn.common.color.Colors;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.dbn.common.ui.util.Components;
import com.dbn.common.ui.util.Fonts;
import com.dbn.common.util.Strings;
import com.dbn.liquibase.task.LiquibaseTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.Dimension;

import static com.dbn.common.ui.link.Hyperlinks.initHyperlink;
import static com.dbn.nls.NlsResources.txt;
import static com.intellij.util.ui.UIUtil.getLabelForeground;

/** Operation dashboard item form presenting one Liquibase operation and its documentation link. */
public class LiquibaseDashboardItemForm extends DBNFormBase {
    private JPanel mainPanel;
    private JLabel nameLabel;
    private JTextPane descriptionTextPane;
    private JButton openButton;
    private DBNHyperlinkLabel moreHyperlinkLabel;
    private int descriptionWidth;

    public LiquibaseDashboardItemForm(
            @NotNull DBNFormBase parent,
            @NotNull LiquibaseTask item,
            @NotNull Runnable action) {
        this(parent, item.getDashboardName(), item.getDashboardDescription(), item.getDashboardDocumentationUrl(), action);
    }

    private LiquibaseDashboardItemForm(
            @NotNull DBNFormBase parent,
            @NotNull String name,
            @NotNull String description,
            @Nullable String documentationUrl,
            @NotNull Runnable action) {
        super(parent);

        nameLabel.setText(name);
        descriptionTextPane.setText(description);
        nameLabel.setFont(Fonts.regular(1));
        descriptionTextPane.setFocusable(false);
        descriptionTextPane.setForeground(Colors.faded(getLabelForeground()));
        openButton.setEnabled(false);
        openButton.addActionListener(e -> action.run());

        if (Strings.isEmpty(documentationUrl)) {
            moreHyperlinkLabel.setVisible(false);
        } else {
            initHyperlink(moreHyperlinkLabel, txt("app.shared.link.ShowMore"), documentationUrl);
        }

        whenFirstShown(() -> installDescriptionResizer());
    }

    private void installDescriptionResizer() {
        if (!(mainPanel.getParent() instanceof JPanel parent)) return;
        if (!(parent.getLayout() instanceof BoxLayout)) return;

        Components.onComponentResized(parent, e -> dispatch(this::resizeDescription));
        resizeDescription();
    }

    private void resizeDescription() {
        int width = descriptionTextPane.getWidth();
        if (width <= 0 || width == descriptionWidth) return;

        descriptionTextPane.setPreferredSize(null);
        descriptionTextPane.setSize(width, Integer.MAX_VALUE);
        int height = descriptionTextPane.getPreferredSize().height;
        descriptionTextPane.setPreferredSize(new Dimension(150, height));
        descriptionTextPane.revalidate();

        Dimension maximumSize = mainPanel.getMaximumSize();
        mainPanel.setMaximumSize(new Dimension(maximumSize.width, mainPanel.getPreferredSize().height));
        mainPanel.revalidate();
        descriptionWidth = width;
    }

    public void setOperationAvailable(boolean available) {
        openButton.setEnabled(available);
    }

    @Override
    protected JPanel getMainComponent() {
        return mainPanel;
    }
}

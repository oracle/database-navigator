/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.liquibase.ui;

import com.dbn.common.color.Colors;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.dbn.common.ui.util.Components;
import com.dbn.common.ui.util.Fonts;
import com.dbn.liquibase.execution.LiquibaseOperation;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.Dimension;

import static com.dbn.common.ui.link.Hyperlinks.initHyperlink;
import static com.dbn.nls.NlsResources.txt;
import static com.intellij.util.ui.UIUtil.getLabelForeground;

/** Dashboard item form presenting one Liquibase operation and its documentation link. */
public class LiquibaseDashboardOperationForm extends DBNFormBase {
    private JPanel mainPanel;
    private JLabel nameLabel;
    private JTextPane descriptionTextPane;
    private JButton openButton;
    private DBNHyperlinkLabel moreHyperlinkLabel;
    private int descriptionWidth;

    public LiquibaseDashboardOperationForm(
            @NotNull LiquibaseDashboardForm parent,
            @NotNull LiquibaseOperation operation,
            @NotNull Runnable action) {
        super(parent);

        nameLabel.setText(operation.getName());
        descriptionTextPane.setText(operation.getHint());
        nameLabel.setFont(Fonts.regular(1));
        descriptionTextPane.setFocusable(false);
        descriptionTextPane.setForeground(Colors.faded(getLabelForeground()));
        openButton.setEnabled(false);
        openButton.addActionListener(e -> action.run());

        initHyperlink(moreHyperlinkLabel, txt("app.shared.link.ShowMore"), operation.getDocumentationUrl());

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

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

package com.dbn.driver.packages.ui;

import com.dbn.common.environment.EnvironmentType;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.panel.DBNCollapsiblePanel;
import com.dbn.driver.packages.DriverPackage;
import com.dbn.driver.packages.Library;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;

public class DriverPackageInfoForm extends DBNFormBase {
    private JPanel headerPanel;
    private JPanel mainPanel;
    private JPanel infoPanel;

    public DriverPackageInfoForm(@Nullable Disposable parent, DriverPackage driverPackage) {
        super(parent);

        initHeaderPanel(driverPackage.getId(), EnvironmentType.DEVELOPMENT);
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        for (Library library : driverPackage.getLibraries()) {
            LibraryInfoForm libraryInfoForm = new LibraryInfoForm(library);
            DBNCollapsiblePanel collapsiblePanel = new DBNCollapsiblePanel(this, libraryInfoForm, false);

            JComponent mainComponent = collapsiblePanel.getMainComponent();
            Dimension preferredSize = mainComponent.getPreferredSize();
            mainComponent.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferredSize.height));

            infoPanel.add(mainComponent);
        }
        infoPanel.add(Box.createVerticalGlue());
        JScrollPane scrollPane = new JScrollPane(infoPanel);
        scrollPane.setPreferredSize(new Dimension(500, 400));

        mainPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private void initHeaderPanel(String libraryName, EnvironmentType environmentType) {
        DBNHeaderForm headerForm = new DBNHeaderForm(this);
        headerForm.setTitle(libraryName);
        headerForm.setBackground(environmentType.getColor());
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);
        headerPanel.setBorder(new EmptyBorder(0, 0, 10, 0));

    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
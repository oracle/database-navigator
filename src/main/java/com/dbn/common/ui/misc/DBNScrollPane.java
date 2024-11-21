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

package com.dbn.common.ui.misc;

import com.dbn.common.ui.util.Borderless;
import com.dbn.common.ui.util.ClientProperty;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Component;

public class DBNScrollPane extends JBScrollPane {


    public DBNScrollPane() {
    }

    public DBNScrollPane(Component view) {
        super(view);
    }

    @Override
    public void setViewportView(Component view) {
        super.setViewportView(view);
        if (view == null) return;

        adjustBackground();
        view.addPropertyChangeListener("background", e -> adjustBackground());
    }

    private void adjustBackground() {
        Component component = getViewComponent();
        if (component == null) return;

        Color background = component.getBackground();
        viewport.setBackground(background);
        setBackground(background);
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
    }

    @Override
    public void setBorder(Border border) {
        Component component = getViewComponent();

        Border clientBorder = ClientProperty.BORDER.get(component);
        if (clientBorder != null) border = clientBorder;
        if (Borderless.isBorderless(component)) border = null;

        super.setBorder(border);
    }

    protected Component getViewComponent() {
        return getViewport().getView();
    }



}

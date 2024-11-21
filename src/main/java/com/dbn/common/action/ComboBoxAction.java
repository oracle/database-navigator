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

package com.dbn.common.action;

import com.dbn.nls.NlsSupport;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

public abstract class ComboBoxAction
        extends com.intellij.openapi.actionSystem.ex.ComboBoxAction
        implements BackgroundUpdateAware, NlsSupport {

    @NotNull
    @Override
    public JComponent createCustomComponent(@NotNull Presentation presentation, @NotNull String place) {
        JPanel panel=new JPanel(new GridBagLayout());
        ComboBoxButton button = new ComboBoxButton(presentation);
        GridBagConstraints constraints = new GridBagConstraints(
                0, 0, 1, 1, 1, 1,
                GridBagConstraints.CENTER,
                GridBagConstraints.BOTH, JBUI.insets(3), 0, 0);
        panel.add(button, constraints);
        panel.setFocusable(false);
        return panel;
    }

    @NotNull
    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return resolveActionUpdateThread();
    }

}

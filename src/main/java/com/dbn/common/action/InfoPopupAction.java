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

package com.dbn.common.action;

import com.dbn.common.icon.Icons;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.DBNTooltip;
import com.dbn.common.ui.info.DBNInfoForm;
import com.intellij.ide.IdeTooltipManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.awt.event.InputEvent;

public abstract class InfoPopupAction extends BasicAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setIcon(Icons.ACTION_INFO);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        InputEvent event = e.getInputEvent();
        if (event == null) return; // should not happen

        JComponent component = (JComponent) event.getComponent();
        if (component == null) return; // should not happen


        DBNInfoForm infoForm = new DBNInfoForm(null, getPopupContent());
        DBNTooltip tooltip = new DBNTooltip(component, component.getLocationOnScreen(), infoForm.getComponent());

        IdeTooltipManager tooltipManager = IdeTooltipManager.getInstance();
        tooltipManager.show(tooltip, true);
    }

    protected abstract TextContent getPopupContent();
}

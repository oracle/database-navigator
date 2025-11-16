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

package com.dbn.common.ui.info;

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.DBNTooltip;
import com.dbn.common.ui.util.Cursors;
import com.dbn.common.ui.util.Mouse;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeTooltip;
import com.intellij.ide.IdeTooltipManager;
import lombok.Setter;

import javax.swing.JLabel;
import java.awt.event.MouseEvent;

@Setter
public class DBNInfoLabel extends JLabel {
    private TextContent content;
    private DBNTooltip popup;

    public DBNInfoLabel() {
        super("", AllIcons.General.Note, JLabel.LEFT);
        setCursor(Cursors.handCursor());

        Mouse.onMousePress(this, 1, e -> showTooltip(e));
    }

    private void showTooltip(MouseEvent e) {

        DBNInfoForm infoForm = new DBNInfoForm(null, content);
        popup = new DBNTooltip(this, getLocation(), infoForm.getComponent());

        IdeTooltipManager tooltipManager = IdeTooltipManager.getInstance();

        IdeTooltip tooltip = tooltipManager.show(popup, true);

    }
}

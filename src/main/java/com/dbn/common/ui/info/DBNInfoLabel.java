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
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.DBNTooltip;
import com.dbn.common.ui.util.Cursors;
import com.dbn.common.ui.util.Mouse;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeTooltipManager;
import lombok.Setter;

import javax.swing.JComponent;
import javax.swing.JLabel;
import java.awt.event.MouseEvent;
import java.util.Timer;
import java.util.TimerTask;

@Setter
public class DBNInfoLabel extends JLabel {
    private static final Timer timer = new Timer(true);
    private TimerTask timerTask;
    private TextContent content;

    public DBNInfoLabel() {
        super("", AllIcons.General.Note, JLabel.LEFT);
        setCursor(Cursors.handCursor());

        Mouse.onMousePress(this, MouseEvent.BUTTON1, e -> showTooltip());
        Mouse.onMouseEntered(this, e -> scheduleShowTooltip());
        Mouse.onMouseExited(this, e -> hideTooltip());

    }

    private DBNTooltip createTooltip() {
        DBNInfoForm infoForm = new DBNInfoForm(null, content);
        return new DBNTooltip(this, getLocation(), infoForm.getComponent());
    }


    private void scheduleShowTooltip() {
        timerTask = createTimerTask(this);
        timer.schedule(timerTask, 500);
    }

    private TimerTask createTimerTask(JComponent component) {
        return new TimerTask() {
            @Override
            public void run() {
                Dispatch.run(component, () -> showTooltip());
            }
        };
    }

    private void showTooltip() {
        cancelSchedule();
        DBNTooltip tooltip = createTooltip();

        IdeTooltipManager tooltipManager = IdeTooltipManager.getInstance();
        tooltipManager.show(tooltip, true);
    }

    private void hideTooltip() {
        cancelSchedule();
        IdeTooltipManager tooltipManager = IdeTooltipManager.getInstance();
        tooltipManager.hideCurrentNow(true);
    }

    private void cancelSchedule() {
        if (timerTask != null) {
            timerTask.cancel();
        }
        timer.purge();
    }
}

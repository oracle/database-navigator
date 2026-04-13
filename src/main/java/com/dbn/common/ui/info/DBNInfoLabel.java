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

import com.dbn.common.icon.Icons;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.text.TextContent;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.util.Cursors;
import com.dbn.common.ui.util.Mouse;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Setter;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.Timer;
import java.util.TimerTask;

@Setter
public class DBNInfoLabel extends JLabel {
    private static final Timer timer = new Timer(true);
    private TimerTask timerTask;
    private TextContent content;

    private static WeakRef<Balloon> currentBalloon;
    private static boolean currentBalloonSticky;

    public DBNInfoLabel() {
        super("", Icons.ACTION_INFO, JLabel.LEFT);
        setCursor(Cursors.handCursor());

        Mouse.onMousePress(this, MouseEvent.BUTTON1, e -> showTooltip(true));
        Mouse.onMouseEntered(this, e -> scheduleShowTooltip());
        Mouse.onMouseExited(this, e -> hideTooltip());
    }

    private void scheduleShowTooltip() {
        timerTask = createTimerTask(this);
        timer.schedule(timerTask, 500);
    }

    private TimerTask createTimerTask(JComponent component) {
        return new TimerTask() {
            @Override
            public void run() {
                Dispatch.run(component, () -> showTooltip(false));
            }
        };
    }

    private void showTooltip(boolean sticky) {
        cancelSchedule();
        hideCurrentPopup(true);

        JComponent popupComponent = createPopupComponent(content);
        BalloonBuilder builder = JBPopupFactory.getInstance().createBalloonBuilder(popupComponent);
        builder.setAnimationCycle(0);
        builder.setCornerRadius(JBUI.scale(8));
        builder.setFillColor(UIUtil.getToolTipBackground());
        builder.setBorderColor(JBUI.CurrentTheme.Tooltip.borderColor());

        RelativePoint popupLocation = JBPopupFactory.getInstance().guessBestPopupLocation(this);
        Point point = popupLocation.getPoint();
        popupLocation = new RelativePoint(popupLocation.getComponent(), new Point(point.x + 12, point.y));

        Balloon balloon = builder.createBalloon();
        currentBalloon = WeakRef.of(balloon);
        currentBalloonSticky = sticky;
        balloon.show(popupLocation, Balloon.Position.atRight);
    }

    private static JComponent createPopupComponent(TextContent content) {
        DBNInfoForm infoForm = new DBNInfoForm(null, content);
        JComponent infoComponent = infoForm.getComponent();

        JPanel component = new JPanel(new BorderLayout());
        component.setOpaque(false);
        component.setBorder(JBUI.Borders.empty(4));
        component.add(infoComponent);
        return component;
    }

    private void hideTooltip() {
        cancelSchedule();
        hideCurrentPopup(false);
    }

    private void cancelSchedule() {
        if (timerTask != null) {
            timerTask.cancel();
        }
        timer.purge();
    }

    private static void hideCurrentPopup(boolean force) {
        if (currentBalloonSticky && !force) return;
        Balloon current = WeakRef.get(currentBalloon);
        if (current != null) {
            current.hide();
        }
    }
}

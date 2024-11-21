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

package com.dbn.common.load;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.util.TimeUtil;
import com.intellij.icons.AllIcons;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;
import java.util.Timer;
import java.util.TimerTask;

public class LoadInProgressIcon implements Icon{
    public static final Icon INSTANCE = new LoadInProgressIcon();

    public static int ROLL_INTERVAL = 80;

    private static final Icon[] ICONS;
    static {
        ICONS = new Icon[8];
        for (int i = 0; i < ICONS.length; i++) {
            switch (i) {
                case 0: ICONS[i] = AllIcons.Process.Step_1; break;
                case 1: ICONS[i] = AllIcons.Process.Step_2; break;
                case 2: ICONS[i] = AllIcons.Process.Step_3; break;
                case 3: ICONS[i] = AllIcons.Process.Step_4; break;
                case 4: ICONS[i] = AllIcons.Process.Step_5; break;
                case 5: ICONS[i] = AllIcons.Process.Step_6; break;
                case 6: ICONS[i] = AllIcons.Process.Step_7; break;
                case 7: ICONS[i] = AllIcons.Process.Step_8; break;
            }
        }
    }

    private static int iconIndex;
    private static long lastAccessTimestamp = System.currentTimeMillis();

    private static volatile Timer ICON_ROLLER;
    private static class IconRollerTimerTask extends TimerTask {
        @Override
        public void run() {
            if (iconIndex == ICONS.length - 1) {
                iconIndex = 0;
            } else {
                iconIndex++;
            }

            if (ICON_ROLLER != null && TimeUtil.isOlderThan(lastAccessTimestamp, TimeUtil.Millis.TEN_SECONDS)) {
                synchronized (IconRollerTimerTask.class) {
                    Timer cachedIconRoller = ICON_ROLLER;
                    ICON_ROLLER = null;
                    Disposer.dispose(cachedIconRoller);
                }
            }
        }
    };

    private static void startRoller() {
        if (ICON_ROLLER == null) {
            synchronized (IconRollerTimerTask.class) {
                if (ICON_ROLLER == null) {
                    ICON_ROLLER = new Timer("DBN - Load in Progress (icon roller)");
                    ICON_ROLLER.schedule(new IconRollerTimerTask(), ROLL_INTERVAL, ROLL_INTERVAL);
                }
            }
        }
    }

    private static Icon getIcon() {
        startRoller();
        lastAccessTimestamp = System.currentTimeMillis();
        return ICONS[iconIndex];
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        getIcon().paintIcon(c, g, x, y);
    }

    @Override
    public int getIconWidth() {
        return ICONS[0].getIconWidth();
    }

    @Override
    public int getIconHeight() {
        return ICONS[0].getIconHeight();
    }
}

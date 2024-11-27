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

package com.dbn.common.ui;

import com.intellij.ide.IdeTooltip;
import com.intellij.ide.TooltipEvent;
import lombok.Setter;

import javax.swing.JComponent;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

@Setter
public class DBNTooltip extends IdeTooltip {
    private Boolean dismissOnTimeout;
    private Integer dismissDelay;

    public DBNTooltip(Component component, Point point, JComponent tipComponent, Object... identity) {
        super(component, point, tipComponent, identity);
    }

    @Override
    public boolean canBeDismissedOnTimeout() {
        return dismissOnTimeout == null ? super.canBeDismissedOnTimeout() : dismissOnTimeout;
    }

    @Override
    public int getDismissDelay() {
        return dismissDelay == null ? super.getDismissDelay() : dismissDelay;
    }

    @Override
    protected boolean canAutohideOn(TooltipEvent event) {
        InputEvent inputEvent = event.getInputEvent();
        if (inputEvent == null) return false;
        if (inputEvent instanceof MouseEvent) {
            MouseEvent mouseEvent = (MouseEvent) inputEvent;
            return mouseEvent.getID() != MouseEvent.MOUSE_MOVED;
        }

        return true;
    }
}

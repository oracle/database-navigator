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

package com.dbn.data.grid.addon;

import com.dbn.common.addon.ComponentAddonBase;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.util.Mouse;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.table.JTableHeader;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.util.Timer;
import java.util.TimerTask;

import static com.dbn.common.ui.util.ClientProperty.COLUMN_DRAG_SCROLL_ADDON;

public class ColumnDragScrollAddon extends ComponentAddonBase<JTable> {
    private double scrollDistance;
    private Timer scrollTimer;

    private ColumnDragScrollAddon(JTable table) {
        super(table);

        JTableHeader tableHeader = table.getTableHeader();

        tableHeader.addMouseMotionListener(Mouse.listener().onDrag(e -> {
            JScrollPane scrollPane = getScrollPane();
            if (scrollPane == null) return;

            JScrollBar horizontalScrollBar = scrollPane.getHorizontalScrollBar();
            if (!horizontalScrollBar.isVisible()) return;

            if (tableHeader.getDraggedColumn() == null) return;

            calculateScrollDistance();
            if (scrollDistance != 0 && scrollTimer == null) {
                scrollTimer = new Timer();
                scrollTimer.schedule(new ScrollTask(), 100, 100);
            }
        }));

        tableHeader.addMouseListener(Mouse.listener().onRelease(e -> {
            if (scrollTimer == null) return;

            Disposer.dispose(scrollTimer);
            scrollTimer = null;
        }));
    }


    public JTable getTable() {
        return getComponent();
    }

    private void calculateScrollDistance() {
        JViewport viewport = getViewport();
        if (viewport == null) return;

        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        if (pointerInfo == null) return;

        double mouseLocation = pointerInfo.getLocation().getX();
        double viewportLocation = viewport.getLocationOnScreen().getX();

        Point viewPosition = viewport.getViewPosition();
        double contentLocation = viewport.getView().getLocationOnScreen().getX();

        if (contentLocation < viewportLocation && mouseLocation < viewportLocation + 20) {
            scrollDistance = - Math.min(viewPosition.x, (viewportLocation - mouseLocation)) * 2;
        } else {
            int viewportWidth = viewport.getWidth();
            int contentWidth = viewport.getView().getWidth();

            if (contentLocation + contentWidth > viewportLocation + viewportWidth && mouseLocation > viewportLocation + viewportWidth - 20) {
                scrollDistance = (mouseLocation - viewportLocation - viewportWidth) * 2;
            } else {
                scrollDistance = 0;
            }
        }
    }


    private class ScrollTask extends TimerTask {
        @Override
        public void run() {
            if (scrollDistance == 0) return;

            JViewport viewport = getViewport();
            if (viewport == null) return;

            Dispatch.run(viewport, () -> {
                Point viewPosition = viewport.getViewPosition();
                viewport.setViewPosition(new Point((int) (viewPosition.x + scrollDistance), viewPosition.y));
                calculateScrollDistance();
            });
        }
    }

    @Nullable
    private JViewport getViewport() {
        return UIUtil.getParentOfType(JViewport.class, getTable());
    }

    @Nullable
    private JScrollPane getScrollPane() {
        return UIUtil.getParentOfType(JScrollPane.class, getTable());
    }

    public static void installTo(JTable table) {
        COLUMN_DRAG_SCROLL_ADDON.get(table, () -> new ColumnDragScrollAddon(table));
    }
}
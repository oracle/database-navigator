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
import com.dbn.common.ui.util.Fonts;
import com.dbn.common.util.Alarms;
import com.dbn.common.util.MathResult;
import com.dbn.data.grid.ui.table.basic.BasicTable;
import com.dbn.data.grid.ui.table.basic.MathPanel;
import com.dbn.data.model.DataModelCell;
import com.dbn.data.model.basic.BasicDataModelCell;
import com.intellij.ide.IdeTooltip;
import com.intellij.ide.IdeTooltipManager;
import com.intellij.util.Alarm;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.dbn.common.ui.util.ClientProperty.SELECTION_MATH_ADDON;

@Getter
public class SelectionMathAddon extends ComponentAddonBase<BasicTable> {
    private MathResult mathResult;

    public SelectionMathAddon(BasicTable table) {
        super(table);

        initSelectionListener();
        initMouseMotionListener();
    }

    public BasicTable getTable() {
        return getComponent();
    }

    private void initMouseMotionListener() {
        getTable().addMouseMotionListener(new MouseMotionAdapter() {
            private final Alarm runner = Alarms.createAlarm(getTable());
            @Override
            public void mouseMoved(MouseEvent e) {
                if (mathResult != null && isCellSelected(e.getPoint())) {
                    Alarms.alarmRequest(runner, 100, true, () -> showSelectionTooltip());
                }
            }
        });
    }

    private void initSelectionListener() {
        getTable().getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            mathResult = null;

            BigDecimal total = BigDecimal.ZERO;
            BigDecimal count = BigDecimal.ZERO;
            BasicTable table = getTable();
            int rows = table.getSelectedRowCount();
            int columns = table.getSelectedColumnCount();
            if (columns != 1 || rows <= 1 || rows >= 200) return;

            int selectedColumn = table.getSelectedColumn();
            int[] selectedRows = table.getSelectedRows();
            for (int selectedRow : selectedRows) {
                Object value = table.getValueAt(selectedRow, selectedColumn);
                if (value instanceof BasicDataModelCell) {
                    BasicDataModelCell<?, ?> cell = (BasicDataModelCell<?, ?>) value;
                    Object userValue = cell.getUserValue();
                    if (userValue == null || userValue instanceof Number) {
                        if (userValue != null) {
                            count = count.add(BigDecimal.ONE);
                            Number number = (Number) userValue;
                            total = total.add(new BigDecimal(number.toString()));
                        }

                    } else {
                        return;
                    }
                } else {
                    return;
                }
            }
            if (count.compareTo(BigDecimal.ZERO) <= 0) return;

            BigDecimal average = total.divide(count, 9, RoundingMode.HALF_UP);
            average = average.stripTrailingZeros();
            mathResult = new MathResult(total, count, average);
            showSelectionTooltip();
        });
    }

    private void showSelectionTooltip() {
        MathResult mathResult = this.mathResult;
        if (mathResult == null) return;

        BasicTable table = getTable();
        Point mousePosition = table.getMousePosition();
        if (mousePosition == null) return;

        if (!isCellSelected(mousePosition)) return;

        MathPanel mathPanel = new MathPanel(table.getProject(), mathResult);
        IdeTooltip tooltip = new IdeTooltip(table, mousePosition, mathPanel.getComponent());
        tooltip.setFont(Fonts.regular(2));
        IdeTooltipManager.getInstance().show(tooltip, true);
    }

    private boolean isCellSelected(Point point) {
        BasicTable table = getTable();
        DataModelCell<?, ?> cell = table.getCellAtLocation(point);
        if (cell == null) return false;

        int rowIndex = cell.getRow().getIndex();
        int columnIndex = cell.getColumnInfo().getIndex();
        return table.isCellSelected(rowIndex, columnIndex);
    }

    public static void installTo(BasicTable table) {
        SELECTION_MATH_ADDON.get(table, () -> new SelectionMathAddon(table));
    }

    @Nullable
    public static SelectionMathAddon of(BasicTable table) {
        return SELECTION_MATH_ADDON.get(table);
    }

    public static void uninstallFrom(BasicTable table) {
        SELECTION_MATH_ADDON.set(table, null);
    }



}

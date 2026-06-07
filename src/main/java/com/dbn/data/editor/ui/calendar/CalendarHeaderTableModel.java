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

package com.dbn.data.editor.ui.calendar;

import org.jetbrains.annotations.Nls;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import static com.dbn.nls.NlsResources.txt;

/******************************************************
 *                  TableModels                       *
 ******************************************************/
class CalendarHeaderTableModel implements TableModel {
    @Override
    public int getRowCount() {
        return 1;
    }

    @Override
    public int getColumnCount() {
        return 7;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return null;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return getWeekdayShortName(columnIndex);
    }

    @Nls
    private static String getWeekdayShortName(int columnIndex) {
        return switch (columnIndex) {
            case 0 -> txt("app.dataEditor.const.CalendarWeekdayShort_SUNDAY");
            case 1 -> txt("app.dataEditor.const.CalendarWeekdayShort_MONDAY");
            case 2 -> txt("app.dataEditor.const.CalendarWeekdayShort_TUESDAY");
            case 3 -> txt("app.dataEditor.const.CalendarWeekdayShort_WEDNESDAY");
            case 4 -> txt("app.dataEditor.const.CalendarWeekdayShort_THURSDAY");
            case 5 -> txt("app.dataEditor.const.CalendarWeekdayShort_FRIDAY");
            case 6 -> txt("app.dataEditor.const.CalendarWeekdayShort_SATURDAY");
            default -> null;
        };
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
    }
}

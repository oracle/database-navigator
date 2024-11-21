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

import com.dbn.common.locale.Formatter;
import com.dbn.common.ui.util.Listeners;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

class CalendarTableModel implements TableModel {
    private final Listeners<TableModelListener> listeners = Listeners.create();
    private final Calendar inputDate = new GregorianCalendar();
    private final Calendar activeMonth = new GregorianCalendar();
    private final Calendar previousMonth = new GregorianCalendar();

    CalendarTableModel(Date date) {
        if (date != null) {
            inputDate.setTime(date);
            activeMonth.setTime(date);
            previousMonth.setTime(date);
        }
        activeMonth.set(Calendar.DAY_OF_MONTH, 1);
        previousMonth.set(Calendar.DAY_OF_MONTH, 1);
        previousMonth.add(Calendar.MONTH, -1);
    }

    @Override
    public int getRowCount() {
        return 6;
    }

    @Override
    public int getColumnCount() {
        return 7;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return null;
    }

    String getCurrentMonthName() {
        return getMonthName(activeMonth.get(Calendar.MONTH));
    }

    String getCurrentYear() {
        return String.valueOf(activeMonth.get(Calendar.YEAR));
    }

    String getMonthName(int month) {
        switch (month) {
            case Calendar.JANUARY: return "January";
            case Calendar.FEBRUARY: return "February";
            case Calendar.MARCH: return "March";
            case Calendar.APRIL: return "April";
            case Calendar.MAY: return "May";
            case Calendar.JUNE: return "June";
            case Calendar.JULY: return "July";
            case Calendar.AUGUST: return "August";
            case Calendar.SEPTEMBER: return "September";
            case Calendar.OCTOBER: return "October";
            case Calendar.NOVEMBER: return "November";
            case Calendar.DECEMBER: return "December";
        }
        return null;
    }

    boolean isFromActiveMonth(int rowIndex, int columnIndex) {
        return !isFromPreviousMonth(rowIndex, columnIndex) &&
                !isFromNextMonth(rowIndex, columnIndex);
    }

    boolean isFromPreviousMonth(int rowIndex, int columnIndex) {
        int value = rowIndex * 7 + columnIndex + 2 - activeMonth.get(Calendar.DAY_OF_WEEK);
        return value < 1;
    }

    boolean isFromNextMonth(int rowIndex, int columnIndex) {
        int value = rowIndex * 7 + columnIndex + 2 - activeMonth.get(Calendar.DAY_OF_WEEK);
        return value > activeMonth.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    boolean isInputDate(int rowIndex, int columnIndex) {
        int value = rowIndex * 7 + columnIndex + 2 - activeMonth.get(Calendar.DAY_OF_WEEK);
        return inputDate.get(Calendar.YEAR) == activeMonth.get(Calendar.YEAR) &&
                inputDate.get(Calendar.MONTH) == activeMonth.get(Calendar.MONTH) &&
                value >= 1 && value <= activeMonth.getActualMaximum(Calendar.DAY_OF_MONTH) &&
                value == inputDate.get(Calendar.DAY_OF_MONTH);
    }

    int getInputDateColumnIndex() {
        return inputDate.get(Calendar.DAY_OF_WEEK) - 1;
    }

    int getInputDateRowIndex() {
        return (activeMonth.get(Calendar.DAY_OF_WEEK) + inputDate.get(Calendar.DAY_OF_MONTH) - 2) / 7;
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
        int value = rowIndex * 7 + columnIndex + 2 - activeMonth.get(Calendar.DAY_OF_WEEK);
        if (value < 1) {
            value = previousMonth.getActualMaximum(Calendar.DAY_OF_MONTH) + value;
        } else if (value > activeMonth.getActualMaximum(Calendar.DAY_OF_MONTH)) {
            value = value - activeMonth.getActualMaximum(Calendar.DAY_OF_MONTH);
        }
        return value;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }

    void rollMonth(int amount) {
        activeMonth.add(Calendar.MONTH, amount);
        previousMonth.add(Calendar.MONTH, amount);
        notifyListeners();
    }

    void rollYear(int amount) {
        activeMonth.add(Calendar.YEAR, amount);
        previousMonth.add(Calendar.YEAR, amount);
        notifyListeners();
    }

    private void notifyListeners() {
        TableModelEvent event = new TableModelEvent(this, 0, 5);
        listeners.notify(l -> l.tableChanged(event));
    }

    Timestamp getTimestamp(int rowIndex, int columnIndex, String timeText, Formatter timeFormatter) {
        Calendar calendar = (Calendar) activeMonth.clone();
        if (isFromPreviousMonth(rowIndex, columnIndex)) {
            calendar.add(Calendar.MONTH, -1);
        } else if (isFromNextMonth(rowIndex, columnIndex)) {
            calendar.add(Calendar.MONTH, 1);
        }
        int day = (Integer) getValueAt(rowIndex, columnIndex);
        calendar.set(Calendar.DAY_OF_MONTH, day);

        try {
            Date time = timeFormatter.parseTime(timeText);
            Calendar timeCalendar = new GregorianCalendar();
            timeCalendar.setTime(time);
            calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
            calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
            calendar.set(Calendar.SECOND, timeCalendar.get(Calendar.SECOND));
            calendar.set(Calendar.MILLISECOND, timeCalendar.get(Calendar.MILLISECOND));
        } catch (ParseException e) {
            conditionallyLog(e);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
        }

        return new Timestamp(calendar.getTime().getTime());
    }
}

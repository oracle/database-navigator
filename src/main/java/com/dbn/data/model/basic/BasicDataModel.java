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

package com.dbn.data.model.basic;

import com.dbn.common.dispose.Disposed;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.filter.Filter;
import com.dbn.common.latent.Latent;
import com.dbn.common.list.FilteredList;
import com.dbn.common.locale.Formatter;
import com.dbn.common.locale.options.RegionalSettingsListener;
import com.dbn.common.project.ProjectRef;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.util.Listeners;
import com.dbn.data.find.DataSearchResult;
import com.dbn.data.model.ColumnInfo;
import com.dbn.data.model.DataModel;
import com.dbn.data.model.DataModelCell;
import com.dbn.data.model.DataModelHeader;
import com.dbn.data.model.DataModelListener;
import com.dbn.data.model.DataModelRow;
import com.dbn.data.model.DataModelState;
import com.dbn.editor.data.model.RecordStatus;
import com.dbn.editor.data.model.RecordStatusHolder;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.util.ArrayList;
import java.util.List;

public class BasicDataModel<
        R extends DataModelRow<? extends BasicDataModel<R, C>, C>,
        C extends DataModelCell<R, ? extends BasicDataModel<R, C>>>
        extends RecordStatusHolder
        implements DataModel<R,C> {

    private final ProjectRef project;
    private DataModelHeader<? extends ColumnInfo> header;
    private DataModelState state;
    private List<R> rows = new ArrayList<>();
    private Filter<R> filter;
    private boolean environmentReadonly;
    private Latent<Formatter> formatter;

    private final Listeners<TableModelListener> tableModelListeners = Listeners.create(this);
    private final Listeners<DataModelListener> dataModelListeners = Listeners.create(this);
    private final Latent<BasicDataGutterModel> listModel = Latent.basic(() -> new BasicDataGutterModel(BasicDataModel.this));
    private final Latent<DataSearchResult> searchResult = Latent.basic(() -> new DataSearchResult());


    private final RegionalSettingsListener regionalSettingsListener = new RegionalSettingsListener() {
        @Override
        public void settingsChanged() {
            formatter = Latent.thread(() -> {
                Project project = getProject();
                Formatter formatter = Formatter.getInstance(project);
                return formatter.clone();
            });
        }
    };


    public BasicDataModel(Project project) {
        this.project = ProjectRef.of(project);
        formatter = Latent.thread(() -> Formatter.getInstance(project).clone());
        ProjectEvents.subscribe(project, this, RegionalSettingsListener.TOPIC, regionalSettingsListener);
    }

    @Override
    protected RecordStatus[] properties() {
        return RecordStatus.VALUES;
    }

    public boolean isEnvironmentReadonly() {
        return environmentReadonly;
    }

    public void setEnvironmentReadonly(boolean environmentReadonly) {
        this.environmentReadonly = environmentReadonly;
    }

    @Override
    public ListModel getListModel() {
        return listModel.get();
    }

    @NotNull
    public Formatter getFormatter() {
        return formatter.get();
    }

    @Override
    public boolean isReadonly() {
        return true;
    }

    @Override
    @NotNull
    public Project getProject() {
        return project.ensure();
    }

    public void setHeader(@NotNull DataModelHeader<? extends ColumnInfo> header) {
        this.header = Disposer.replace(this.header, header);
        Disposer.register(this, this.header);
    }

    @Override
    @NotNull
    public DataModelHeader<? extends ColumnInfo> getHeader() {
        return Failsafe.nn(header);
    }

    @Override
    @NotNull
    public DataModelState getState() {
        if (state == null) {
            state = createState();
        }
        return state;
    }

    @Override
    public void setState(DataModelState state) {
        this.state = state;
    }

    @Override
    public void setFilter(Filter<R> filter) {
        List<R> rows = getRows();
        if (filter == null) {
            this.rows = FilteredList.unwrap(rows);
        }
        else {
            FilteredList<R> filteredList;
            if (rows instanceof FilteredList) {
                filteredList = (FilteredList<R>) rows;
                filteredList.setFilter(filter);
            } else {
                filteredList = FilteredList.stateful(filter, rows);
                this.rows = filteredList;
            }
        }
        this.filter = filter;
    }

    @Nullable
    @Override
    public Filter<R> getFilter() {
        return filter;
    }

    protected DataModelState createState() {
        return new DataModelState();
    }

    @NotNull
    @Override
    public List<R> getRows() {
        return Failsafe.nn(rows);
    }

    public void setRows(List<R> rows) {
        if (filter != null) {
            this.rows = FilteredList.stateful(filter, rows);
        } else {
            this.rows = rows;
        }

        getState().setRowCount(getRowCount());
    }

    public void addRow(R row) {
        getRows().add(row);
        getState().setRowCount(getRowCount());
    }

    protected void addRowAtIndex(int index, R row) {
        List<R> rows = getRows();
        int size = rows.size();
        if (index > size) index = size;

        rows.add(index, row);
        updateRowIndexes(index);
        getState().setRowCount(getRowCount());
    }

    protected void removeRowAtIndex(int index) {
        DataModelRow row = getRows().remove(index);
        updateRowIndexes(index);
        getState().setRowCount(getRowCount());

        Disposer.dispose(row);
    }

    @Nullable
    @Override
    public R getRowAtIndex(int index) {
        // model may be reloading when this is called, hence
        // IndexOutOfBoundsException is thrown if the range is not checked
        List<R> rows = getRows();
        return index > -1 && rows.size() > index ? rows.get(index) : null;
    }

    @Nullable
    public C getCellAt(int rowIndex, int columnIndex) {
        checkRowBounds(rowIndex);
        checkColumnBounds(columnIndex);

        R row = getRowAtIndex(rowIndex);
        return row == null ? null : row.getCellAtIndex(columnIndex);
    }

    @Override
    public ColumnInfo getColumnInfo(int columnIndex) {
        checkColumnBounds(columnIndex);

        return getHeader().getColumnInfo(columnIndex);
    }

    @Override
    public int indexOfRow(R row) {
        return getRows().indexOf(row);
    }

    protected void updateRowIndexes(int startIndex) {
        updateRowIndexes(getRows(), startIndex);
    }

    protected void updateRowIndexes(List<R> rows, int startIndex) {
        for (int i=startIndex; i<rows.size();i++) {
            rows.get(i).setIndex(i);
        }
    }

    /*********************************************************
     *                 Listener notifiers                    *
     *********************************************************/
    public void notifyCellUpdated(int rowIndex, int columnIndex) {
        TableModelEvent tableModelEvent = new TableModelEvent(this, rowIndex, rowIndex, columnIndex);
        ListDataEvent listDataEvent = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, rowIndex, rowIndex);
        notifyListeners(listDataEvent, tableModelEvent);
    }

    protected void notifyRowUpdated(int rowIndex) {
        TableModelEvent tableModelEvent = new TableModelEvent(this, rowIndex, rowIndex);
        ListDataEvent listDataEvent = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, rowIndex, rowIndex);
        notifyListeners(listDataEvent, tableModelEvent);
    }

    protected void notifyRowsDeleted(int fromRowIndex, int toRowIndex) {
        TableModelEvent tableModelEvent = new TableModelEvent(this, fromRowIndex, toRowIndex, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE);
        ListDataEvent listDataEvent = new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, fromRowIndex, toRowIndex);
        notifyListeners(listDataEvent, tableModelEvent);
    }

    protected void notifyRowsUpdated(int fromRowIndex, int toRowIndex) {
        TableModelEvent tableModelEvent = new TableModelEvent(this, fromRowIndex, toRowIndex, TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE);
        ListDataEvent listDataEvent = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, fromRowIndex, toRowIndex);
        notifyListeners(listDataEvent, tableModelEvent);
    }

    protected void notifyRowsInserted(int fromRowIndex, int toRowIndex) {
        TableModelEvent tableModelEvent = new TableModelEvent(this, fromRowIndex, toRowIndex, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT);
        ListDataEvent listDataEvent = new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, fromRowIndex, toRowIndex);
        notifyListeners(listDataEvent, tableModelEvent);
    }

    protected void notifyListeners(@Nullable ListDataEvent listDataEvent, @Nullable TableModelEvent modelEvent) {
        Dispatch.run(() -> {
            if (listDataEvent != null) {
                BasicDataGutterModel gutterModel = listModel.value();
                if (gutterModel != null) {
                    gutterModel.notifyListeners(listDataEvent);
                }
            }

            if (modelEvent != null) {
                tableModelListeners.notify(l -> l.tableChanged(modelEvent));
                dataModelListeners.notify(l -> l.modelChanged());
            }
        });
    }

    /*********************************************************
     *                     DataModel                        *
     *********************************************************/
    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return header == null ? 0 : header.getColumnCount();
    }

    @Override
    public String getColumnName(int columnIndex) {
        return getHeader().getColumnName(columnIndex);
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
        // model may be reloading when this is called, hence
        return getCellAt(rowIndex, columnIndex);
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {}

    @Override
    public void addTableModelListener(TableModelListener listener) {
        tableModelListeners.add(listener);
    }

    @Override
    public void removeTableModelListener(TableModelListener listener) {
        tableModelListeners.remove(listener);
    }

    @Override
    public void addDataModelListener(DataModelListener listener) {
        dataModelListeners.add(listener);
    }

    @Override
    public void removeDataModelListener(DataModelListener listener) {
        dataModelListeners.remove(listener);
    }

    @Override
    public DataSearchResult getSearchResult() {
        return searchResult.get();
    }
    
    @Override
    public boolean hasSearchResult() {
        return searchResult.loaded() && !searchResult.get().isEmpty();
    }

    @Override
    public int getColumnIndex(String columnName) {
        return getHeader().getColumnIndex(columnName);
    }

    /********************************************************
     *                    Disposable                        *
     *******************************************************  */
    @Override
    public void disposeInner() {
        rows = Disposer.replace(rows, Disposed.list());
        nullify();
    }
}

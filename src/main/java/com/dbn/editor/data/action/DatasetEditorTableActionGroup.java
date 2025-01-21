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

package com.dbn.editor.data.action;

import com.dbn.common.action.BasicAction;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.icon.Icons;
import com.dbn.common.ref.WeakRef;
import com.dbn.data.model.ColumnInfo;
import com.dbn.data.sorting.SortDirection;
import com.dbn.data.type.DBDataType;
import com.dbn.data.type.GenericDataType;
import com.dbn.editor.data.DatasetEditor;
import com.dbn.editor.data.filter.ConditionJoinType;
import com.dbn.editor.data.filter.ConditionOperator;
import com.dbn.editor.data.filter.DatasetBasicFilter;
import com.dbn.editor.data.filter.DatasetFilter;
import com.dbn.editor.data.filter.DatasetFilterInput;
import com.dbn.editor.data.filter.DatasetFilterManager;
import com.dbn.editor.data.model.DatasetEditorModelCell;
import com.dbn.editor.data.ui.table.DatasetEditorTable;
import com.dbn.object.DBColumn;
import com.dbn.object.DBDataset;
import com.dbn.object.action.NavigateToObjectAction;
import com.dbn.object.action.ObjectNavigationListActionGroup;
import com.dbn.object.common.list.DBObjectNavigationList;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.ide.CopyPasteManager;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

@Slf4j
public class DatasetEditorTableActionGroup extends DefaultActionGroup {
    private final ColumnInfo columnInfo;
    private final Object columnValue;
    boolean isHeaderAction;
    private final WeakRef<DatasetEditor> datasetEditor;
    public DatasetEditorTableActionGroup(DatasetEditor datasetEditor, @Nullable DatasetEditorModelCell cell, ColumnInfo columnInfo) {
        this.datasetEditor = WeakRef.of(datasetEditor);
        this.columnInfo = columnInfo;
        DatasetEditorTable table = datasetEditor.getEditorTable();

        isHeaderAction = cell == null;
        columnValue = cell == null ? null : cell.getUserValue();

        HideColumnAction hideColumnAction = new HideColumnAction();
        add(hideColumnAction);
        addSeparator();
        if (cell != null && cell.isModified() && !cell.isLobValue()) {
            DataRevertAction revertChangesAction = new DataRevertAction(cell);
            add(revertChangesAction);
        }

        DefaultActionGroup filterActionGroup = new DefaultActionGroup(txt("app.dataEditor.action.Filter"), true);
        filterActionGroup.getTemplatePresentation().setIcon(Icons.DATASET_FILTER_NEW);
        //filterActionGroup.getTemplatePresentation().setIcon(Icons.DATASET_FILTER);
        filterActionGroup.add(new CreateFilterAction(false));
        filterActionGroup.addSeparator();
        if (columnValue != null ) filterActionGroup.add(new CreateFilterAction(true));
        DBDataType dataType = columnInfo.getDataType();
        String text = getClipboardContent((int) dataType.getLength());
        if (text != null) {
            filterActionGroup.add(new CreateClipboardFilterAction(text, false));
            if (dataType.getGenericDataType() == GenericDataType.LITERAL) {
                filterActionGroup.add(new CreateClipboardFilterAction(text, true));
            }
        }

        // show the instructions additional condition action in case the filter is basic,
        // the join is AND, and the column is not already present
        DatasetFilterManager filterManager = DatasetFilterManager.getInstance(table.getDataset().getProject());
        DatasetFilter activeFilter = filterManager.getActiveFilter(table.getDataset());
        if (activeFilter instanceof DatasetBasicFilter) {
            DatasetBasicFilter basicFilter = (DatasetBasicFilter) activeFilter;
            if (basicFilter.getJoinType() == ConditionJoinType.AND &&
                    !basicFilter.containsConditionForColumn(columnInfo.getName())) {
                filterActionGroup.addSeparator();
                filterActionGroup.add(new CreateAdditionalConditionAction());
            }
        }
        add(filterActionGroup);

        if (columnInfo.isSortable()) {
            DefaultActionGroup sortingActionGroup = new DefaultActionGroup(txt("app.dataEditor.action.Sort"), true);
            //sortingActionGroup.getTemplatePresentation().setIcon(Icons.COMMON_SORTING);
            sortingActionGroup.add(new SortAscendingAction());
            sortingActionGroup.add(new SortDescendingAction());
            add(sortingActionGroup);
/*
            add(new SortAscendingAction());
            add(new SortDescendingAction());
*/
        }

        DBDataset dataset = table.getDataset();
        DBColumn column = Failsafe.nn(dataset.getColumn(columnInfo.getName()));
        if (columnValue != null) {
            if (column.isForeignKey()) {
                DatasetFilterInput filterInput = table.getModel().resolveForeignKeyRecord(cell);
                if (filterInput != null) {
                    add(new ReferencedRecordOpenAction(filterInput));
                }
            }
            if (column.isPrimaryKey()) {
                ReferencingRecordsOpenAction action = new ReferencingRecordsOpenAction(column, columnValue);
                action.setPopupLocation(table.getColumnHeaderLocation(column));
                add(action);
            }
        }

        addSeparator();

        DefaultActionGroup columnPropertiesActionGroup = new DefaultActionGroup(txt("app.dataEditor.action.ColumnInfo"), true);
        columnPropertiesActionGroup.add(new NavigateToObjectAction(column));
        for (DBObjectNavigationList navigationList : column.getNavigationLists()) {
            if (!navigationList.isLazy()) {
                add(new ObjectNavigationListActionGroup(column, navigationList, true));
            }
        }
        add(columnPropertiesActionGroup);
        addSeparator();

        add(new DataExportAction());
    }

    private static String getClipboardContent(int maxLength) {
        try {
            CopyPasteManager copyPasteManager = CopyPasteManager.getInstance();
            Transferable transferable = copyPasteManager.getContents();;
            if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String text = (String) transferable.getTransferData(DataFlavor.stringFlavor);
                if (text == null) {
                    return null;
                } else {
                    text = text.trim();
                    if (text.length() == 0 || text.length() > maxLength) {
                        return null;
                    }
                    return text;
                }
            }

        } catch (Exception e) {
            conditionallyLog(e);
            log.error("Failed to load clipboard content", e);
        }
        return null;
    }

    @NotNull
    public DatasetEditor getDatasetEditor() {
        return datasetEditor.ensure();
    }

    private class HideColumnAction extends BasicAction {
        private HideColumnAction() {
            super(txt("app.dataEditor.action.HideColumn"));
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            DatasetEditorTable editorTable = getDatasetEditor().getEditorTable();
            int columnIndex = columnInfo.getIndex();
            editorTable.hideColumn(columnIndex);
        }
    }

    private class HideAuditColumnsAction extends BasicAction {
        private HideAuditColumnsAction() {
            super(txt("app.dataEditor.action.HideAuditColumns"));
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            DatasetEditorTable editorTable = getDatasetEditor().getEditorTable();
            editorTable.hideAuditColumns();
        }
    }

    private class ShowAuditColumnsAction extends BasicAction {
        private ShowAuditColumnsAction() {
            super(txt("app.dataEditor.action.ShowAuditColumns"));
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            DatasetEditorTable editorTable = getDatasetEditor().getEditorTable();
            editorTable.showAuditColumns();
        }
    }

    private class SortAscendingAction extends BasicAction {
        private SortAscendingAction() {
            super(txt("app.data.action.SortAscending"), null, Icons.ACTION_SORT_ASC);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            DatasetEditorTable editorTable = getDatasetEditor().getEditorTable();
            int modelColumnIndex = columnInfo.getIndex();
            int tableColumnIndex = editorTable.convertColumnIndexToView(modelColumnIndex);
            editorTable.sort(tableColumnIndex, SortDirection.ASCENDING, false);
        }
    }

    private class SortDescendingAction extends BasicAction {
        private SortDescendingAction() {
            super(txt("app.data.action.SortDescending"), null, Icons.ACTION_SORT_DESC);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            DatasetEditorTable editorTable = getDatasetEditor().getEditorTable();
            int modelColumnIndex = columnInfo.getIndex();
            int tableColumnIndex = editorTable.convertColumnIndexToView(modelColumnIndex);
            editorTable.sort(tableColumnIndex, SortDirection.DESCENDING, false);
        }
    }

    private class CreateFilterAction extends BasicAction {
        private final boolean filterByValue;
        private CreateFilterAction(boolean filterByValue) {
            super(filterByValue ?
                    txt("app.dataEditor.action.FilterByValue") :
                    txt("app.dataEditor.action.FilterByColumn"));
            this.filterByValue = filterByValue;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            DBDataset dataset = getDatasetEditor().getDataset();
            DatasetFilterManager datasetFilterManager = DatasetFilterManager.getInstance(dataset.getProject());
            Object value = filterByValue ? columnValue : null;
            datasetFilterManager.createBasicFilter(dataset, columnInfo.getName(), value, ConditionOperator.EQUAL, !filterByValue);
        }
    }

    private class CreateClipboardFilterAction extends BasicAction {
        private final String text;
        private final boolean like;
        private CreateClipboardFilterAction(String text, boolean like) {
            super(like ?
                    txt("app.dataEditor.action.FilterByClipboardValueLike") :
                    txt("app.dataEditor.action.FilterByClipboardValue"));
            this.text = text;
            this.like = like;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            DBDataset dataset = getDatasetEditor().getDataset();
            DatasetFilterManager datasetFilterManager = DatasetFilterManager.getInstance(dataset.getProject());
            String value = like ? '%' + text + '%' : text;
            ConditionOperator operator = like ? ConditionOperator.LIKE : ConditionOperator.EQUAL;
            datasetFilterManager.createBasicFilter(dataset, columnInfo.getName(), value, operator, false);
        }
    }

    private class CreateAdditionalConditionAction extends BasicAction {
        private CreateAdditionalConditionAction() {
            super(columnValue == null ?
                    txt("app.dataEditor.action.AddColumnToFilter") :
                    txt("app.dataEditor.action.AddValueToFilter"));
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            DBDataset dataset = getDatasetEditor().getDataset();
            DatasetFilterManager filterManager = DatasetFilterManager.getInstance(dataset.getProject());
            DatasetBasicFilter basicFilter = (DatasetBasicFilter) filterManager.getActiveFilter(dataset);
            filterManager.addConditionToFilter(basicFilter, dataset, columnInfo, columnValue, isHeaderAction);
        }
    }
}

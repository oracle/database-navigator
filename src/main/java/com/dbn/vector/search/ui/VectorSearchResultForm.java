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

package com.dbn.vector.search.ui;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.ui.util.Keyboard;
import com.dbn.connection.ConnectionHandler;
import com.dbn.data.find.SearchableDataComponent;
import com.dbn.data.grid.ui.table.basic.BasicTable;
import com.dbn.data.grid.ui.table.resultSet.ResultSetTable;
import com.dbn.data.model.resultSet.ResultSetDataModel;
import com.dbn.data.record.RecordViewInfo;
import com.dbn.vector.search.VectorSearchConsole;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.Shortcut;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import static com.dbn.data.sorting.SortDirection.ASCENDING;
import static com.dbn.nls.NlsResources.txt;

public class VectorSearchResultForm extends DBNFormBase implements SearchableDataComponent {
    private JPanel mainPanel;
    private JPanel searchPanel;
    private DBNScrollPane resultScrollPane;

    private @Getter ResultSetTable resultTable;

    public VectorSearchResultForm(VectorSearchForm parent) {
        super(parent);
        initResultTable();

        // no search action around the table (install as key listener to the table)
        installSearchKeyListener();
    }

    private void initResultTable() {
        VectorSearchConsole searchEditor = getSearchConsole();
        RecordViewInfo recordViewInfo = new RecordViewInfo(txt("app.vector.title.SearchResult"), null);
        ConnectionHandler connection = searchEditor.getConnection();
        ResultSetDataModel dataModel = new ResultSetDataModel<>(connection);
        resultTable = new ResultSetTable<>(this, dataModel, true, recordViewInfo);
        resultScrollPane.setViewportView(resultTable);
        resultTable.installValuePopupAddon();
        Disposer.register(this, resultTable);
    }

    public void setLoading(boolean loading) {
        resultTable.setLoading(loading);
    }

    public void setSearchResult(ResultSetDataModel dataModel) {
        if (dataModel == null) return;

        resultTable.setModel(dataModel);
        resultTable.sort(0, ASCENDING,  true);
    }

    private VectorSearchConsole getSearchConsole() {
        VectorSearchForm searchForm = ensureParentComponent();
        return searchForm.getSearchConsole();
    }

    private final KeyListener keyListener = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent keyEvent) {
            Shortcut[] shortcuts = Keyboard.getShortcuts(IdeActions.ACTION_FIND);
            if (!keyEvent.isConsumed() && Keyboard.match(shortcuts, keyEvent)) {
                keyEvent.consume();
                showSearchHeader();
            }
        }
    };

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    public @NotNull BasicTable<?> getTable() {
        return resultTable;
    }

    @Override
    public @NotNull JPanel getSearchPanel() {
        return searchPanel;
    }

}

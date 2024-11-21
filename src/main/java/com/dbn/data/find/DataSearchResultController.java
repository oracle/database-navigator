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

package com.dbn.data.find;

import com.dbn.common.exception.OutdatedContentException;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.PooledThread;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Strings;
import com.dbn.data.grid.ui.table.basic.BasicTable;
import com.dbn.data.model.DataModel;
import com.dbn.data.model.DataModelCell;
import com.dbn.data.model.DataModelRow;
import com.dbn.data.model.basic.BasicDataModel;
import com.intellij.find.FindManager;
import com.intellij.find.FindResult;
import org.jetbrains.annotations.NotNull;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class DataSearchResultController {
    private final WeakRef<SearchableDataComponent> searchableComponent;
    private final AtomicReference<PooledThread> searchHandle = new AtomicReference<>();

    DataSearchResultController(SearchableDataComponent searchableComponent) {
        this.searchableComponent = WeakRef.of(searchableComponent);
    }

    @NotNull
    public SearchableDataComponent getSearchableComponent() {
        return searchableComponent.ensure();
    }

    void moveCursor(DataSearchDirection direction) {
        BasicTable<? extends BasicDataModel> table = getSearchableComponent().getTable();
        DataModel dataModel = table.getModel();
        DataSearchResult searchResult = dataModel.getSearchResult();
        DataSearchResultScrollPolicy scrollPolicy = DataSearchResultScrollPolicy.HORIZONTAL;
        DataSearchResultMatch oldSelection = searchResult.getSelectedMatch();
        DataSearchResultMatch selection =
                direction == DataSearchDirection.DOWN ? searchResult.selectNext(scrollPolicy) :
                        direction == DataSearchDirection.UP ? searchResult.selectPrevious(scrollPolicy) : null;

        updateSelection(table, oldSelection, selection);
    }

    private void selectFirst(int selectedRowIndex, int selectedColumnIndex) {
        BasicTable<? extends BasicDataModel> table = getSearchableComponent().getTable();
        DataModel dataModel = table.getModel();
        DataSearchResult searchResult = dataModel.getSearchResult();
        DataSearchResultScrollPolicy scrollPolicy = DataSearchResultScrollPolicy.HORIZONTAL;

        DataSearchResultMatch oldSelection = searchResult.getSelectedMatch();
        DataSearchResultMatch selection = searchResult.selectFirst(selectedRowIndex, selectedColumnIndex, scrollPolicy);

        updateSelection(table, oldSelection, selection);
    }

    private static void updateSelection(BasicTable table, DataSearchResultMatch oldSelection, DataSearchResultMatch selection) {
        if (oldSelection != null) {
            DataModelCell cell = oldSelection.getCell();
            Rectangle cellRectangle = table.getCellRect(cell);
            table.repaint(cellRectangle);
        }

        if (selection != null) {
            DataModelCell cell = selection.getCell();
            Rectangle cellRectangle = table.getCellRect(cell);
            table.repaint(cellRectangle);
            cellRectangle.grow(100, 100);
            table.scrollRectToVisible(cellRectangle);
        }
    }

    void updateResult(DataFindModel findModel) {
        Background.run(null, searchHandle, () -> {
            BasicTable table = getSearchableComponent().getTable();
            DataModel dataModel = table.getModel();
            DataSearchResult searchResult = dataModel.getSearchResult();

            try {
                long updateTimestamp = System.currentTimeMillis();
                searchResult.startUpdating(updateTimestamp);

                FindManager findManager = FindManager.getInstance(table.getProject());

                List<DataSearchResultMatch> matches = new ArrayList<>();
                for (Object r : dataModel.getRows()) {
                    DataModelRow row = (DataModelRow) r;
                    for (Object c : row.getCells()) {
                        DataModelCell cell = (DataModelCell) c;
                        String userValue = cell.getPresentableValue();
                        if (Strings.isNotEmpty(userValue)) {
                            int findOffset = 0;
                            while (true) {
                                FindResult findResult = findManager.findString(userValue, findOffset, findModel);
                                if (findResult.isStringFound()) {
                                    int startOffset = findResult.getStartOffset();
                                    int endOffset = findResult.getEndOffset();

                                    searchResult.checkTimestamp(updateTimestamp);
                                    DataSearchResultMatch match = new DataSearchResultMatch(cell, startOffset, endOffset);
                                    matches.add(match);

                                    findOffset = endOffset;
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                }
                searchResult.setMatches(matches);
            } catch (ConcurrentModificationException e){
                conditionallyLog(e);
                throw new OutdatedContentException(this);
            } finally {
                searchResult.stopUpdating();
            }

            Dispatch.run(() -> {
                int selectedRowIndex = table.getSelectedRow();
                int selectedColumnIndex = table.getSelectedRow();
                if (selectedRowIndex < 0) selectedRowIndex = 0;
                if (selectedColumnIndex < 0) selectedColumnIndex = 0;
                getSearchableComponent().cancelEditActions();

                table.clearSelection();
                UserInterface.repaint(table);

                selectFirst(selectedRowIndex, selectedColumnIndex);
                searchResult.notifyListeners();
            });
        });
    }
}

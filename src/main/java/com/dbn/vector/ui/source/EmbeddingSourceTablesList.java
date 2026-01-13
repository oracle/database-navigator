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

package com.dbn.vector.ui.source;

import com.dbn.common.util.Dialogs;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.request.EmbeddingSourceTable;
import com.dbn.vector.ui.source.bulk.TableSelectionDialog;
import com.intellij.openapi.ui.DialogWrapper;

import javax.swing.JList;
import java.util.List;

public class EmbeddingSourceTablesList extends JList<EmbeddingSourceTable> {
  private VectorEmbeddingRequest embeddingRequest;

  public EmbeddingSourceTablesList(List<EmbeddingSourceTable> tableSources, VectorEmbeddingRequest embeddingRequest) {
    super(new EmbeddingSourceTablesListModel(tableSources));
    setCellRenderer(new EmbeddingSourceTablesListRenderer());
    setVisibleRowCount(5);
    this.embeddingRequest = embeddingRequest;
  }

  public void insertRows() {
    Dialogs.show(
            () -> new TableSelectionDialog(embeddingRequest.getProject(), embeddingRequest.getConnection()),
            (dialog, exitCode) -> {
              System.out.println("DBTableList.insertRows: Dialog closed with exitCode=" + exitCode);
              if (exitCode == DialogWrapper.OK_EXIT_CODE) {
                List<EmbeddingSourceTable> selectedTables = dialog.getSelectedTableSources();
                System.out.println("DBTableList.insertRows: Got " + selectedTables.size() + " tables");
                for (EmbeddingSourceTable t : selectedTables) {
                  System.out.println("  - " + t.getSchemaName() + "." + t.getTableName());
                }
                EmbeddingSourceTablesListModel model = getModel();
                model.addAll(selectedTables);
                System.out.println("DBTableList.insertRows: Model now has " + model.getSize() + " items");
              }
            }
    );
  }
  public void insertRow() {
    Dialogs.show(
            () -> new EmbeddingSourceTableDialog(embeddingRequest),
            (dialog, exitCode) -> {
              if (exitCode == DialogWrapper.OK_EXIT_CODE) {
                EmbeddingSourceTable sourceTable = embeddingRequest.getSourceConfig().getSourceTable().clone();
                EmbeddingSourceTablesListModel model = getModel();
                model.add(sourceTable);
              }
            }
    );
  }

  public void removeRows() {
    EmbeddingSourceTablesListModel model = getModel();
    int[] indices = getSelectedIndices();

    model.removeRows(indices);
    setSelectedIndices(new int[0]);
  }

  public void moveRowsUp() {
    EmbeddingSourceTablesListModel model = getModel();
    int[] indices = getSelectedIndices();
    model.moveRowsUp(indices);

    for (int i = 0; i < indices.length; i++) indices[i]--;
    setSelectedIndices(indices);
  }

  public void moveRowsDown() {
    EmbeddingSourceTablesListModel model = getModel();
    int[] indices = getSelectedIndices();
    model.moveRowsDown(indices);

    for (int i = 0; i < indices.length; i++) indices[i]++;
    setSelectedIndices(indices);
  }

  public EmbeddingSourceTablesListModel getModel() {
    return (EmbeddingSourceTablesListModel) super.getModel();
  }

  public List<EmbeddingSourceTable> getTables() {
    return getModel().getElements();
  }
}

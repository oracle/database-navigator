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

package com.dbn.event.notification.ui;

import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.table.DBNColoredTableCellRenderer;
import com.dbn.common.ui.table.DBNTable;
import com.dbn.event.notification.model.DataChangeEventBundle;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;

public class NotificationEventTable extends DBNTable<DataChangeEventBundle> {

  public NotificationEventTable(@NotNull DBNComponent parent, @NotNull DataChangeEventBundle model) {
    super(parent, model, true);
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    setDefaultRenderer(String.class, new CellRenderer());
    setBackground(Colors.getEditorBackground());
    setCellSelectionEnabled(true);
    initTableSorter();
    adjustColumnWidths();

    setAccessibleName(this, "Notification Events Dashboard");
  }

  @Override
  public void setModel(@NotNull TableModel dataModel) {
    super.setModel(dataModel);
    initTableSorter();
  }

  private class CellRenderer extends DBNColoredTableCellRenderer {
    @Override
    protected void customizeCellRenderer(DBNTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
      if (value != null) {
        append(value.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        if (column == 0) {
          String operation = value.toString().toLowerCase();
          if (operation.contains("insert")) {
            setIcon(Icons.ACTION_ADD);
          } else if (operation.contains("update")) {
            setIcon(Icons.ACTION_EDIT);
          } else if (operation.contains("delete")) {
            setIcon(Icons.ACTION_REMOVE);
          }
        }
      }
    }
  }
}
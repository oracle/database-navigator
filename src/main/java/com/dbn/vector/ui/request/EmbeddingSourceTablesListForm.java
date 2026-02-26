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

package com.dbn.vector.ui.request;

import com.dbn.vector.model.request.EmbeddingSourceTable;
import com.dbn.vector.ui.VectorToolboxFormBase;
import com.intellij.ui.ToolbarDecorator;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.ui.util.Decorators.createToolbarDecorator;
import static com.dbn.common.ui.util.Decorators.createToolbarDecoratorComponent;

public class EmbeddingSourceTablesListForm extends VectorToolboxFormBase {
  private JPanel component;
  private JPanel listPanel;

  private final @Getter EmbeddingSourceTablesList tableList;

  public EmbeddingSourceTablesListForm(@NotNull VectorToolboxFormBase parent) {
    super(parent);

    tableList = new EmbeddingSourceTablesList(new ArrayList<>(), getEmbeddingRequest());
    listPanel.add(initListComponent());
  }
  private JPanel initListComponent() {
    ToolbarDecorator decorator = createToolbarDecorator(tableList);
    decorator.setAddAction(b -> tableList.insertRow());
    decorator.setEditAction(b -> tableList.updateRow());
    decorator.setRemoveAction(b -> tableList.removeRows());
    decorator.setMoveUpAction(b -> tableList.moveRowsUp());
    decorator.setMoveDownAction(b -> tableList.moveRowsDown());

    // TODO enable bulk action (find a way to reorder actions)
/*
    decorator.addExtraAction(new Separator());
    decorator.addExtraAction(new AnAction() {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        tableList.insertRows();
      }

      @Override
      public void update(@NotNull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        presentation.setIcon(Icons.ACTION_ADD);
        presentation.setText("Add bulk");
      }
    });
*/

    return createToolbarDecoratorComponent(decorator, tableList);
  }

  @Override
  protected JComponent getMainComponent() {
    return component;
  }

  public List<EmbeddingSourceTable> getTables(){
    return tableList.getModel().getElements();
  }

  public void setTables(List<EmbeddingSourceTable> tables) {
    EmbeddingSourceTablesListModel model = tableList.getModel();
    model.reset(tables);
  }
}

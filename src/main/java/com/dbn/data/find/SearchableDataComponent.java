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

import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.listener.KeyAdapter;
import com.dbn.common.ui.util.Keyboard;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.data.grid.ui.table.basic.BasicTable;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.Shortcut;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import javax.swing.text.JTextComponent;
import java.awt.event.KeyEvent;

public interface SearchableDataComponent extends DBNForm {
    default void showSearchHeader() {
        BasicTable<?> table = getTable();
        table.cancelEditing();
        table.clearSelection();

        DataSearchComponent searchComponent = getSearchComponent();
        searchComponent.initializeFindModel();

        JTextComponent searchField = searchComponent.getSearchField();
        JPanel searchPanel = getSearchPanel();
        if (searchPanel.isVisible()) {
            searchField.selectAll();
        } else {
            searchPanel.setVisible(true);
        }
        searchField.requestFocus();
    }

    default void hideSearchHeader() {
        DataSearchComponent searchComponent = getSearchComponent();
        searchComponent.resetFindModel();

        JPanel searchPanel = getSearchPanel();
        searchPanel.setVisible(false);

        BasicTable<?> table = getTable();
        UserInterface.repaintAndFocus(table);
    }

    default void cancelEditActions() {
        BasicTable<?> table = getTable();
        table.cancelEditing();
    }

    default String getSelectedText() {
        return null;
    }

    default void installSearchKeyListener() {
        Keyboard.insertKeyListener(getTable(), new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                Shortcut[] shortcuts = Keyboard.getShortcuts(IdeActions.ACTION_FIND);
                if (e.isConsumed()) return;
                if (!Keyboard.match(shortcuts, e)) return;

                e.consume();
                showSearchHeader();
            }
        });

    }

    @NotNull
    BasicTable<?> getTable();

    @NotNull
    JPanel getSearchPanel();

    default DataSearchComponent getSearchComponent() {
        return DataSearchComponent.ensure(this);
    }


}

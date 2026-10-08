/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.common.ui.misc;

import com.dbn.common.action.ToggleAction;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.Presentable;
import com.dbn.common.ui.list.ColoredListCellRenderer;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

import javax.swing.JList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DBNMultiSelectComboBox<T> extends DBNActionComboBox<T> {
    private final List<T> values = new ArrayList<>();
    private final Set<T> selectedValues = new LinkedHashSet<>();
    private final Map<AnAction, T> actionValues = new IdentityHashMap<>();
    private boolean singleSelection;

    public DBNMultiSelectComboBox() {
        super(new DBNComboBoxModel<>());
        setRenderer(new SelectionRenderer());
    }

    public void setValues(Collection<? extends T> values) {
        this.values.clear();
        this.values.addAll(values);

        selectedValues.retainAll(this.values);
        if (singleSelection && selectedValues.size() > 1) {
            T selectedValue = selectedValues.iterator().next();
            selectedValues.clear();
            selectedValues.add(selectedValue);
        }

        setModel(new DBNComboBoxModel<>(this.values));
        syncSelectedItem();
        repaint();
    }

    public void addValues(Collection<? extends T> values) {
        for (T value : values) {
            if (!this.values.contains(value)) {
                this.values.add(value);
                getModel().addElement(value);
            }
        }
        repaint();
    }

    public void clearValues() {
        values.clear();
        selectedValues.clear();
        getModel().removeAllElements();
        syncSelectedItem();
        repaint();
    }

    public void setSingleSelection(boolean singleSelection) {
        if (this.singleSelection == singleSelection) return;

        this.singleSelection = singleSelection;
        if (singleSelection && selectedValues.size() > 1) {
            T selectedValue = selectedValues.iterator().next();
            selectedValues.clear();
            selectedValues.add(selectedValue);
            syncSelectedItem();
            fireActionEvent();
        }
    }

    @NotNull
    public List<T> getSelectedItems() {
        return Collections.unmodifiableList(new ArrayList<>(selectedValues));
    }

    public void setSelectedItems(T[] selectedItems) {
        setSelectedItems(Arrays.asList(selectedItems));
    }

    public void setSelectedItems(Collection<? extends T> selectedItems) {
        Set<T> newSelection = new LinkedHashSet<>();
        for (T value : selectedItems) {
            if (values.contains(value)) {
                newSelection.add(value);
                if (singleSelection) break;
            }
        }

        if (Objects.equals(selectedValues, newSelection)) return;

        selectedValues.clear();
        selectedValues.addAll(newSelection);
        syncSelectedItem();
        repaint();
        fireActionEvent();
    }

    @Override
    public void setSelectedItem(Object selectedItem) {
        if (selectedItem == null) {
            setSelectedItems(List.of());
        } else {
            @SuppressWarnings("unchecked") T value = (T) selectedItem;
            setSelectedItems(List.of(value));
        }
    }

    public boolean isSelected(T value) {
        return selectedValues.contains(value);
    }

    public void onSelectionChange(Consumer<List<T>> consumer) {
        addActionListener(e -> consumer.accept(getSelectedItems()));
    }

    @Override
    protected ActionGroup createActionGroup() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionValues.clear();
        for (T value : values) {
            SelectionToggleAction action = new SelectionToggleAction(value);
            actionValues.put(action, value);
            actionGroup.add(action);
        }
        return actionGroup;
    }

    @Override
    protected boolean preselectAction(AnAction action) {
        return singleSelection && Objects.equals(actionValues.get(action), getSelectedItem());
    }

    private void selectValue(T value, boolean selected) {
        Set<T> newSelection = new LinkedHashSet<>(selectedValues);
        if (selected) {
            if (singleSelection) newSelection.clear();
            newSelection.add(value);
        } else {
            if (singleSelection && newSelection.size() == 1 && newSelection.contains(value)) return;
            newSelection.remove(value);
        }
        setSelectedItems(newSelection);
        if (singleSelection && selected) {
            closePopup();
        }
    }

    private void syncSelectedItem() {
        T selectedItem = selectedValues.isEmpty() ? null : selectedValues.iterator().next();
        getModel().setSelectedItem(selectedItem);
    }

    private String getValueName(T value) {
        if (value instanceof Presentable presentable) {
            return presentable.getName();
        }
        return value == null ? "" : value.toString();
    }

    private String getSelectedItemsText() {
        return selectedValues.stream().map(this::getValueName).collect(Collectors.joining(", "));
    }

    private class SelectionToggleAction extends ToggleAction {
        private final T value;

        private SelectionToggleAction(T value) {
            this.value = value;
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            presentation.setIcon(!singleSelection && isSelected(e) ? Icons.ACTION_CHECK : null);
            presentation.setText(getValueName(value), false);
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return DBNMultiSelectComboBox.this.isSelected(value);
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean selected) {
            selectValue(value, selected);
        }
    }

    private class SelectionRenderer extends ColoredListCellRenderer<T> {
        @Override
        protected void customize(@NotNull JList<? extends T> list, T value, int index, boolean selected, boolean hasFocus) {
            String text = index == -1 ? getSelectedItemsText() : getValueName(value);
            append(text);
            if (value instanceof Presentable presentable) {
                setIcon(presentable.getIcon());
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public DBNComboBoxModel<T> getModel() {
        return (DBNComboBoxModel<T>) super.getModel();
    }
}

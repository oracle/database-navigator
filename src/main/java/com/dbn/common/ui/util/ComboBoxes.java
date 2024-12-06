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

package com.dbn.common.ui.util;

import com.dbn.common.ui.Presentable;
import com.dbn.common.ui.misc.DBNComboBoxModel;
import com.intellij.ui.ColoredListCellRenderer;
import org.jetbrains.annotations.NotNull;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.dbn.common.util.Unsafe.cast;

public class ComboBoxes {
    public static void addItems(JComboBox comboBox, Iterable items) {
        for (Object item : items) {
            comboBox.addItem(item);
        }
    }

    public static void addItems(DefaultComboBoxModel comboBox, Iterable items) {
        for (Object item : items) {
            comboBox.addElement(item);
        }
    }

    public static <T extends Presentable> void initComboBox(JComboBox<T> comboBox, T... options) {
        initComboBox(comboBox, Arrays.asList(options));
    }

    public static <T extends Presentable> void initComboBox(JComboBox<T> comboBox, Collection<T> options) {
        DBNComboBoxModel<T> model = new DBNComboBoxModel<>();
        model.getItems().addAll(options);
        comboBox.setModel(model);
        comboBox.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends T> list, T value, int index, boolean selected, boolean hasFocus) {
                if (value != null) {
                    append(value.getName());
                    setIcon(value.getIcon());
                }
            }
        });
    }

    public static <T extends Presentable> void initSelectionListener(JComboBox<T> comboBox, Consumer<T> selectionConsumer) {
        comboBox.addItemListener(e -> {
            if (e.getStateChange() != ItemEvent.SELECTED) return;

            T item = cast(e.getItem());
            selectionConsumer.accept(item);
        });
    }


    /**
     * Initializes the persistence of the combo box selection
     * @param comboBox the {@link JComboBox} to initialize persistence for
     * @param selectionSupplier the supplier of initial selection (will be invoked when model of the combo box changes)
     * @param selectionConsumer the consumer of the selection (will be invoked when selection changes)
     * @param <T> the type of entries in the combo box
     */
    public static <T extends Presentable> void initPersistence(JComboBox<T> comboBox, Supplier<String> selectionSupplier, Consumer<String> selectionConsumer) {
        initSelectionListener(comboBox, s -> selectionConsumer.accept(s == null ? null : s.getName()));

        comboBox.addPropertyChangeListener(e -> {
            if ("model".equals(e.getPropertyName())) {
                selectElement(comboBox, selectionSupplier.get());
                if (comboBox.getSelectedItem() == null && comboBox.getItemCount() > 0) {
                    comboBox.setSelectedIndex(0);
                }
            }
        });
    }

    public static <T> T getSelection(JComboBox<T> comboBox) {
        return (T) comboBox.getSelectedItem();
    }

    public static <T> void setSelection(JComboBox<T> comboBox, T value) {
        comboBox.setSelectedItem(value);
    }

    public static <T extends Presentable> void selectElement(JComboBox<T> comboBox, String name) {
        List<T> elements = getElements(comboBox);
        for (T element : elements) {
            if (element.getName().equals(name)) {
                setSelection(comboBox, element);
                return;
            }
        }

    }

    public static void selectFirstElement(JComboBox comboBox) {
        if (comboBox.getItemCount() == 0) return;
        comboBox.setSelectedIndex(0);
    }

    public static <T> List<T> getElements(JComboBox<T> comboBox) {
        List<T> list = new ArrayList<>();
        ComboBoxModel<T> model = comboBox.getModel();
        for (int i = 0; i< model.getSize(); i++) {
            T element = model.getElementAt(i);
            if (element != null) {
                list.add(element);
            }
        }

        return list;
    }

}

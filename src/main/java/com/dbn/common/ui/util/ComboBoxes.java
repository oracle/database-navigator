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
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.misc.DBNComboBoxModel;
import com.dbn.common.ui.select.DBNComboBoxRenderer;
import org.jetbrains.annotations.Nullable;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.MutableComboBoxModel;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static com.dbn.common.ui.util.ClientProperty.VISITED;
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

    public static <T> void initComboBox(JComboBox<T> comboBox, boolean withEmptyOption, T... options) {
        initComboBox(comboBox, Arrays.asList(options));
        if (withEmptyOption) {
            MutableComboBoxModel<T> mutableModel = cast(comboBox.getModel());
            mutableModel.insertElementAt(null, 0);
        }
    }

    public static <T> void initComboBox(JComboBox<T> comboBox, T... options) {
        initComboBox(comboBox, Arrays.asList(options));
    }

    public static void setEmptyOptionsText(JComboBox comboBox, String text) {
        ClientProperty.EMPTY_OPTIONS_TEXT.set(comboBox, text);
    }

    public static String getEmptyOptionsText(JComboBox comboBox) {
        return ClientProperty.EMPTY_OPTIONS_TEXT.get(comboBox);
    }

    public static <T> void resetComboBox(JComboBox<T> comboBox) {
        initComboBox(comboBox, Collections.emptyList());
        selectElement(comboBox, null);
        VISITED.set(comboBox, false); // reset validation "visited" marker
    }

    public static <T> void initComboBox(JComboBox<T> comboBox, Collection<T> options) {
        T selection = getSelection(comboBox);
        DBNComboBoxModel<T> model = new DBNComboBoxModel<>(options);
        comboBox.setModel(model);
        initComboBoxRenderer(comboBox);
        if (selection != null && options.contains(selection)) {
            setSelection(comboBox, selection);
        }
        VISITED.set(comboBox, false); // reset validation "visited" marker
    }

    public static <T> void initComboBoxRenderer(JComboBox<T> comboBox) {
        comboBox.setRenderer(new DBNComboBoxRenderer<T>(comboBox));
    }

    public static <T> void initSelectionListener(JComboBox<T> comboBox, Consumer<T> selectionConsumer) {
        comboBox.addItemListener(e -> {
            if (e.getStateChange() != ItemEvent.SELECTED) return;

            T item = cast(e.getItem());
            selectionConsumer.accept(item);
        });
    }


    @Nullable
    public static <T> T getSelection(JComboBox<T> comboBox) {
        return (T) comboBox.getSelectedItem();
    }

    public static <T> void setSelection(JComboBox<T> comboBox, T value) {
        comboBox.setSelectedItem(value);
    }

    public static <T> void selectElement(JComboBox<T> comboBox, String name) {
        List<T> elements = getElements(comboBox);
        for (T element : elements) {
            String elementName = element.toString();
            if (element instanceof Presentable) {
                Presentable presentable = (Presentable) element;
                elementName = presentable.getName();
            }

            if (elementName.equals(name)) {
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

    public static <T> void onSelectionChange(DBNComboBox<T> comboBox, Consumer<T> consumer) {
        comboBox.addListener((oldValue, newValue) -> {
            consumer.accept(newValue);
        });
    }

    public static <T> void onSelectionChange(JComboBox<T> comboBox, Consumer<T> consumer) {
        if (comboBox instanceof DBNComboBox) {
            DBNComboBox<T> dbnComboBox = cast(comboBox);
            onSelectionChange(dbnComboBox, consumer);
            return;
        }

        comboBox.addItemListener(e -> {
            if (e.getStateChange() != ItemEvent.SELECTED) return;
            T newValue = cast(e.getItem());

            consumer.accept(newValue);
        });
    }
}

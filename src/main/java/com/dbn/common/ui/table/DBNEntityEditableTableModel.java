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

package com.dbn.common.ui.table;

import com.dbn.common.util.Cloneable;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Lists;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static com.dbn.common.util.CollectionUtil.cloneElements;
import static com.dbn.common.util.Unsafe.cast;

/**
 * Typed implementation of an editable table model
 * It assumes the data of the table can be stored as a list of entities, by presenting different attributes of the given entities
 * It also assumes the elements are cloneable and have an accessible non-args constructor (to allow creating blank new entries in the editable tables)
 *
 * @param <T> the type of the entity behind the table model
 *
 * @author Dan Cioca (Oracle)
 */
@Getter
@Setter
public abstract class DBNEntityEditableTableModel<T extends Cloneable<T>> extends DBNEditableTableModel {
    private Supplier<List<T>> elements;
    private List<T> originalElements;
    private final List<ColumnDefinition<T, ?>> columns = new ArrayList<>();
    private transient int elementsCount;

    protected DBNEntityEditableTableModel(Supplier<List<T>> elements) {
        this.elements = elements;
        this.originalElements = cloneElements(elements.get());
        this.elementsCount = originalElements.size();
    }

    public void resetChanges() {
        List<T> elements = getElements();
        elements.clear();
        elements.addAll(originalElements);
        notifyListeners(-1, -1, -1);
    }

    public void applyChanges() {
        List<T> elements = getElements();
        this.originalElements = cloneElements(elements);
    }

    public boolean moveRowDown(int row) {
        List<T> elements = getElements();
        if (row >= elements.size() - 1) return false;

        Collections.swap(elements, row, row + 1);
        notifyListeners(row, row + 1, -1);
        return true;
    }

    public boolean moveRowUp(int row) {
        List<T> elements = getElements();
        if (row <= 0) return false;

        Collections.swap(elements, row, row - 1);
        notifyListeners(row, row - 1, -1);
        return true;


    }

    public List<T> getElements() {
        List<T> elements = this.elements.get();
        int elementsCount = elements.size();
        if (elementsCount != this.elementsCount) {
            this.elementsCount = elementsCount;
            notifyListeners(-1, -1, -1);
        }
        return elements;
    }

    /**
     * Definition of a column in the typed table model
     * @param <E> the entity type
     * @param <V> the entity attribute type
     */
    @Getter
    private static class ColumnDefinition<E, V> {
        private final @Nls String name;
        private final Class<V> type;
        private final ValueGetter<E, V> valueGetter;
        private final ValueSetter<E, V> valueSetter;

        public ColumnDefinition(@Nls String name, Class<V> type, ValueGetter<E, V> valueGetter, ValueSetter<E, V> valueSetter) {
            this.name = name;
            this.type = type;
            this.valueGetter = valueGetter;
            this.valueSetter = valueSetter;
        }
    }

    public void addElement(T element) {
        List<T> elements = this.elements.get();
        elements.add(element);
        notifyListeners(elements.size() - 1, elements.size() - 1, -1);
    }

    /**
     * Utility for adding a column to the table model
     * @param name the name of the column
     * @param type the data type of the column
     * @param valueGetter functional interface to load the value of the attribute representing the column
     * @param valueSetter functional interface to update the value of the attribute representing the column
     * @param <V> the type of the attribute representing the column
     */
    public final <V> void addColumn(@Nls String name, Class<V> type, ValueGetter<T, V> valueGetter, ValueSetter<T, V> valueSetter) {
        columns.add(new ColumnDefinition<>(name, type, valueGetter, valueSetter));
    }

    @Nullable
    private <V> ColumnDefinition<T, V> getColumnDefinition(int columnIndex) {
        if (columnIndex >= columns.size()) return null;
        return cast(columns.get(columnIndex));
    }

    private T getElement(int rowIndex) {
        List<T> elements = this.elements.get();
        return Lists.getElementAt(elements, rowIndex);
    }

    @Override
    public final int getRowCount() {
        return getElements().size();
    }

    @Override
    public final int getColumnCount() {
        return columns.size();
    }

    @Override
    public final boolean isCellEditable(int rowIndex, int columnIndex) {
        ColumnDefinition<T, Object> definition = getColumnDefinition(columnIndex);
        if (definition == null) return false;

        ValueSetter<T, Object> valueSetter = definition.getValueSetter();
        return valueSetter != null;
    }


    @Override
    public final @Nls String getColumnName(int columnIndex) {
        ColumnDefinition<?, ?> definition = getColumnDefinition(columnIndex);
        if (definition == null) return null;

        return definition.getName();
    }

    @Override
    public final Class<?> getColumnClass(int columnIndex) {
        ColumnDefinition<T, ?> definition = getColumnDefinition(columnIndex);
        if (definition == null) return null;

        return definition.getType();
    }

    @Override
    public final Object getValueAt(int rowIndex, int columnIndex) {
        return getValue(rowIndex, columnIndex);
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        boolean changed = setValue(value, rowIndex, columnIndex);

        if (changed) notifyListeners(rowIndex, rowIndex, columnIndex);
    }

    protected final <V> V getValue(int rowIndex, int columnIndex) {
        ColumnDefinition<T, V> definition = getColumnDefinition(columnIndex);
        if (definition == null) return null;

        T entity = getElement(rowIndex);
        ValueGetter<T, V> valueGetter = definition.getValueGetter();
        return valueGetter.getValue(entity);
    }

    protected final <V> boolean setValue(V value, int rowIndex, int columnIndex) {
        ColumnDefinition<T, V> definition = getColumnDefinition(columnIndex);
        if (definition == null) return false;

        ValueSetter<T, V> valueSetter = definition.getValueSetter();
        if (valueSetter == null) return false;

        V currentValue = getValue(rowIndex, columnIndex);
        if (Commons.match(currentValue, value)) return false;

        T entity = getElement(rowIndex);
        valueSetter.setValue(entity, value);

        return true;
    }

    @Override
    public final void insertRow(int rowIndex) {
        List<T> elements = this.elements.get();
        elements.add(rowIndex, createElement());
        notifyListeners(rowIndex, elements.size() - 1, -1);
    }

    protected T createElement() {
        throw new UnsupportedOperationException("createElement() not implemented");
    }

    @Override
    public final void removeRow(int rowIndex) {
        List<T> elements = this.elements.get();
        if (elements.size() <= rowIndex) return;

        elements.remove(rowIndex);
        notifyListeners(rowIndex, elements.size() - 1, -1);
    }

    @FunctionalInterface
    public interface ValueGetter<E, T> {
        T getValue(E entity);
    }

    @FunctionalInterface
    public interface ValueSetter<E, T> {
        void setValue(E entity, T value);
    }
}

/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * This software is dual-licensed to you under the Universal Permissive License
 * (UPL) 1.0 as shown at https://oss.oracle.com/licenses/upl or Apache License
 * 2.0 as shown at http://www.apache.org/licenses/LICENSE-2.0. You may choose
 * either license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.dbn.common.ui.table;

import com.dbn.common.Reflection;
import com.dbn.common.util.Cloneable;
import com.dbn.common.util.CollectionUtil;
import com.dbn.common.util.Commons;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
public abstract class DBNTypedEditableTableModel<T extends Cloneable<T>> extends DBNEditableTableModel {
    private final Class<T> entityType;
    private final List<T> elements;
    private final List<ColumnDefinition<T, ?>> columns = new ArrayList<>();

    protected DBNTypedEditableTableModel(Class<T> entityType, List<T> elements) {
        this.entityType = entityType;
        this.elements = new ArrayList<>(elements);
    }

    public void setElements(List<T> elements) {
        this.elements.clear();
        CollectionUtil.cloneElements(elements, this.elements);
        notifyListeners(0, getRowCount(), -1);
    }

    /**
     * Definition of a column in the typed table model
     * @param <E> the entity type
     * @param <V> the entity attribute type
     */
    @Getter
    private static class ColumnDefinition<E, V> {
        private final String name;
        private final Class<V> type;
        private final ValueGetter<E, V> valueGetter;
        private final ValueSetter<E, V> valueSetter;

        public ColumnDefinition(String name, Class<V> type, ValueGetter<E, V> valueGetter, ValueSetter<E, V> valueSetter) {
            this.name = name;
            this.type = type;
            this.valueGetter = valueGetter;
            this.valueSetter = valueSetter;
        }
    }

    /**
     * Utility for adding a column to the table model
     * @param name the name of the column
     * @param type the data type of the column
     * @param valueGetter functional interface to load the value of the attribute representing the column
     * @param valueSetter functional interface to update the value of the attribute representing the column
     * @param <V> the type of the attribute representing the column
     */
    public final <V> void addColumn(String name, Class<V> type, ValueGetter<T, V> valueGetter, ValueSetter<T, V> valueSetter) {
        columns.add(new ColumnDefinition<>(name, type, valueGetter, valueSetter));
    }

    @Nullable
    private <V> ColumnDefinition<T, V> getColumnDefinition(int columnIndex) {
        if (columnIndex >= columns.size()) return null;
        return cast(columns.get(columnIndex));
    }

    private T getElement(int rowIndex) {
        while (rowIndex >= elements.size()) {
            elements.add(createElement());
        }
        return elements.get(rowIndex);
    }

    private @NotNull T createElement() {
        return Reflection.newInstance(entityType);
    }

    @Override
    public final int getRowCount() {
        return elements.size();
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
    public final String getColumnName(int columnIndex) {
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

        V currentValue = getValue(rowIndex, columnIndex);
        if (Commons.match(currentValue, value)) return false;

        T entity = getElement(rowIndex);
        ValueSetter<T, V> valueSetter = definition.getValueSetter();
        valueSetter.setValue(entity, value);

        return true;
    }

    @Override
    public final void insertRow(int rowIndex) {
        elements.add(rowIndex, createElement());
        notifyListeners(rowIndex, elements.size() - 1, -1);
    }

    @Override
    public final void removeRow(int rowIndex) {
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

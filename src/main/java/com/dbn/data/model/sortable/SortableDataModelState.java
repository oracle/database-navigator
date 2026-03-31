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

package com.dbn.data.model.sortable;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Cloneable;
import com.dbn.data.model.DataModelState;
import com.dbn.data.sorting.SortingState;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.util.Unsafe.cast;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class SortableDataModelState<T extends SortableDataModelState> extends DataModelState implements Cloneable<SortableDataModelState>, PersistentStateElement {
    protected SortingState sortingState = new SortingState();

    @Override
    @SneakyThrows
    public T clone() {
        T clone = cast(super.clone());
        clone.sortingState = Cloneable.clone(sortingState);
        return clone;
    }

    @Override
    public void readState(@NotNull Element element) {
        Element sortingElement = element.getChild("sorting");
        sortingState.readState(sortingElement);
    }

    @Override
    public void writeState(Element element) {
        Element sortingElement = newElement(element, "sorting");
        sortingState.writeState(sortingElement);
    }
}

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

package com.dbn.common.ui.list;

import javax.swing.JList;
import java.util.List;

import static com.dbn.common.util.Unsafe.cast;

public class MutableObjectList<T> extends JList<T> {

    public MutableObjectList(MutableObjectListModel<T> dataModel) {
        super(dataModel);
    }

    public void removeRows() {
        MutableObjectListModel<T> model = getModel();
        int[] indices = getSelectedIndices();

        model.removeRows(indices);
        setSelectedIndices(new int[0]);
    }

    public void moveRowsUp() {
        MutableObjectListModel<T> model = getModel();
        int[] indices = getSelectedIndices();
        model.moveRowsUp(indices);

        for (int i = 0; i < indices.length; i++) indices[i]--;
        setSelectedIndices(indices);
    }

    public void moveRowsDown() {
        MutableObjectListModel<T> model = getModel();
        int[] indices = getSelectedIndices();
        model.moveRowsDown(indices);

        for (int i = 0; i < indices.length; i++) indices[i]++;
        setSelectedIndices(indices);
    }

    public MutableObjectListModel<T> getModel() {
        return cast(super.getModel());
    }

    public List<T> getElements() {
        return getModel().getElements();
    }
}

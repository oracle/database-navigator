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

import com.dbn.data.model.DataModelCell;
import lombok.Getter;
import lombok.NonNull;

import static com.dbn.common.dispose.Failsafe.nd;

@Getter
public class DataSearchResultMatch {
    private final int startOffset;
    private final int endOffset;
    private final DataModelCell cell;

    public DataSearchResultMatch(DataModelCell cell, int startOffset, int endOffset) {
        this.cell = cell;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    @NonNull
    public DataModelCell getCell() {
        return nd(cell);
    }

    public int getColumnIndex() {
        return getCell().getIndex();
    }

    public int getRowIndex() {
        return getCell().getRow().getIndex();
    }
}

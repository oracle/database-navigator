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

package com.dbn.editor.data;

import com.dbn.common.property.PropertyHolderBase;

import static com.dbn.editor.data.DataLoadInstruction.DELIBERATE_ACTION;
import static com.dbn.editor.data.DataLoadInstruction.PRESERVE_CHANGES;
import static com.dbn.editor.data.DataLoadInstruction.REBUILD;
import static com.dbn.editor.data.DataLoadInstruction.USE_CURRENT_FILTER;
import static com.dbn.editor.data.DataLoadInstruction.VALUES;

public class DataLoadInstructions extends PropertyHolderBase.IntStore<DataLoadInstruction> {


    public DataLoadInstructions(DataLoadInstruction... instructions) {
        for (DataLoadInstruction instruction : instructions) {
            set(instruction, true);
        }
    }

    public static DataLoadInstructions clone(DataLoadInstructions source) {
        DataLoadInstructions instructions = new DataLoadInstructions();
        instructions.inherit(source);
        return instructions;
    }

    @Override
    protected DataLoadInstruction[] properties() {
        return VALUES;
    }

    public boolean isUseCurrentFilter() {
        return is(USE_CURRENT_FILTER);
    }

    public boolean isPreserveChanges() {
        return is(PRESERVE_CHANGES);
    }

    public boolean isDeliberateAction() {
        return is(DELIBERATE_ACTION);
    }

    public boolean isRebuild() {
        return is(REBUILD);
    }

    public void setUseCurrentFilter(boolean value) {
        set(USE_CURRENT_FILTER, value);
    }

    public void setKeepChanges(boolean value) {
        set(PRESERVE_CHANGES, value);
    }

    public void setDeliberateAction(boolean value) {
        set(DELIBERATE_ACTION, value);
    }

    public void setRebuild(boolean value) {
        set(REBUILD, value);
    }
}

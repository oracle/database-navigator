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

package com.dbn.common.navigation;

import com.dbn.common.property.PropertyHolderBase;

import static com.dbn.common.navigation.NavigationInstruction.FOCUS;
import static com.dbn.common.navigation.NavigationInstruction.OPEN;
import static com.dbn.common.navigation.NavigationInstruction.RESET;
import static com.dbn.common.navigation.NavigationInstruction.SCROLL;
import static com.dbn.common.navigation.NavigationInstruction.SELECT;
import static com.dbn.common.navigation.NavigationInstruction.VALUES;

public class NavigationInstructions extends PropertyHolderBase.IntStore<NavigationInstruction> {

    public static final NavigationInstructions NONE = new NavigationInstructions();

    private NavigationInstructions(NavigationInstruction ... instructions) {
        super(instructions);
    }

    public static NavigationInstructions create(NavigationInstruction ... instructions) {
        return new NavigationInstructions(instructions);
    }

    public boolean isOpen() {
        return is(OPEN);
    }

    public boolean isFocus() {
        return is(FOCUS);
    }

    public boolean isScroll() {
        return is(SCROLL);
    }

    public boolean isSelect() {
        return is(SELECT);
    }

    public boolean isReset() {
        return is(RESET);
    }

    public NavigationInstructions with(NavigationInstruction instruction) {
        return with(instruction, true);
    }

    public NavigationInstructions without(NavigationInstruction instruction) {
        return with(instruction, false);
    }

    public NavigationInstructions with(NavigationInstruction instruction, boolean value) {
        set(instruction, value);
        return this;
    }

    @Override
    protected NavigationInstruction[] properties() {
        return VALUES;
    }
}

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

package com.dbn.common.ui;

import com.dbn.common.routine.Consumer;
import lombok.Getter;
import org.jetbrains.annotations.Nls;

import javax.swing.Icon;
import java.util.function.Supplier;

@Getter
public abstract class ValueFactory<T> {
    private final @Nls String actionName;

    public ValueFactory(@Nls String actionName) {
        this.actionName = actionName;
    }

    public Icon getIcon(){
        return null;
    }

    public abstract void createValue(Consumer<T> consumer);

    public static <T> ValueFactory<T> create(@Nls String actionName, Supplier<T> supplier) {
        return new ValueFactory<T>(actionName) {
            @Override
            public void createValue(Consumer<T> consumer) {
                T value = supplier.get();
                consumer.accept(value);
            }
        };
    }
}

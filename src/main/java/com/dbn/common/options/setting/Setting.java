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

package com.dbn.common.options.setting;

import com.dbn.common.util.Commons;
import com.intellij.openapi.options.ConfigurationException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public abstract class Setting<T, E> {
    private final String name;
    private T value;

    protected Setting(String name, T value) {
        this.name = name;
        this.value = value;
    }

    public T value() {
        return value;
    }

    public boolean setValue(T value) {
        boolean response = !Commons.match(this.value, value);
        this.value = value;
        return response;
    }

    @Override
    public String toString() {
        return "[" + getClass().getSimpleName() + "] " + name + " = " + value;
    }

    public abstract boolean to(E component) throws ConfigurationException;

    public abstract void from(E component);
}

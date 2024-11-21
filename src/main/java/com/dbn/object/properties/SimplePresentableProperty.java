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

package com.dbn.object.properties;

import com.intellij.pom.Navigatable;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.swing.Icon;

@Getter
@EqualsAndHashCode(callSuper = false)
public class SimplePresentableProperty extends PresentableProperty{
    private final String name;
    private final String value;
    private final Icon icon;

    public SimplePresentableProperty(String name, String value, Icon icon) {
        this.name = name;
        this.value = value;
        this.icon = icon;
    }

    public SimplePresentableProperty(String name, String value) {
        this(name, value, null);
    }

    @Override
    public Navigatable getNavigatable() {
        return null;
    }
}

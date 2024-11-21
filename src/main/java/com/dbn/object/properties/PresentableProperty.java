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

import javax.swing.Icon;

import static com.dbn.common.dispose.Failsafe.guarded;

public abstract class PresentableProperty {
    public abstract String getName();

    public abstract String getValue();

    public abstract Icon getIcon();

    public String toString() {
        return guarded("DISPOSED", this, p -> p.getName() + ": " + p.getValue());
    }

    public abstract Navigatable getNavigatable();
}

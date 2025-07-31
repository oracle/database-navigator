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

package com.dbn.common.filter;

import com.dbn.common.util.Lists;
import lombok.Value;
import org.jetbrains.annotations.NonNls;

import javax.swing.Icon;
import java.util.Collection;
import java.util.List;

import static com.dbn.common.util.Strings.containsIgnoreCase;
import static com.dbn.common.util.Strings.equalsIgnoreCase;
import static com.dbn.common.util.Strings.isEmpty;

@Value
public class FilterOption {
    private final String value;
    private final String name;
    private final Icon icon;

    public FilterOption(String value) {
        this(value, value, null);
    }

    public FilterOption(@NonNls String value, String name, Icon icon) {
        this.value = value;
        this.name = name;
        this.icon = icon;
    }

    public static List<FilterOption> fromValues(Collection<String> values) {
        return Lists.convert(values, value -> new FilterOption(value));
    }

    public boolean matches(@NonNls String value) {
        return isEmpty(this.value) || this.value.equals(value);
    }

    public boolean matchesLike(@NonNls String value) {
        return isEmpty(this.value) || containsIgnoreCase(value, this.value);
    }

    public boolean matchesIgnoreCase(@NonNls String value) {
        return isEmpty(this.value) || equalsIgnoreCase(value, this.value);
    }
}

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

package com.dbn.connection;

import com.dbn.common.constant.Constant;
import com.dbn.common.ui.Presentable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum ServerType implements Presentable, Constant<ServerType> {
    DEFAULT,
    DEDICATED,
    SHARED,
    POOLED,
    ;

    @Override
    public @NotNull String getName() {
        return name();
    }

    @Nullable
    public static ServerType get(String id) {
        return Arrays
                .stream(values())
                .filter(p -> p.name().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    @Override
    public String toString() {
        return name();
    }
}

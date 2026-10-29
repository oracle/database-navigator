/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.common.ui.table;

import com.intellij.openapi.util.NlsActions.ActionText;
import com.intellij.openapi.util.NlsContexts.Tooltip;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** An action that can be performed for a table row when its availability condition is met. */
public final class DBNTableRowAction<R> {
    private final @Nullable Icon icon;
    private final @NotNull @ActionText String name;
    private final @Nullable @Tooltip String tooltip;
    private final Predicate<? super R> availability;
    private final Consumer<? super R> action;

    public DBNTableRowAction(
            @Nullable Icon icon,
            @NotNull @ActionText String name,
            @Nullable @Tooltip String tooltip,
            @NotNull Predicate<? super R> availability,
            @NotNull Consumer<? super R> action) {
        this.name = Objects.requireNonNull(name);
        this.icon = icon;
        this.tooltip = tooltip;
        this.availability = Objects.requireNonNull(availability);
        this.action = Objects.requireNonNull(action);
    }

    public @NotNull @ActionText String getName() {
        return name;
    }

    public @Nullable Icon getIcon() {
        return icon;
    }

    public boolean isAvailable(@Nullable R row) {
        return row != null && availability.test(row);
    }

    public void perform(@Nullable R row) {
        if (isAvailable(row)) action.accept(row);
    }

    public @Nullable @Tooltip String getTooltip(@Nullable R row) {
        return isAvailable(row) ? tooltip : null;
    }
}

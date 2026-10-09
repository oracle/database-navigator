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

package com.dbn.connection.context.ui;

import com.dbn.common.text.TextContent;
import com.dbn.connection.context.DatabaseContextSelection;
import com.intellij.openapi.util.NlsContexts.DialogMessage;
import com.intellij.openapi.util.NlsContexts.DialogTitle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

import static com.dbn.nls.NlsResources.txt;

/**
 * Input used to configure and transport a database context selector.
 *
 * <p>The input contains the presentation, initial selection, optional session
 * selection, validation, and continuation behavior. The project is supplied
 * separately by {@link DatabaseContextSelector} because it is the runtime
 * context in which the selector loads database objects.</p>
 */
public final class DatabaseContextSelectorInput {
    private String title = txt("msg.shared.title.SelectDatabaseContext");
    private TextContent headerContent = TextContent.plain(txt("app.shared.hint.DatabaseContextSelection"));
    private DatabaseContextSelection initialSelection;
    private boolean sessionSelectionEnabled;
    private Function<DatabaseContextSelection, @Nullable @DialogMessage String> selectionVerification = selection -> null;
    private Consumer<DatabaseContextSelection> continueCallback;

    @NotNull
    @DialogTitle
    public String getTitle() {
        return title;
    }

    @NotNull
    public TextContent getHeaderContent() {
        return headerContent;
    }

    @Nullable
    public DatabaseContextSelection getInitialSelection() {
        return initialSelection;
    }

    public boolean isSessionSelectionEnabled() {
        return sessionSelectionEnabled;
    }

    @NotNull
    public Function<DatabaseContextSelection, @Nullable @DialogMessage String> getSelectionVerification() {
        return selectionVerification;
    }

    @Nullable
    public Consumer<DatabaseContextSelection> getContinueCallback() {
        return continueCallback;
    }

    @NotNull
    public DatabaseContextSelectorInput withTitle(@DialogTitle @NotNull String title) {
        this.title = title;
        return this;
    }

    @NotNull
    public DatabaseContextSelectorInput withHeader(@NotNull TextContent headerContent) {
        this.headerContent = headerContent;
        return this;
    }

    @NotNull
    public DatabaseContextSelectorInput withInitialSelection(@Nullable DatabaseContextSelection initialSelection) {
        this.initialSelection = initialSelection;
        return this;
    }

    @NotNull
    public DatabaseContextSelectorInput withSessionSelector() {
        return withSessionSelector(true);
    }

    @NotNull
    public DatabaseContextSelectorInput withSessionSelector(boolean enabled) {
        this.sessionSelectionEnabled = enabled;
        return this;
    }

    @NotNull
    public DatabaseContextSelectorInput withVerification(
            @Nullable Function<DatabaseContextSelection, @Nullable @DialogMessage String> selectionVerification) {
        this.selectionVerification = selectionVerification == null ? selection -> null : selectionVerification;
        return this;
    }

    @NotNull
    public DatabaseContextSelectorInput withContinueCallback(@NotNull Consumer<DatabaseContextSelection> continueCallback) {
        this.continueCallback = continueCallback;
        return this;
    }
}

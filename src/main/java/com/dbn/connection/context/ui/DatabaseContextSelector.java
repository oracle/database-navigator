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
import com.dbn.common.util.Dialogs;
import com.dbn.connection.context.DatabaseContextSelection;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.NlsContexts.DialogMessage;
import com.intellij.openapi.util.NlsContexts.DialogTitle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.dbn.common.ui.util.Popups.resizeToFitContent;
import static com.dbn.common.util.Dialogs.show;

/**
 * Fluent creator for database context selection dialogs and popups.
 *
 * <p>The selector starts with a generic context header and connection/schema
 * selection. Session selection must be explicitly enabled by the caller.
 * Verification is optional. A Continue callback must be configured before the
 * selector is shown.</p>
 */
public final class DatabaseContextSelector {
    private final Project project;
    private final DatabaseContextSelectorInput input;

    private DatabaseContextSelector(@NotNull Project project, @NotNull DatabaseContextSelectorInput input) {
        this.project = project;
        this.input = input;
    }

    @NotNull
    public static DatabaseContextSelector create(@NotNull Project project) {
        return create(project, new DatabaseContextSelectorInput());
    }

    @NotNull
    public static DatabaseContextSelector create(
            @NotNull Project project,
            @NotNull DatabaseContextSelectorInput input) {
        return new DatabaseContextSelector(project, input);
    }

    @NotNull
    public DatabaseContextSelector withTitle(@DialogTitle @NotNull String title) {
        input.withTitle(title);
        return this;
    }

    @NotNull
    public DatabaseContextSelector withHeader(@NotNull TextContent headerContent) {
        input.withHeader(headerContent);
        return this;
    }

    @NotNull
    public DatabaseContextSelector withInitialSelection(@Nullable DatabaseContextSelection initialSelection) {
        input.withInitialSelection(initialSelection);
        return this;
    }

    @NotNull
    public DatabaseContextSelector withSessionSelector() {
        return withSessionSelector(true);
    }

    @NotNull
    public DatabaseContextSelector withSessionSelector(boolean enabled) {
        input.withSessionSelector(enabled);
        return this;
    }

    @NotNull
    public DatabaseContextSelector withVerification(
            @Nullable Function<DatabaseContextSelection, @Nullable @DialogMessage String> selectionVerification) {
        input.withVerification(selectionVerification);
        return this;
    }

    @NotNull
    public DatabaseContextSelector withContinueCallback(@NotNull Consumer<DatabaseContextSelection> continueCallback) {
        input.withContinueCallback(continueCallback);
        return this;
    }

    public void showDialog() {
        show(() -> new DatabaseContextSelectionDialog(project, input));
    }

    public void showPopup(@NotNull JComponent aroundComponent) {
        DatabaseContextSelectionForm form = createForm();

        ComponentPopupBuilder popupBuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(
                form.getComponent(),
                form.getPreferredFocusedComponent());
        popupBuilder.setRequestFocus(true);
        popupBuilder.setResizable(false);

        JBPopup popup = popupBuilder.createPopup();
        form.setContentChangeCallback(() -> Dialogs.resizeToFitContent(form.getComponent()));
        form.setContinueCompletionCallback(() -> popup.cancel());
        Disposer.register(popup, form);
        popup.showUnderneathOf(aroundComponent);
        resizeToFitContent(popup);
    }

    @NotNull
    private DatabaseContextSelectionForm createForm() {
        return new DatabaseContextSelectionForm(project, input);
    }
}

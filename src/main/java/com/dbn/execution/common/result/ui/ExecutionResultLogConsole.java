/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.execution.common.result.ui;

import com.dbn.common.icon.Icons;
import com.dbn.common.thread.Dispatch;
import com.dbn.connection.ConnectionHandler;
import com.dbn.execution.logging.ui.DatabaseLoggingResultConsole;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeListener;

/** Logging console and tab state used by an execution result form. */
public class ExecutionResultLogConsole extends DatabaseLoggingResultConsole implements Disposable {
    private JTabbedPane tabs;
    private JComponent tabComponent;
    private ChangeListener tabSelectionListener;

    public ExecutionResultLogConsole(
            @NotNull ConnectionHandler connection,
            String title,
            boolean buildInActions) {
        super(connection, title, buildInActions);
        normalizeBorders();
    }

    public void installOn(@NotNull JTabbedPane tabs) {
        removeTabListener();
        this.tabs = tabs;
        this.tabComponent = getComponent();
        tabs.addTab(getTitle(), Icons.EXEC_LOG_OUTPUT_CONSOLE, tabComponent);

        Dispatch.run(tabComponent, () -> normalizeBorders());
        tabSelectionListener = e -> updateTabIcon(false);
        tabs.addChangeListener(tabSelectionListener);
        updateTabIcon(false);
    }

    public void markOutputUnread() {
        updateTabIcon(true);
    }

    private void updateTabIcon(boolean unread) {
        if (tabs == null || tabComponent == null) return;

        int tabIndex = tabs.indexOfComponent(tabComponent);
        if (tabIndex < 0) return;

        if (tabs.getSelectedComponent() == tabComponent) {
            tabs.setIconAt(tabIndex, Icons.EXEC_LOG_OUTPUT_CONSOLE);
        } else if (unread) {
            tabs.setIconAt(tabIndex, Icons.EXEC_LOG_OUTPUT_CONSOLE_UNREAD);
        }
    }

    private void removeTabListener() {
        if (tabSelectionListener != null && tabs != null) {
            tabs.removeChangeListener(tabSelectionListener);
            tabSelectionListener = null;
        }
    }

    @Override
    public void dispose() {
        removeTabListener();
        super.dispose();
        tabs = null;
        tabComponent = null;
    }
}

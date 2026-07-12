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

import com.dbn.common.dispose.Disposer;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.util.Borders;
import com.dbn.connection.ConnectionHandler;
import com.dbn.execution.logging.LogOutput;
import com.dbn.execution.logging.LogOutputContext;
import com.dbn.execution.logging.ui.DatabaseLoggingResultConsole;
import com.intellij.openapi.Disposable;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeListener;

/** Logging console and tab state used by an execution result form. */
public class ExecutionResultLogConsole implements Disposable {
    private final DatabaseLoggingResultConsole console;
    private JTabbedPane tabs;
    private JComponent tabComponent;
    private ChangeListener tabSelectionListener;

    public ExecutionResultLogConsole(
            @NotNull ConnectionHandler connection,
            String title,
            boolean buildInActions) {
        console = new DatabaseLoggingResultConsole(connection, title, buildInActions);
        console.getComponent().setBorder(Borders.lineBorder(JBColor.border(), 0, 0, 1, 0));
    }

    @NotNull
    public void writeToConsole(@NotNull LogOutputContext context, @NotNull LogOutput output) {
        console.writeToConsole(context, output);
    }

    @NotNull
    public JComponent getComponent() {
        return console.getComponent();
    }

    @NotNull
    public String getTitle() {
        return console.getTitle();
    }

    public void installOn(@NotNull JTabbedPane tabs) {
        removeTabListener();
        this.tabs = tabs;
        this.tabComponent = console.getComponent();
        tabs.addTab(console.getTitle(), Icons.EXEC_LOG_OUTPUT_CONSOLE, tabComponent);
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
        Disposer.dispose(console);
        tabs = null;
        tabComponent = null;
    }
}

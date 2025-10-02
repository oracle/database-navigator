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

package com.dbn.assistant.tool.impl;

import com.dbn.assistant.tool.AssistantToolBase;
import com.dbn.assistant.tool.spec.ConsoleEditorTool;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.console.DatabaseConsoleManager;
import com.dbn.editor.console.SQLConsoleEditor;
import com.dbn.object.DBConsole;
import com.dbn.object.type.DBObjectType;
import com.dbn.vfs.DBConsoleType;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.List;
import java.util.Set;

import static com.dbn.common.util.Naming.nextNumberedIdentifier;

public class ConsoleEditorToolImpl extends AssistantToolBase implements ConsoleEditorTool {

    @Override
    public List<String> listSqlConsoleNames() {
        List<DBConsole> consoles = getConnection().getConsoleBundle().getConsoles();
        return getObjectNames(consoles, false);
    }

    @Override
    public String getCurrentConsoleName() {
        FileEditorManager editorManager = FileEditorManager.getInstance(getProject());
        FileEditor selectedEditor = editorManager.getSelectedEditor();
        if (selectedEditor == null) return null;

        if (selectedEditor instanceof SQLConsoleEditor) {
            SQLConsoleEditor sqlConsoleEditor = (SQLConsoleEditor) selectedEditor;
            VirtualFile consoleFile = sqlConsoleEditor.getFile();
            if (consoleFile == null) return null;

            return consoleFile.getName();
        }

        return null;
    }

    @Override
    public String loadSqlConsoleContent(String consoleName) {
        DBConsole console = getConnection().getConsoleBundle().getConsole(consoleName);
        verify(console, DBObjectType.CONSOLE, consoleName);

        CharSequence consoleContent = console.getVirtualFile().getContent().getText();
        return consoleContent.toString();
    }

    public void updateSqlConsoleContent(String consoleName, String newContent) {
        DBConsole console = getConnection().getConsoleBundle().getConsole(consoleName);
        verify(console, DBObjectType.CONSOLE, consoleName);

        console.getVirtualFile().updateContent(newContent);
    }

    @Override
    public void openSqlConsole(String consoleName) {
        DBConsole console = getConnection().getConsoleBundle().getConsole(consoleName);
        verify(console, DBObjectType.CONSOLE, consoleName);
        openEditor(console);
    }

    @Override
    public String openNewSqlConsole(String consoleContent) {
        ConnectionHandler connection = getConnection();

        Set<String> consoleNames = connection.getConsoleBundle().getConsoleNames();
        String baseName = connection.getName() + " 1";
        String consoleName = nextNumberedIdentifier(baseName, true, () -> consoleNames);

        DatabaseConsoleManager consoleManager = DatabaseConsoleManager.getInstance(getProject());
        consoleManager.createConsole(connection, consoleName, consoleContent, DBConsoleType.STANDARD);
        return consoleName;
    }
}

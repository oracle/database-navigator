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

package com.dbn.execution.compiler.ui;

import com.dbn.common.icon.Icons;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.execution.compiler.CompileType;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;

import static com.dbn.nls.NlsResources.txt;

@Getter
public class CompilerTypeSelectionDialog extends DBNDialog<CompilerTypeSelectionForm> {
    private CompileType selection;
    private DBObjectRef<DBSchemaObject> object;

    public CompilerTypeSelectionDialog(Project project, @Nullable DBSchemaObject object) {
        super(project, txt("msg.compiler.title.CompileType"), true);
        setModal(true);
        setResizable(false);
        this.object = DBObjectRef.of(object);
        //setVerticalStretch(0);
        init();
    }

    @NotNull
    @Override
    protected CompilerTypeSelectionForm createForm() {
        DBSchemaObject object = DBObjectRef.get(this.object);
        return new CompilerTypeSelectionForm(this, object);
    }

    @Override
    @NotNull
    protected final Action[] initializeActions() {
        return actions(
                new CompileKeep(),
                new CompileNormalAction(),
                new CompileDebugAction(),
                getCancelAction());
    }

    private class CompileKeep extends AbstractAction {
        private CompileKeep() {
            super(txt("msg.compiler.button.KeepCurrent"));
            makeDefaultAction(this);;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            selection = CompileType.KEEP;
            doOKAction();
        }
    }

    private class CompileNormalAction extends AbstractAction {
        private CompileNormalAction() {
            super(txt("msg.compiler.button.Normal"), Icons.OBJECT_COMPILE);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            selection = CompileType.NORMAL;
            doOKAction();
        }
    }

    private class CompileDebugAction extends AbstractAction {
        private CompileDebugAction() {
            super(txt("msg.shared.button.Debug"), Icons.OBJECT_COMPILE_DEBUG);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            selection = CompileType.DEBUG;
            doOKAction();
        }
    }
}

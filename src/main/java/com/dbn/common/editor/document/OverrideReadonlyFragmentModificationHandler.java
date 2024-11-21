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

package com.dbn.common.editor.document;

import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.Messages;
import com.dbn.vfs.file.DBConsoleVirtualFile;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.ReadOnlyFragmentModificationException;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.actionSystem.ReadonlyFragmentModificationHandler;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;

import static com.dbn.common.action.UserDataKeys.GUARDED_BLOCK_REASON;
import static com.dbn.nls.NlsResources.txt;

public class OverrideReadonlyFragmentModificationHandler implements
        ReadonlyFragmentModificationHandler {

    private static final ReadonlyFragmentModificationHandler originalHandler = EditorActionManager.getInstance().getReadonlyFragmentModificationHandler();
    public static final ReadonlyFragmentModificationHandler INSTANCE = new OverrideReadonlyFragmentModificationHandler();
    private OverrideReadonlyFragmentModificationHandler() {

    }

    @Override
    public void handle(final ReadOnlyFragmentModificationException e) {
        RangeMarker guardedBlock = e.getGuardedBlock();
        if (guardedBlock == null) return;

        Document document = guardedBlock.getDocument();
        String message = document.getUserData(GUARDED_BLOCK_REASON);
        if (message != null) {
            Messages.showErrorDialog(null, txt("msg.codeEditor.title.ActionDenied"), message);
        } else {
            VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
            if (virtualFile instanceof DBSourceCodeVirtualFile || virtualFile instanceof LightVirtualFile || virtualFile instanceof DBConsoleVirtualFile) {
                //Messages.showErrorDialog("You're not allowed to change name and type of the edited component.", "Action denied");
            } else {
                Dispatch.run(() -> originalHandler.handle(e));
            }
        }
    }
}

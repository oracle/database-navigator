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

package com.dbn.navigation.psi;

import com.dbn.common.dispose.Failsafe;
import com.dbn.connection.DatabaseEntity;
import com.dbn.editor.DatabaseFileEditorManager;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.common.property.DBObjectProperty;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.vfs.DBVirtualFile;
import com.dbn.vfs.DatabaseFileViewProvider;
import com.intellij.lang.FileASTNode;
import com.intellij.lang.Language;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import static com.dbn.common.dispose.Failsafe.guarded;

public class DBObjectPsiFile extends UserDataHolderBase implements PsiFile, Disposable, ReadonlyPsiElementStub {
    private final DBObjectRef<?> objectRef;

    public DBObjectPsiFile(DBObjectRef<?> objectRef) {
        this.objectRef = objectRef;
    }

    @NotNull
    public DBObject getObject() {
        return Failsafe.nn(objectRef.get());
    }

    /*********************************************************
     *                      PsiElement                       *
     *********************************************************/
    @Override
    @NotNull
    public String getName() {
        return objectRef.getObjectName();
    }

    @Override
    public ItemPresentation getPresentation() {
        return getObject();
    }

    @Override
    @NotNull
    public Project getProject() throws PsiInvalidElementAccessException {
        return getObject().getProject();
    }

    @Override
    @NotNull
    public Language getLanguage() {
        return Language.ANY;
    }

    @Override
    public PsiDirectory getParent() {
        return guarded(null, this, f -> f.resolveParent());
    }

    private @Nullable PsiDirectory resolveParent() {
        DBObject object = getObject();
        DatabaseEntity parent = object.getParent();
        if (parent instanceof DBObjectList) {
            DBObjectList<?> objectList = (DBObjectList<?>) parent;
            return objectList.getPsiDirectory();
        }
        return null;
    }

    @Override
    public FileASTNode getNode() {
        return null;
    }

    @Override
    public void navigate(boolean requestFocus) {
        guarded(this, f -> f.navigateToObject(requestFocus));;
    }

    private void navigateToObject(boolean requestFocus) {
        DBObject object = getObject();
        if (object.is(DBObjectProperty.EDITABLE)) {
            DatabaseFileEditorManager editorManager = DatabaseFileEditorManager.getInstance(getProject());
            editorManager.connectAndOpenEditor(object, null, false, requestFocus);
        } else {
            object.navigate(requestFocus);
        }
    }

    @Override
    public PsiFile getContainingFile() throws PsiInvalidElementAccessException {
        return this;
    }

    @Override
    public Icon getIcon(int flags) {
        return getObject().getIcon();
    }

    /*********************************************************
     *                        PsiFile                        *
     *********************************************************/
    @Override
    @NotNull
    public VirtualFile getVirtualFile() {
        return getObject().getVirtualFile();
    }

    @Override
    public boolean processChildren(@NotNull PsiElementProcessor processor) {
        return false;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public PsiDirectory getContainingDirectory() {
        return getParent();
    }

    @Override
    public long getModificationStamp() {
        return 0;
    }

    @Override
    @NotNull
    public PsiFile getOriginalFile() {
        return this;
    }

    @Override
    @NotNull
    public FileType getFileType() {
        return UnknownFileType.INSTANCE;
    }

    @Override
    @NotNull
    public PsiFile[] getPsiRoots() {
        return new PsiFile[] {this};
    }

    @Override
    @NotNull
    public FileViewProvider getViewProvider() {
        DBVirtualFile virtualFile = (DBVirtualFile) getVirtualFile();
        DatabaseFileViewProvider viewProvider = virtualFile.getCachedViewProvider();
        if (viewProvider == null) {
            viewProvider = new DatabaseFileViewProvider(getProject(), getVirtualFile(), true);
        }
        return viewProvider;
    }

    @Override
    public void subtreeChanged() {

    }

    @Override
    public void checkSetName(String name) throws IncorrectOperationException {

    }
}

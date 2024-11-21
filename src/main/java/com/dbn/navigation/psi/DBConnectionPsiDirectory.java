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

import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.Failsafe;
import com.dbn.connection.ConnectionHandler;
import com.dbn.object.common.list.DBObjectListContainer;
import com.dbn.vfs.file.DBConnectionVirtualFile;
import com.intellij.lang.Language;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiInvalidElementAccessException;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.List;

public class DBConnectionPsiDirectory implements ReadonlyPsiDirectoryStub {
    private DBConnectionVirtualFile virtualFile;

    public DBConnectionPsiDirectory(ConnectionHandler connection) {
        virtualFile = new DBConnectionVirtualFile(connection);
    }

    @Override
    @NotNull
    public DBConnectionVirtualFile getVirtualFile() {
        return Failsafe.nn(virtualFile);
    }

    @NotNull
    public ConnectionHandler getConnection() {
        return getVirtualFile().getConnection();
    }

    @Override
    @NotNull
    public String getName() {
        return getConnection().getName();
    }

    @Override
    public ItemPresentation getPresentation() {
        return getConnection().getObjectBundle();
    }

    @Override
    public void dispose() {
        Disposer.dispose(virtualFile);
        virtualFile = null;
    }

    @Override
    @NotNull
    public Project getProject() throws PsiInvalidElementAccessException {
        return Failsafe.nn(getVirtualFile().getProject());
    }

    @Override
    @NotNull
    public Language getLanguage() {
        return Language.ANY;
    }

    @Override
    @NotNull
    public PsiElement[] getChildren() {
        List<PsiElement> children = new ArrayList<>();
        DBObjectListContainer objectLists = virtualFile.getConnection().getObjectBundle().getObjectLists();
        objectLists.visit(o -> children.add(o.getPsiDirectory()), false);
        return children.toArray(new PsiElement[0]);
    }

    @Override
    public PsiDirectory getParent() {
        return null;
    }

    @Override
    public void navigate(boolean requestFocus) {
        getConnection().getObjectBundle().navigate(requestFocus);
    }

    @Override
    public Icon getIcon(int flags) {
        return getVirtualFile().getIcon();
    }
}

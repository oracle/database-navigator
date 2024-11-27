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

package com.dbn.object.common;

import com.dbn.connection.ConnectionHandler;
import com.dbn.database.common.metadata.DBObjectMetadata;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class DBFictiveObject extends DBObjectImpl implements PsiReference {
    private String name;
    public DBFictiveObject(DBObjectType objectType, String name) {
        super(null, objectType, name);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean isVirtual() {
        return true;
    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, DBObjectMetadata metadata) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getQualifiedNameWithType() {
        return getName();
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return ref().getObjectType();
    }


    @Override
    public void navigate(boolean requestFocus) {

    }

    /*********************************************************
     *                       PsiReference                    *
     *********************************************************/
    @Override
    @NotNull
    public PsiElement getElement() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public TextRange getRangeInElement() {
        return new TextRange(0, name.length());
    }

    @Override
    public PsiElement resolve() {
        return null;
    }

    @Override
    @NotNull
    public String getCanonicalText() {
        return name;
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
        return null;
    }

    @Override
    public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
        return null;
    }

    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        return false;
    }

    @Override
    @NotNull
    public Object[] getVariants() {
        return new Object[0];
    }

    @Override
    public boolean isSoft() {
        return false;
    }

}

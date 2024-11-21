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

import com.dbn.common.compatibility.Compatibility;
import com.dbn.language.common.psi.EmptySearchScope;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface NamedPsiElementStub extends PsiNamedElement, Navigatable, Disposable {


    char[] EMPTY_CHAR_ARRAY = new char[0];

    @Override
    default boolean canNavigate() {
        return true;
    }

    @Override
    default boolean canNavigateToSource() {
        return false;
    }

    @Override
    @NotNull
    default GlobalSearchScope getResolveScope() {
        return EmptySearchScope.INSTANCE;
    }

    @Override
    @NotNull
    default SearchScope getUseScope() {
        return EmptySearchScope.INSTANCE;
    }

    @Override
    default boolean isEquivalentTo(PsiElement another) {
        return false;
    }

    @Override
    default PsiManager getManager() {
        return PsiManager.getInstance(getProject());
    }

    @Override
    default ASTNode getNode() {
        return null;
    }

    @Override
    default TextRange getTextRange() {
        return TextRange.EMPTY_RANGE;
    }

    @Override
    default int getTextOffset() {
        return 0;
    }

    @Override
    default String getText() {
        return "";
    }

    @Override
    default int getTextLength() {
        return 0;
    }

    @Override
    default int getStartOffsetInParent() {
        return 0;
    }

    @Override
    default boolean textMatches(@NotNull CharSequence text) {
        return false;
    }

    @Override
    default boolean textMatches(@NotNull PsiElement element) {
        return false;
    }

    @Override
    default boolean textContains(char c) {
        return false;
    }

    @Override
    default @NotNull char[] textToCharArray() {
        return EMPTY_CHAR_ARRAY;
    }

    @Override
    default boolean isPhysical() {
        return true;
    }

    @Override
    default PsiElement getContext() {
        return null;
    }


    @Override
    default PsiElement getNavigationElement() {
        return this;
    }

    @Override
    default PsiElement getOriginalElement() {
        return this;
    }

    @Override
    default PsiElement getFirstChild() {
        return null;
    }

    @Override
    default PsiElement getLastChild() {
        return null;
    }

    @Override
    default PsiElement getNextSibling() {
        return null;
    }

    @Override
    default PsiElement getPrevSibling() {
        return null;
    }

    @Override
    default PsiElement findElementAt(int offset) {
        return null;
    }

    @Override
    default PsiReference findReferenceAt(int offset) {
        return null;
    }

    @Override
    default void accept(@NotNull PsiElementVisitor visitor) {

    }

    @Override
    default void acceptChildren(@NotNull PsiElementVisitor visitor) {

    }

    @Override
    default PsiElement copy() {
        return null;
    }


    @Override
    default boolean isValid() {
        return true;
    }

    @Override
    default boolean isWritable() {
        return false;
    }

    @Override
    default PsiReference getReference() {
        return null;
    }

    @Override
    @NotNull
    default PsiReference[] getReferences() {
        return PsiReference.EMPTY_ARRAY;
    }

    @Override
    @NotNull
    default PsiElement[] getChildren() {
        return PsiElement.EMPTY_ARRAY;
    }

    @Override
    default  <T> T getUserData(@NotNull Key<T> key) {return null;}

    @Override
    default  <T> void putUserData(@NotNull Key<T> key, T value) {}

    @Override
    default  <T> T getCopyableUserData(@NotNull Key<T> key) {
        return null;
    }

    @Override
    default  <T> void putCopyableUserData(@NotNull Key<T> key, T value) {}

    @Override
    default boolean processDeclarations(@NotNull PsiScopeProcessor processor, @NotNull ResolveState state, @Nullable PsiElement lastParent, @NotNull PsiElement place) {
        return false;
    }

    /**
     * TODO check if still required after IDE 20.x
     */
    @Compatibility
    default FileStatus getFileStatus() {
        return FileStatus.NOT_CHANGED;
    }

    @Override
    default void dispose() {}

}

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

package com.dbn.diagnostics.data;

import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.TokenType;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@UtilityClass
public final class ParserDiagnosticsUtil {

    @NotNull
    public static StateTransition computeStateTransition(
            @Nullable IssueCounter oldIssues,
            @Nullable IssueCounter newIssues) {

        int oldCount = oldIssues == null ? 0 : oldIssues.issueCount();
        int newCount = newIssues == null ? 0 : newIssues.issueCount();
        if (newCount == 0) {
            return StateTransition.FIXED;
        }

        if (oldCount == 0) {
            return StateTransition.BROKEN;
        }

        if (newCount > oldCount) {
            return StateTransition.DEGRADED;
        }

        if (newCount < oldCount) {
            return StateTransition.IMPROVED;
        }

        return StateTransition.UNCHANGED;
    }

    /** Returns as soon as the file contains one parser error or warning. */
    public static boolean hasIssues(PsiFile file) {
        Deque<PsiElement> elements = new ArrayDeque<>();
        elements.push(file);
        while (!elements.isEmpty()) {
            PsiElement element = elements.pop();
            if (element instanceof PsiWhiteSpace) continue;
            if (element instanceof PsiComment) continue;
            if (element instanceof PsiErrorElement) return true;

            if (isWarningToken(element)) return true;
            if (element instanceof LeafPsiElement) continue;

            for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
                elements.push(child);
            }
        }
        return false;
    }

    public static int countErrors(PsiFile file) {
        Set<Integer> errorOffsets = new HashSet<>();
        PsiRecursiveElementVisitor visitor = new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (element instanceof PsiErrorElement) {
                    errorOffsets.add(element.getTextOffset());
                }
                super.visitElement(element);
            }
        };
        visitor.visitFile(file);
        return errorOffsets.size();
    }

    /** Returns the same combined issue count used by parser diagnostics results. */
    public static int countIssues(PsiFile file) {
        return countErrors(file) + countWarnings(file);
    }

    public static int countWarnings(PsiFile file) {
        AtomicInteger count = new AtomicInteger();
        PsiRecursiveElementVisitor visitor = new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (element instanceof PsiWhiteSpace) return;
                if (element instanceof PsiComment) return;

                if (isWarningToken(element)) count.incrementAndGet();
                if (!(element instanceof LeafPsiElement)) super.visitElement(element);
            }
        };
        visitor.visitFile(file);
        return count.get();
    }

    private static boolean isWarningToken(PsiElement element) {
        if (!(element instanceof LeafPsiElement leafPsiElement)) return false;
        if (!(element.getParent() instanceof DBLanguagePsiFile)) return false;

        IElementType elementType = leafPsiElement.getElementType();
        return elementType instanceof TokenType tokenType &&
                !tokenType.isCharacter() && !tokenType.isChameleon();
    }
}

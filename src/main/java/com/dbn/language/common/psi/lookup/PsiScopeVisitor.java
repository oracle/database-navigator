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

package com.dbn.language.common.psi.lookup;

import com.dbn.common.lookup.Visitor;
import com.dbn.language.common.psi.BasePsiElement;
import com.intellij.psi.PsiElement;

import java.util.function.Predicate;

public abstract class PsiScopeVisitor implements Visitor<BasePsiElement> {
    private PsiScopeVisitor() {}

    public static void visit(BasePsiElement element, Predicate<BasePsiElement> visitor) {
        new PsiScopeVisitor() {
            @Override
            protected boolean visitScope(BasePsiElement scope) {
                return visitor.test(scope);
            }
        }.visit(element);
    }

    public final void visit(BasePsiElement element) {
        BasePsiElement scope = element.getEnclosingScopeElement();
        while (scope != null) {
            boolean breakTreeWalk = visitScope(scope);
            if (breakTreeWalk || scope.elementType.scopeIsolation) break;

            // LOOKUP
            PsiElement parent = scope.getParent();
            if (parent instanceof BasePsiElement) {
                BasePsiElement basePsiElement = (BasePsiElement) parent;
                scope = basePsiElement.getEnclosingScopeElement();

            } else {
                scope = null;
            }
        }
    }

    protected abstract boolean visitScope(BasePsiElement scope);
}

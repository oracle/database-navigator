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

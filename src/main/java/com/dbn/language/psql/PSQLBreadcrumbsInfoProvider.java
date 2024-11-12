package com.dbn.language.psql;

import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.common.psi.IdentifierPsiElement;
import com.dbn.language.common.psi.NamedPsiElement;
import com.dbn.object.type.DBObjectType;
import com.intellij.lang.Language;
import com.intellij.psi.PsiElement;
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Collections;
import java.util.List;

public class PSQLBreadcrumbsInfoProvider implements BreadcrumbsProvider {

    private static final Language[] LANGUAGES = {PSQLLanguage.INSTANCE};

    @Override
    public Language[] getLanguages() {
        return LANGUAGES;
    }

    @Override
    public boolean acceptElement(@NotNull PsiElement psiElement) {
        IdentifierPsiElement identifierPsiElement = getBreadcrumbIdentifier(psiElement);
        return identifierPsiElement != null;
    }

    @NotNull
    @Override
    public String getElementInfo(@NotNull PsiElement psiElement) {
        IdentifierPsiElement identifierPsiElement = getBreadcrumbIdentifier(psiElement);

        return identifierPsiElement != null ? identifierPsiElement.getText() : "";
    }

    @Nullable
    @Override
    public Icon getElementIcon(@NotNull PsiElement psiElement) {
        IdentifierPsiElement identifierPsiElement = getBreadcrumbIdentifier(psiElement);
        if (identifierPsiElement != null) {
            return identifierPsiElement.getIcon(false);
        }
        return null;
    }

    @Nullable
    @Override
    public String getElementTooltip(@NotNull PsiElement element) {
        if (element instanceof BasePsiElement) {
            BasePsiElement basePsiElement = (BasePsiElement) element;
            return basePsiElement.elementType.getDescription();
        }
        return null;
    }

    @Nullable
    @Override
    public PsiElement getParent(@NotNull PsiElement element) {
        PsiElement parent = element.getParent();
        if (parent instanceof BasePsiElement) {
            BasePsiElement basePsiElement = (BasePsiElement) parent;
            return basePsiElement.getEnclosingScopeElement();
        }
        return null;
    }

    @NotNull
    @Override
    public List<PsiElement> getChildren(@NotNull PsiElement element) {
        return Collections.emptyList();
    }

    @Nullable
    private IdentifierPsiElement getBreadcrumbIdentifier(@NotNull PsiElement psiElement) {
        if (psiElement instanceof NamedPsiElement) {
            NamedPsiElement namedPsiElement = (NamedPsiElement) psiElement;
            boolean isObject =
                    namedPsiElement.is(ElementTypeAttribute.OBJECT_DEFINITION) ||
                    namedPsiElement.is(ElementTypeAttribute.OBJECT_DECLARATION) ||
                    namedPsiElement.is(ElementTypeAttribute.OBJECT_SPECIFICATION);

            if (isObject) {
                BasePsiElement subject = namedPsiElement.findFirstPsiElement(ElementTypeAttribute.SUBJECT);
                if (subject instanceof IdentifierPsiElement) {
                    IdentifierPsiElement identifierPsiElement = (IdentifierPsiElement) subject;
                    DBObjectType objectType = identifierPsiElement.getObjectType();
                    if (objectType.matchesOneOf(
                            DBObjectType.METHOD,
                            DBObjectType.PROGRAM,
                            DBObjectType.SYNONYM,
                            DBObjectType.TYPE,
                            DBObjectType.CURSOR,
                            DBObjectType.TRIGGER)) {
                        return identifierPsiElement;
                    }
                }
            }
        }
        return null;
    }
}

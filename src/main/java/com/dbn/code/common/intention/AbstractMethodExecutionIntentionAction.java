package com.dbn.code.common.intention;

import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.element.util.IdentifierCategory;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.common.psi.IdentifierPsiElement;
import com.dbn.language.common.psi.PsiUtil;
import com.dbn.language.common.psi.lookup.ObjectLookupAdapter;
import com.dbn.object.DBMethod;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.util.Commons.nvln;

public abstract class AbstractMethodExecutionIntentionAction extends EditorIntentionAction {
    private DBObjectRef<DBMethod> lastChecked;
    public static final ObjectLookupAdapter METHOD_LOOKUP_ADAPTER = new ObjectLookupAdapter(null, IdentifierCategory.DEFINITION, DBObjectType.METHOD);

    @Override
    @NotNull
    public final String getText() {
        DBMethod method = getMethod();
        if (method != null) {
            DBObjectType objectType = method.getObjectType();
            if (objectType.matches(DBObjectType.PROCEDURE)) objectType = DBObjectType.PROCEDURE;
            if (objectType.matches(DBObjectType.FUNCTION)) objectType = DBObjectType.FUNCTION;
            return getActionName() + ' ' + objectType.getName() + ' ' + method.getName();
        }
        return getActionName();
    }

    protected abstract String getActionName();

    @Nullable
    protected DBMethod resolveMethod(Editor editor, PsiFile psiFile) {
        if (psiFile instanceof DBLanguagePsiFile) {
            DBLanguagePsiFile dbLanguagePsiFile = (DBLanguagePsiFile) psiFile;
            DBObject underlyingObject = dbLanguagePsiFile.getUnderlyingObject();

            if (underlyingObject != null) {
                if (underlyingObject instanceof DBMethod) {
                    DBMethod method = (DBMethod) underlyingObject;
                    lastChecked = DBObjectRef.of(method);
                    return method;
                }

                if (underlyingObject.getObjectType().isParentOf(DBObjectType.METHOD) && editor != null) {
                    BasePsiElement psiElement = PsiUtil.lookupLeafAtOffset(psiFile, editor.getCaretModel().getOffset());
                    if (psiElement != null) {
                        BasePsiElement methodPsiElement = null;
                        if (psiElement instanceof IdentifierPsiElement) {
                            IdentifierPsiElement identifierPsiElement = (IdentifierPsiElement) psiElement;
                            DBObjectType objectType = identifierPsiElement.getObjectType();
                            if (identifierPsiElement.isDefinition() && objectType.getGenericType() == DBObjectType.METHOD) {
                                methodPsiElement = identifierPsiElement;
                            }
                        }

                        methodPsiElement = nvln(methodPsiElement, () -> METHOD_LOOKUP_ADAPTER.findInParentScopeOf(psiElement));
                        if (methodPsiElement instanceof IdentifierPsiElement) {
                            IdentifierPsiElement identifierPsiElement = (IdentifierPsiElement) methodPsiElement;
                            DBObject object = identifierPsiElement.getUnderlyingObject();
                            if (object instanceof DBMethod) {
                                DBMethod method = (DBMethod) object;
                                lastChecked = DBObjectRef.of(method);
                                return method;
                            }

                        }
                    }
                }
            }
        }
        lastChecked = null;
        return null;
    }

    @Nullable
    protected DBMethod getMethod() {
        return lastChecked == null ? null : lastChecked.get();
    }

}

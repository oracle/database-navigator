package com.dbn.language.common.psi;

import com.dbn.code.common.style.formatting.FormattingAttributes;
import com.dbn.common.util.Strings;
import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.impl.BlockElementType;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.IterationElementType;
import com.dbn.language.common.element.impl.NamedElementType;
import com.dbn.language.common.element.impl.OneOfElementType;
import com.dbn.language.common.element.impl.SequenceElementType;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.psi.lookup.PsiLookupAdapter;
import com.dbn.object.type.DBObjectType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

public class SequencePsiElement<T extends ElementTypeBase> extends BasePsiElement<T> {
    public SequencePsiElement(ASTNode astNode, T elementType) {
        super(astNode, elementType);
    }

    @Override
    public FormattingAttributes getFormattingAttributes() {
        return super.getFormattingAttributes();
    }

    @Override
    public int approximateLength() {
        int length = 0;
        PsiElement child = getFirstChild();
        while (child != null) {
            if (child instanceof BasePsiElement) {
                BasePsiElement basePsiElement = (BasePsiElement) child;
                length = length + basePsiElement.approximateLength();
            }
            child = child.getNextSibling();
        }
        return length;
    }

    /*********************************************************
     *                   Lookup routines                     *
     *********************************************************/

    @Override
    @Nullable
    public BasePsiElement findPsiElement(
            PsiLookupAdapter lookupAdapter,
            int scopeCrossCount) {

        PsiElement child = getFirstChild();
        while (child != null) {
            if (child instanceof BasePsiElement) {
                BasePsiElement basePsiElement = (BasePsiElement) child;
                if (lookupAdapter.accepts(basePsiElement)) {
                    boolean isScopeBoundary = basePsiElement.isScopeBoundary();
                    if (!isScopeBoundary || scopeCrossCount > 0) {
                        int childScopeCrossCount = isScopeBoundary ? scopeCrossCount-1 : scopeCrossCount;
                        BasePsiElement result = basePsiElement.findPsiElement(lookupAdapter, childScopeCrossCount);
                        if (result != null) return result;
                    }
                }
            }
            child = child.getNextSibling();
        }
        return null;
    }

    @Override
    public void collectPsiElements(
            PsiLookupAdapter lookupAdapter,
            int scopeCrossCount,
            @NotNull Consumer<BasePsiElement> consumer) {

        if (lookupAdapter.matches(this)) {
            consumer.accept(this);

        }
        PsiElement child = getFirstChild();
        while (child != null) {
            if (child instanceof BasePsiElement) {
                BasePsiElement basePsiElement = (BasePsiElement) child;

                if (lookupAdapter.accepts(basePsiElement)) {
                    boolean isScopeBoundary = basePsiElement.isScopeBoundary();
                    if (!isScopeBoundary || scopeCrossCount > 0) {
                        int childScopeCrossCount = isScopeBoundary ? scopeCrossCount-1 : scopeCrossCount;
                        basePsiElement.collectPsiElements(lookupAdapter, childScopeCrossCount, consumer);
                    }
                }
            }
            child = child.getNextSibling();
        }
    }

    @Override
    public void collectExecVariablePsiElements(@NotNull Consumer<ExecVariablePsiElement> consumer) {
        PsiElement child = getFirstChild();
        while (child != null) {
            if (child instanceof BasePsiElement) {
                BasePsiElement basePsiElement = (BasePsiElement) child;
                basePsiElement.collectExecVariablePsiElements(consumer);
            }
            child = child.getNextSibling();
        }
    }

    @Override
    public void collectSubjectPsiElements(@NotNull Consumer<IdentifierPsiElement> consumer) {
        PsiElement child = getFirstChild();
        while (child != null) {
            if (child instanceof BasePsiElement) {
                BasePsiElement basePsiElement = (BasePsiElement) child;
                basePsiElement.collectSubjectPsiElements(consumer);
            }
            child = child.getNextSibling();
        }
    }

    @Override
    public void collectVirtualObjectPsiElements(DBObjectType objectType, Consumer<BasePsiElement> consumer) {
        //if (getElementType().getLookupCache().containsVirtualObject(objectType)) {
            if (elementType.isVirtualObject()) {
                DBObjectType virtualObjectType = elementType.virtualObjectType;
                if (objectType == virtualObjectType) {
                    consumer.accept(this);
                }
            }
            PsiElement child = getFirstChild();
            while (child != null) {
                if (child instanceof BasePsiElement) {
                    BasePsiElement basePsiElement = (BasePsiElement) child;
                    basePsiElement.collectVirtualObjectPsiElements(objectType, consumer);
                }
                child = child.getNextSibling();
            }
        //}
    }

    @Override
    public NamedPsiElement findNamedPsiElement(String id) {
        PsiElement child = getFirstChild();
        while (child != null) {
            if (child instanceof SequencePsiElement) {
                SequencePsiElement bundlePsiElement = (SequencePsiElement) child;
                if (bundlePsiElement instanceof NamedPsiElement) {
                    NamedPsiElement namedPsiElement = (NamedPsiElement) bundlePsiElement;
                    if (Objects.equals(namedPsiElement.elementType.getId(), id)) {
                        return namedPsiElement;
                    }
                }

                NamedPsiElement namedPsiElement = bundlePsiElement.findNamedPsiElement(id);
                if (namedPsiElement != null) {
                    return namedPsiElement;
                }
            }
            child = child.getNextSibling();
        }
        return null;
    }

    @Override
    public BasePsiElement findFirstPsiElement(ElementTypeAttribute attribute) {
        if (elementType.is(attribute)) {
            return this;
        }

        PsiElement child = getFirstChild();
        while (child != null) {
            if (child instanceof BasePsiElement) {
                BasePsiElement basePsiElement = (BasePsiElement) child;
                BasePsiElement firstElement = basePsiElement.findFirstPsiElement(attribute);
                if (firstElement != null) {
                    return firstElement;
                }
            }
            child = child.getNextSibling();
        }
        return null;
    }

    @Override
    public BasePsiElement findFirstPsiElement(Class<? extends ElementType> clazz) {
        if (clazz.isAssignableFrom(elementType.getClass())) {
            return this;
        }

        PsiElement child = getFirstChild();
        while (child != null) {
            if (child instanceof BasePsiElement) {
                BasePsiElement basePsiElement = (BasePsiElement) child;
                BasePsiElement firstElement = basePsiElement.findFirstPsiElement(clazz);
                if (firstElement != null) {
                    return firstElement;
                }
            }
            child = child.getNextSibling();
        }
        return null;
    }

    @Override
    public BasePsiElement findFirstLeafPsiElement() {
        PsiElement firstChild = getFirstChild();
        while (firstChild != null) {
            if (firstChild instanceof BasePsiElement) {
                BasePsiElement basePsiElement = (BasePsiElement) firstChild;
                return basePsiElement.findFirstLeafPsiElement();
            }
            firstChild = firstChild.getNextSibling();
        }
        return null;
    }

    @Override
    public BasePsiElement findPsiElementBySubject(ElementTypeAttribute attribute, CharSequence subjectName, DBObjectType subjectType) {
        if (elementType.is(attribute)) {
            BasePsiElement subjectPsiElement = findFirstPsiElement(ElementTypeAttribute.SUBJECT);
            if (subjectPsiElement instanceof IdentifierPsiElement) {
                IdentifierPsiElement identifierPsiElement = (IdentifierPsiElement) subjectPsiElement;
                if (identifierPsiElement.getObjectType() == subjectType &&
                        Strings.equalsIgnoreCase(subjectName, identifierPsiElement.getChars())) {
                    return this;
                }
            }
        }
        PsiElement child = getFirstChild();
        while (child != null) {
            if (child instanceof BasePsiElement) {
                BasePsiElement basePsiElement = (BasePsiElement) child;
                BasePsiElement childPsiElement = basePsiElement.findPsiElementBySubject(attribute, subjectName, subjectType);
                if (childPsiElement != null) {
                    return childPsiElement;
                }
            }
            child = child.getNextSibling();
        }
        return null;
    }

    @Override
    public BasePsiElement findPsiElementByAttribute(ElementTypeAttribute attribute) {
        if (elementType.is(attribute)) {
            return this;
        }
        PsiElement child = getFirstChild();
        while (child != null) {
            if (child instanceof BasePsiElement) {
                BasePsiElement basePsiElement = (BasePsiElement) child;
                BasePsiElement childPsiElement = basePsiElement.findPsiElementByAttribute(attribute);
                if (childPsiElement != null) {
                    return childPsiElement;
                }
            }
            child = child.getNextSibling();

        }
        return null;
    }

    @Override
    public boolean containsPsiElement(BasePsiElement childPsiElement) {
        if (this == childPsiElement) {
            return true;
        }

        PsiElement child = getFirstChild();
        while (child != null) {
            if (child instanceof BasePsiElement) {
                BasePsiElement basePsiElement = (BasePsiElement) child;
                boolean containsPsiElement = basePsiElement.containsPsiElement(childPsiElement);
                if (containsPsiElement) {
                    return true;
                }
            }
            child = child.getNextSibling();
        }
        return false;
    }

    /*********************************************************
     *                    Miscellaneous                      *
     *********************************************************/
     @Override
     public boolean hasErrors() {
         PsiElement child = getFirstChild();

         while (child != null) {
            if (child instanceof BasePsiElement) {
                BasePsiElement basePsiElement = (BasePsiElement) child;
                if (basePsiElement.hasErrors()) {
                    return true;
                }
            }
             child = child.getNextSibling();
         }

/*         if (true) return false;

         PsiElement[] psiElements = getChildren();
         if (getElementType() instanceof SequenceElementType) {
            int offset = 0;
            SequenceElementType sequenceElementType = (SequenceElementType) getElementType();
            ElementTypeRef[] children = sequenceElementType.getChildren();

            for (int i=0; i<children.length; i++) {
                while (offset < psiElements.length &&
                        (psiElements[offset] instanceof PsiWhiteSpace ||
                         psiElements[offset] instanceof PsiErrorElement)) offset++;

                PsiElement psiElement = offset == psiElements.length ? null : psiElements[offset];
                if (psiElement!= null && psiElement instanceof BasePsiElement && children[i].getElementType() == ((BasePsiElement)psiElement).getElementType()) {
                    offset++;
                    if (offset == psiElements.length) {
                        boolean isLast = i == children.length-1;
                        return !isLast && !sequenceElementType.isOptionalFromIndex(i+1);
                    }
                } else {
                    if (!children[i].isOptional() && !(psiElement instanceof PsiWhiteSpace) && !(psiElement instanceof PsiComment)) {
                        return true;
                    }
                }
            }
        } else if (getElementType() instanceof IterationElementType) {
            IterationElementType iterationElementType = (IterationElementType) getElementType();
            PsiElement psiElement = getLastChild();
            if (psiElement == null) {
                return true;
            } else if (psiElement instanceof BasePsiElement){
                BasePsiElement basePsiElement = (BasePsiElement) psiElement;
                return basePsiElement.getElementType() != iterationElementType.getIteratedElementType();
            } else {
                return psiElement instanceof PsiErrorElement;
            }
        }*/
        return false;
    }

    public boolean isSequence(){
        return elementType instanceof SequenceElementType;
    }

    public boolean isBlock(){
        return elementType instanceof BlockElementType;
    }

    public boolean isIteration(){
        return elementType instanceof IterationElementType;
    }

    public boolean isOneOf() {
        return elementType instanceof OneOfElementType;
    }

    public boolean isNamedSequence() {
        return elementType instanceof NamedElementType;
    }

    public boolean isFirstChild(PsiElement psiElement){
         return psiElement == getFirstChild();
    }

    @Override
    public boolean matches(@Nullable BasePsiElement basePsiElement, MatchType matchType) {
        PsiElement localChild = getFirstChild();
        PsiElement remoteChild = basePsiElement == null ? null : basePsiElement.getFirstChild();

        while(localChild != null && remoteChild != null) {
            if (localChild instanceof BasePsiElement && remoteChild instanceof BasePsiElement) {
                BasePsiElement localPsiElement = (BasePsiElement) localChild;
                BasePsiElement remotePsiElement = (BasePsiElement) remoteChild;
                if (!localPsiElement.matches(remotePsiElement, matchType)) {
                    return false;
                }
                localChild = PsiUtil.getNextSibling(localChild);
                remoteChild = PsiUtil.getNextSibling(remoteChild);
            } else {
                return false;
            }
        }
        return localChild == null && remoteChild == null;
    }


}

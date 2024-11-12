package com.dbn.language.common.element.path;

import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.SequenceElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.source.tree.FileElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;

public class AstNode implements LanguageNode {
    private final ASTNode astNode;

    public AstNode(ASTNode astNode) {
        this.astNode = astNode;
    }

    @Override
    public AstNode getParent() {
        ASTNode treeParent = astNode.getTreeParent();
        if (treeParent != null && !(treeParent instanceof FileElement)) {
            return new AstNode(treeParent);
        }
        return null;
    }

    @Override
    public int getIndexInParent() {
        ASTNode parentAstNode = astNode.getTreeParent();
        if (parentAstNode.getElementType() instanceof SequenceElementType) {
            SequenceElementType sequenceElementType = (SequenceElementType) parentAstNode.getElementType();
            int index = 0;
            ASTNode child = parentAstNode.getFirstChildNode();
            while (child != null) {
                if (astNode == child) {
                    break;
                }
                index++;
                child = child.getTreeNext();
                if (child instanceof PsiWhiteSpace){
                    child = child.getTreeNext();
                }
            }
            IElementType elementType = astNode.getElementType();
            if (elementType instanceof ElementType) {
                return sequenceElementType.indexOf((ElementType) elementType, index);
            }

        }
        return 0;
    }

    @Override
    @Nullable
    public ElementTypeBase getElement() {
        IElementType elementType = astNode.getElementType();

        return elementType instanceof ElementTypeBase ? (ElementTypeBase) elementType : null;
    }

    @Override
    public boolean isRecursive() {
        return false; 
    }

    @Override
    public boolean isAncestor(ElementTypeBase elementType) {
        return false;
    }

    @Override
    public void detach() {

    }
}

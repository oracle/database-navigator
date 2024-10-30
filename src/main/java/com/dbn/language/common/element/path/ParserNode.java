package com.dbn.language.common.element.path;

import com.dbn.language.common.element.impl.ElementTypeBase;
import com.intellij.lang.PsiBuilder;

public class ParserNode extends LanguageNodeBase {
    public final int startOffset;
    public int currentOffset;
    public int cursorPosition;
    public PsiBuilder.Marker elementMarker;

    public ParserNode(ElementTypeBase elementType, ParserNode parent, int startOffset, int cursorPosition) {
        super(elementType, parent);
        this.startOffset = startOffset;
        this.currentOffset = startOffset;
        this.cursorPosition = cursorPosition;
    }

    @Override
    public ParserNode getParent() {
        return (ParserNode) super.parent;
    }

    @Override
    public boolean isRecursive() {
        ParserNode parseNode = (ParserNode) this.parent;
        while (parseNode != null) {
            if (parseNode.element == this.element &&
                parseNode.startOffset == startOffset) {
                return true;
            }
            parseNode = (ParserNode) parseNode.parent;
        }
        return false;
    }

    public boolean isRecursive(int currentOffset) {
        ParserNode parseNode = (ParserNode) this.parent;
        while (parseNode != null) {
            if (parseNode.element == this.element &&
                parseNode.currentOffset == currentOffset) {
                    return true;
                }
            parseNode = (ParserNode) parseNode.parent;
        }
        return false;
    }

    public int incrementIndex(int builderOffset) {
        cursorPosition++;
        this.currentOffset = builderOffset;
        return cursorPosition;
    }

    @Override
    public void detach() {
        super.detach();
        elementMarker = null;
    }
}


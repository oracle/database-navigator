package com.dbn.language.common.element.impl;

import java.util.Objects;

public class WrappingDefinition {
    public final TokenElementType beginElementType;
    public final TokenElementType endElementType;

    public WrappingDefinition(TokenElementType beginElementType, TokenElementType endElementType) {
        this.beginElementType = beginElementType;
        this.endElementType = endElementType;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if (obj instanceof WrappingDefinition) {
            WrappingDefinition definition = (WrappingDefinition) obj;
            return
                Objects.equals(this.beginElementType.tokenType, definition.beginElementType.tokenType) &&
                Objects.equals(this.endElementType.tokenType, definition.endElementType.tokenType);
        }
        return false;
    }
}

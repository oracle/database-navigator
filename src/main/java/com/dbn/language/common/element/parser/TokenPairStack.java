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

package com.dbn.language.common.element.parser;

import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.SimpleTokenType;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.TokenTypeBundle;
import com.dbn.language.common.element.TokenPairTemplate;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.intellij.util.containers.Stack;

public class TokenPairStack {
    private int stackSize = 0;
    private final Stack<TokenPairMarker> markers = new Stack<>();
    private final ParserBuilder builder;

    private final SimpleTokenType beginToken;
    private final SimpleTokenType endToken;


    public TokenPairStack(ParserBuilder builder, DBLanguageDialect language, TokenPairTemplate template) {
        this.builder = builder;

        TokenTypeBundle parserTokenTypes = language.getParserTokenTypes();
        beginToken = parserTokenTypes.getTokenType(template.getBeginToken());
        endToken = parserTokenTypes.getTokenType(template.getEndToken());
    }

    /**
     * cleanup all markers registered after the builder offset (remained dirty after a marker rollback)
     */
    public void rollback(ElementTypeBase element) {
        int builderOffset = builder.getOffset();
        while (!markers.isEmpty()) {
            TokenPairMarker lastMarker = markers.peek();
            if (lastMarker.offset >= builderOffset) {
                markers.pop();
                if (stackSize > 0) stackSize--;
            } else {
                break;
            }
        }
    }

    public void acknowledge(ElementTypeBase element, TokenType token, boolean explicit) {
        if (token == beginToken) {
            stackSize++;
            TokenPairMarker marker = new TokenPairMarker(element, builder.getOffset(), explicit);
            markers.push(marker);
        } else if (token == endToken) {
            if (stackSize > 0) stackSize--;
            if (!markers.isEmpty()) {
                conclude(element);
            }
        }
    }

    public void conclude(ElementTypeBase element) {
        while(markers.size() > stackSize) {
            TokenPairMarker marker = markers.pop();
            if (marker.owner != element && element != null) {
                System.out.println();
            }
        }
    }

    public void reset() {
        stackSize = 0;
        markers.clear();
    }

    public boolean isExplicitRange() {
        if (markers.isEmpty()) return false;

        TokenPairMarker marker = markers.peek();
        return marker.explicit;

    }

    public void setExplicitRange(boolean value) {
        if (!markers.isEmpty()) {
            TokenPairMarker marker = markers.peek();
            marker.explicit = value;
        }
    }

    public static class TokenPairMarker {
        private final ElementTypeBase owner;
        private final int offset;
        private boolean explicit;

        public TokenPairMarker(ElementTypeBase owner, int offset, boolean explicit) {
            this.owner = owner;
            this.offset = offset;
            this.explicit = explicit;
        }

        @Override
        public String toString() {
            return offset + " " + explicit;
        }
    }
}

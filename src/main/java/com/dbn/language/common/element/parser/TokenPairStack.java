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
import com.dbn.language.common.element.impl.LeafElementType;
import com.intellij.util.containers.Stack;

import static com.dbn.language.common.element.util.ElementTypeAttribute.OPTIONAL_WRAPPING;
import static com.dbn.language.common.element.util.ElementTypeAttribute.SURROGATE_LEAD;

public class TokenPairStack extends ParserBuilderExtension {
    private final Stack<TokenPairMarker> markers = new Stack<>();

    public final SimpleTokenType beginToken;
    public final SimpleTokenType endToken;


    public TokenPairStack(ParserBuilder builder, DBLanguageDialect language, TokenPairTemplate template) {
        super(builder);

        TokenTypeBundle parserTokenTypes = language.getParserTokenTypes();
        beginToken = parserTokenTypes.getTokenType(template.getBeginToken());
        endToken = parserTokenTypes.getTokenType(template.getEndToken());
    }

    /**
     * cleanup all markers registered after the builder offset (remained dirty after a marker rollback)
     */
    public void rollback(ElementTypeBase element) {
        while (!markers.isEmpty()) {
            // strong owner match on rollbacks
            TokenPairMarker marker = markers.peek();
            if (marker.owner != element) return;

            markers.pop();
        }
    }

    public boolean acknowledge(LeafElementType leafElement, boolean borrowed) {
        if (leafElement.is(SURROGATE_LEAD)) return false;

        TokenType token = leafElement.tokenType;
        if (token == beginToken) return acknowledgeBegin(leafElement, borrowed);
        if (token == endToken) return acknowledgeEnd(leafElement, borrowed);
        return false;
    }

    private boolean acknowledgeBegin(LeafElementType leafElement, boolean borrowed) {
        boolean explicit = !leafElement.is(OPTIONAL_WRAPPING);
        TokenPairMarker marker = new TokenPairMarker(leafElement.parent, builder.getOffset(), explicit, borrowed);
        markers.push(marker);
        return true;
    }

    private boolean acknowledgeEnd(LeafElementType leafElement, boolean borrowed) {
        if (markers.isEmpty()) return false;

        ElementTypeBase parent = leafElement.parent;
        boolean explicit = !leafElement.is(OPTIONAL_WRAPPING);

        TokenPairMarker marker = markers.peek();
        if (explicit) {
            if (marker.explicit && marker.owner == parent) {
                markers.pop();
                if (marker.borrowed) {
                    markers.pop();
                }
                return true;
            }
        } else {
            if (!marker.explicit) {
                markers.pop();
                return true;
            }
        }

        return false;
    }

    public void reset() {
        markers.clear();
    }

    public boolean isExplicitRange() {
        if (markers.isEmpty()) return false;

        TokenPairMarker marker = markers.peek();
        return marker.explicit;

    }

    public static class TokenPairMarker {
        private final ElementTypeBase owner;
        private final int offset;
        private final boolean explicit;
        private final boolean borrowed;

        public TokenPairMarker(ElementTypeBase owner, int offset, boolean explicit, boolean borrowed) {
            this.owner = owner;
            this.offset = offset;
            this.explicit = explicit;
            this.borrowed = borrowed;
        }

        @Override
        public String toString() {
            return offset + ": " + owner + " - " + explicit;
        }
    }
}

/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.language.common.element.lookup;

import com.dbn.common.util.XmlContents;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.ElementTypeBundle;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.IterationElementType;
import com.dbn.language.common.element.impl.LeafElementType;
import com.dbn.language.common.element.impl.NamedElementType;
import com.dbn.language.common.element.impl.OneOfElementType;
import com.dbn.language.common.element.impl.QualifiedIdentifierElementType;
import com.dbn.language.common.element.impl.SequenceElementType;
import com.dbn.language.common.element.impl.WrapperElementType;
import com.dbn.language.common.element.path.ParserNode;
import com.dbn.language.psql.PSQLLanguage;
import org.jdom.Document;
import org.jetbrains.annotations.NonNls;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Active parser paths exercise token lookahead without constructing PSI or an application. */
public class NextTokenResolverTest {
    @Test
    public void skipsTheActiveChildAndStopsAtTheFirstRequiredSuccessor() throws Exception {
        NamedElementType root = grammar("""
                <token type-id="KW_BEGIN"/>
                <token type-id="KW_COMMIT" optional="true"/>
                <token type-id="KW_NULL"/>
                <token type-id="KW_END"/>
                """);
        ParserNode node = node(root, null, 0);
        LeafElementType source = (LeafElementType) child(root, 0);

        assertLookup(false, source, "KW_BEGIN", node);
        assertLookup(true, source, "KW_COMMIT", node);
        assertLookup(true, source, "KW_NULL", node);
        assertLookup(false, source, "KW_END", node);
        assertRequiredLookup(false, source, "KW_BEGIN", node);
        assertRequiredLookup(false, source, "KW_COMMIT", node);
        assertRequiredLookup(true, source, "KW_NULL", node);
        assertRequiredLookup(false, source, "KW_END", node);
        assertEquals(0, node.elementIndex);
    }

    @Test
    public void requiredLookupUsesNestedRequiredTokenIndexes() throws Exception {
        NamedElementType root = grammar("""
                <token type-id="KW_BEGIN"/>
                <sequence>
                    <token type-id="KW_COMMIT" optional="true"/>
                    <token type-id="KW_NULL"/>
                </sequence>
                <token type-id="KW_END"/>
                """);
        ParserNode node = node(root, null, 0);
        LeafElementType source = (LeafElementType) child(root, 0);

        assertLookup(true, source, "KW_COMMIT", node);
        assertRequiredLookup(false, source, "KW_COMMIT", node);
        assertRequiredLookup(true, source, "KW_NULL", node);
        assertRequiredLookup(false, source, "KW_END", node);
    }

    @Test
    public void optionalTailDoesNotPreventAnOuterRequiredContinuation() throws Exception {
        NamedElementType root = grammar("""
                <sequence>
                    <token type-id="KW_BEGIN"/>
                    <token type-id="KW_COMMIT" optional="true"/>
                </sequence>
                <token type-id="CHR_LEFT_PARENTHESIS"/>
                """);
        SequenceElementType inner = (SequenceElementType) child(root, 0);
        ParserNode node = node(inner, node(root, null, 0), 0);
        LeafElementType source = (LeafElementType) child(inner, 0);

        assertLookup(true, source, "KW_COMMIT", node);
        assertRequiredLookup(false, source, "KW_COMMIT", node);
        assertRequiredLookup(true, source, "CHR_LEFT_PARENTHESIS", node);
    }

    @Test
    public void indexedFirstTokensIncludeNestedAlternatives() throws Exception {
        NamedElementType root = grammar("""
                <token type-id="KW_BEGIN"/>
                <sequence><one-of tokens="KW_COMMIT,KW_NULL"/></sequence>
                <token type-id="KW_END"/>
                """);
        ParserNode node = node(root, null, 0);
        LeafElementType source = (LeafElementType) child(root, 0);

        assertLookup(true, source, "KW_COMMIT", node);
        assertLookup(true, source, "KW_NULL", node);
        assertLookup(false, source, "KW_END", node);
    }

    @Test
    public void nestedSequenceUsesItsParentsActiveOccurrence() throws Exception {
        NamedElementType root = grammar("""
                <element ref-id="atom"/>
                <token type-id="KW_COMMIT"/>
                <element ref-id="atom"/>
                <token type-id="KW_END"/>
                """, """
                <element-def id="atom" description="Atom"><token type-id="KW_BEGIN"/></element-def>
                """);
        NamedElementType atom = (NamedElementType) child(root, 0);
        LeafElementType source = (LeafElementType) child(atom, 0);
        ParserNode parent = node(root, null, 0);
        ParserNode first = node(atom, parent, 0);
        assertLookup(true, source, "KW_COMMIT", first);
        assertLookup(false, source, "KW_END", first);

        parent.elementIndex = 2;
        ParserNode second = node(atom, parent, 0);
        // Grammar identity alone would incorrectly select the first reference and suggest COMMIT.
        assertTrue(NextTokenResolver.isNextPossibleToken(source, token(root, "KW_END"), second));
        assertFalse(NextTokenResolver.isNextPossibleToken(source, token(root, "KW_COMMIT"), second));
        assertTrue(NextTokenResolver.isNextRequiredToken(source, token(root, "KW_END"), second));
        assertFalse(NextTokenResolver.isNextRequiredToken(source, token(root, "KW_COMMIT"), second));
        assertEquals(2, parent.elementIndex);
        assertEquals(0, second.elementIndex);
    }

    @Test
    public void matchingOrBlockedSequenceDoesNotVisitItsParent() throws Exception {
        NamedElementType root = grammar("""
                <token type-id="KW_BEGIN"/>
                <token type-id="KW_COMMIT"/>
                <token type-id="KW_END"/>
                """);
        ParserNode node = new ParserNode(root, null, 0, 0) {
            @Override
            public ParserNode getParent() {
                throw new AssertionError("A resolved lookup must return without walking further");
            }
        };
        LeafElementType source = (LeafElementType) child(root, 0);

        assertTrue(NextTokenResolver.isNextPossibleToken(source, token(root, "KW_COMMIT"), node));
        assertFalse(NextTokenResolver.isNextPossibleToken(source, token(root, "KW_END"), node));
        assertTrue(NextTokenResolver.isNextRequiredToken(source, token(root, "KW_COMMIT"), node));
        assertFalse(NextTokenResolver.isNextRequiredToken(source, token(root, "KW_END"), node));
    }

    @Test
    public void onlyUnseparatedIterationsOfferAnotherItemDirectly() throws Exception {
        for (boolean separated : new boolean[]{false, true}) {
            NamedElementType root = grammar("<iteration" + (separated ? " separator=\"CHR_COMMA\"" : "") + """
                    ><token type-id="KW_NULL"/></iteration>
                    <token type-id="KW_END"/>
                    """);
            IterationElementType iteration = (IterationElementType) child(root, 0);
            ParserNode node = node(iteration, node(root, null, 0), 0);
            LeafElementType source = (LeafElementType) iteration.iteratedElement;

            assertLookup(!separated, source, "KW_NULL", node);
            assertLookup(true, source, "KW_END", node);
            assertRequiredLookup(!separated, source, "KW_NULL", node);
            assertRequiredLookup(true, source, "KW_END", node);
        }
    }

    @Test
    public void wrapperEndBlocksOuterTokens() throws Exception {
        NamedElementType root = grammar("""
                <wrapper template="PARENTHESES"><token type-id="STRING"/></wrapper>
                <token type-id="KW_END"/>
                """);
        WrapperElementType wrapper = (WrapperElementType) child(root, 0);
        ParserNode node = node(wrapper, node(root, null, 0), 0);
        LeafElementType source = (LeafElementType) wrapper.wrappedElement;

        assertLookup(true, source, "CHR_RIGHT_PARENTHESIS", node);
        assertLookup(false, source, "KW_END", node);
        assertRequiredLookup(true, source, "CHR_RIGHT_PARENTHESIS", node);
        assertRequiredLookup(false, source, "KW_END", node);
    }

    @Test
    public void activeChoiceCanMatchOtherAlternativesOrAnOuterSuccessor() throws Exception {
        NamedElementType root = grammar("""
                <one-of tokens="KW_NULL,KW_COMMIT"/>
                <token type-id="KW_END"/>
                """);
        OneOfElementType choice = (OneOfElementType) child(root, 0);
        ParserNode node = node(choice, node(root, null, 0), 0);
        LeafElementType source = (LeafElementType) choice.children[0].elementType;

        assertLookup(true, source, "KW_COMMIT", node);
        assertLookup(true, source, "KW_END", node);
        assertLookup(false, source, "KW_BEGIN", node);
        assertRequiredLookup(false, source, "KW_COMMIT", node);
        assertRequiredLookup(true, source, "KW_END", node);
    }

    @Test
    public void qualifiedIdentifierSeparatorPreventsLookingOutsideTheIdentifier() throws Exception {
        NamedElementType root = grammar("""
                <qualified-identifier>
                    <variant><object-ref type="SCHEMA"/><object-ref type="TABLE"/></variant>
                </qualified-identifier>
                <token type-id="KW_END"/>
                """);
        QualifiedIdentifierElementType identifier = (QualifiedIdentifierElementType) child(root, 0);
        ParserNode node = node(identifier, node(root, null, 0), 0);

        assertLookup(false, identifier.separatorToken, "KW_END", node);
        assertLookup(true, identifier.variants.get(0)[0], "KW_END", node);
        assertRequiredLookup(false, identifier.separatorToken, "KW_END", node);
        assertRequiredLookup(true, identifier.variants.get(0)[0], "KW_END", node);
    }

    @Test
    public void missingPathHasNoSuccessor() throws Exception {
        NamedElementType root = grammar("<token type-id=\"KW_BEGIN\"/>");
        assertLookup(false, (LeafElementType) child(root, 0), "KW_END", null);
        assertRequiredLookup(false, (LeafElementType) child(root, 0), "KW_END", null);
    }

    private static NamedElementType grammar(@NonNls String contents, @NonNls String... definitions) throws Exception {
        String xml = "<element-defs><element-def id=\"test\" description=\"Test\">" +
                contents + "</element-def>" + String.join("", definitions) + "</element-defs>";
        Document document = XmlContents.streamToDocument(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        DBLanguageDialect dialect = PSQLLanguage.INSTANCE.getMainLanguageDialect();
        ElementTypeBundle bundle = new ElementTypeBundle(dialect, dialect.getParserTokenTypes(), document, null, null);
        NamedElementType root = bundle.getNamedElementType("test");
        assertTrue("Fixture grammar must be loaded", root.isDefinitionLoaded());
        return root;
    }

    private static ElementTypeBase child(SequenceElementType sequence, int index) {
        return sequence.children[index].elementType;
    }

    private static ParserNode node(ElementTypeBase element, ParserNode parent, int index) {
        return new ParserNode(element, parent, 0, index);
    }

    private static TokenType token(ElementTypeBase element, @NonNls String id) {
        return element.bundle.tokenTypeBundle.getTokenType(id);
    }

    @SuppressWarnings("deprecation")
    private static void assertLookup(boolean expected, LeafElementType source, @NonNls String tokenId, ParserNode node) {
        TokenType token = token(source, tokenId);
        assertEquals(expected, NextTokenResolver.isNextPossibleToken(source, token, node));
    }

    @SuppressWarnings("deprecation")
    private static void assertRequiredLookup(boolean expected, LeafElementType source, @NonNls String tokenId, ParserNode node) {
        TokenType token = token(source, tokenId);
        assertEquals(expected, NextTokenResolver.isNextRequiredToken(source, token, node));
    }
}

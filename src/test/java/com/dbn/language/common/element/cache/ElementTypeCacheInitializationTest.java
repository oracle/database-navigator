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

package com.dbn.language.common.element.cache;

import com.dbn.common.util.XmlContents;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.ElementTypeBundle;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.IterationElementType;
import com.dbn.language.common.element.impl.LeafElementType;
import com.dbn.language.common.element.impl.NamedElementType;
import com.dbn.language.common.element.impl.QualifiedIdentifierElementType;
import com.dbn.language.psql.PSQLLanguage;
import org.jdom.Document;
import org.jetbrains.annotations.NonNls;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ElementTypeCacheInitializationTest {
    @Test
    public void iterationSeparatorsReachAncestorCachesWithoutBecomingFirstTokens() throws Exception {
        ElementTypeBundle bundle = grammar("""
                <element-def id="test" description="Test">
                    <iteration separator="CHR_COMMA,CHR_SEMICOLON">
                        <token type-id="KW_NULL"/>
                    </iteration>
                </element-def>
                """);
        NamedElementType root = bundle.getNamedElementType("test");
        IterationElementType iteration = (IterationElementType) root.children[0].elementType;

        for (ElementTypeBase element : new ElementTypeBase[]{iteration, root}) {
            for (LeafElementType separator : iteration.separatorTokens) {
                assertTrue(element.cache.containsToken(separator.tokenType));
                assertTrue(element.cache.getAllPossibleTokens().contains(separator.tokenType));
                assertFalse(element.cache.couldStartWithLeaf(separator));
                assertFalse(element.cache.shouldStartWithLeaf(separator));
            }
            assertEquals(Set.of(token(bundle, "KW_NULL")), element.cache.getFirstPossibleTokens());
            assertEquals(Set.of(token(bundle, "KW_NULL")), element.cache.getFirstRequiredTokens());
        }
    }

    @Test
    public void qualifiedIdentifierSeparatorReachesAncestorCaches() throws Exception {
        ElementTypeBundle bundle = grammar("""
                <element-def id="test" description="Test">
                    <qualified-identifier>
                        <variant><object-ref type="SCHEMA"/><object-ref type="TABLE"/></variant>
                    </qualified-identifier>
                </element-def>
                """);
        NamedElementType root = bundle.getNamedElementType("test");
        QualifiedIdentifierElementType identifier = (QualifiedIdentifierElementType) root.children[0].elementType;

        for (ElementTypeBase element : new ElementTypeBase[]{identifier, root}) {
            assertTrue(element.cache.containsToken(identifier.separatorToken.tokenType));
            assertTrue(element.cache.getAllPossibleTokens().contains(identifier.separatorToken.tokenType));
            assertFalse(element.cache.isFirstPossibleToken(identifier.separatorToken.tokenType));
            assertFalse(element.cache.isFirstRequiredToken(identifier.separatorToken.tokenType));
            assertEquals(Set.of(identifier.variants.get(0)[0]), element.cache.getFirstPossibleLeafs());
        }
    }

    @Test
    public void namedWrappingCachesDoNotDependOnDefinitionOrder() throws Exception {
        String reference = """
                <element-def id="test" description="Test"><element ref-id="wrapped"/></element-def>
                """;
        String definition = """
                <element-def id="wrapped" description="Wrapped" optional-wrapping="PARENTHESES">
                    <token type-id="KW_NULL"/>
                </element-def>
                """;

        for (String definitions : new String[]{reference + definition, definition + reference}) {
            ElementTypeBundle bundle = grammar(definitions);
            NamedElementType wrapped = bundle.getNamedElementType("wrapped");
            LeafElementType content = (LeafElementType) wrapped.children[0].elementType;
            for (NamedElementType element : new NamedElementType[]{wrapped, bundle.getNamedElementType("test")}) {
                assertEquals(Set.of(wrapped.wrapping.beginElement, content), element.cache.getFirstPossibleLeafs());
                assertEquals(Set.of(content), element.cache.getFirstRequiredLeafs());
                assertEquals(Set.of(token(bundle, "CHR_LEFT_PARENTHESIS"), token(bundle, "KW_NULL")),
                        element.cache.getFirstPossibleTokens());
                assertEquals(Set.of(token(bundle, "KW_NULL")), element.cache.getFirstRequiredTokens());
                assertEquals(Set.of(token(bundle, "CHR_LEFT_PARENTHESIS"), token(bundle, "CHR_RIGHT_PARENTHESIS"),
                        token(bundle, "KW_NULL")), element.cache.getAllPossibleTokens());
            }
        }
    }

    @Test
    public void compactNamedTokensAreInitializedWithoutLaterReferences() throws Exception {
        ElementTypeBundle bundle = grammar("""
                <element-def id="test" description="Test"><element ref-id="compact"/></element-def>
                <element-def id="compact" description="Compact" tokens="KW_BEGIN,KW_END"/>
                <element-def id="unreferenced" description="Unreferenced" tokens="KW_BEGIN,KW_END"/>
                """);

        for (String id : new String[]{"test", "compact", "unreferenced"}) {
            NamedElementType element = bundle.getNamedElementType(id);
            assertEquals(Set.of(token(bundle, "KW_BEGIN"), token(bundle, "KW_END")), element.cache.getAllPossibleTokens());
            assertEquals(Set.of(token(bundle, "KW_BEGIN")), element.cache.getFirstPossibleTokens());
            assertEquals(Set.of(token(bundle, "KW_BEGIN")), element.cache.getFirstRequiredTokens());
        }
    }

    private static ElementTypeBundle grammar(@NonNls String definitions) throws Exception {
        String xml = "<element-defs>" + definitions + "</element-defs>";
        Document document = XmlContents.streamToDocument(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        DBLanguageDialect dialect = PSQLLanguage.INSTANCE.getMainLanguageDialect();
        ElementTypeBundle bundle = new ElementTypeBundle(dialect, dialect.getParserTokenTypes(), document, null, null);
        assertNull("Fixture cache initialization must complete", bundle.getBuilder());
        assertTrue("Fixture grammar must be loaded", bundle.getNamedElementType("test").isDefinitionLoaded());
        return bundle;
    }

    private static TokenType token(ElementTypeBundle bundle, @NonNls String id) {
        return bundle.tokenTypeBundle.getTokenType(id);
    }
}

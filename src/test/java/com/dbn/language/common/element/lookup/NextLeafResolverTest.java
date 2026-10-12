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
import com.dbn.language.common.element.ElementTypeBundle;
import com.dbn.language.common.element.cache.ElementLookupContext;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.IterationElementType;
import com.dbn.language.common.element.impl.LeafElementType;
import com.dbn.language.common.element.impl.NamedElementType;
import com.dbn.language.common.element.impl.OneOfElementType;
import com.dbn.language.common.element.impl.QualifiedIdentifierElementType;
import com.dbn.language.common.element.impl.SequenceElementType;
import com.dbn.language.common.element.impl.WrapperElementType;
import com.dbn.language.common.element.parser.Branch;
import com.dbn.language.psql.PSQLLanguage;
import com.intellij.lang.ASTNode;
import com.intellij.mock.MockApplication;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import org.jdom.Document;
import org.jetbrains.annotations.NonNls;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;

import static com.dbn.language.common.element.util.ElementTypeAttribute.STATEMENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Synthetic parsed ASTs exercise completion independently of parser recovery and database connections. */
public class NextLeafResolverTest {
    private static Disposable applicationDisposable;

    @BeforeClass
    public static void setUpApplication() {
        if (ApplicationManager.getApplication() != null) return;

        // Real AST nodes consult application-level PSI versioning even in synthetic trees.
        applicationDisposable = Disposer.newDisposable("NextLeafResolverTest");
        MockApplication.setUp(applicationDisposable);
    }

    @AfterClass
    public static void tearDownApplication() {
        if (applicationDisposable == null) return;

        try {
            Disposer.dispose(applicationDisposable);
        } finally {
            applicationDisposable = null;
        }
    }

    @Test
    public void beginAfterDeclarationAndCommentsEntersStatementBody() throws Exception {
        NamedElementType root = grammar("""
                <sequence optional="true">
                    <token type-id="KW_DECLARE"/>
                </sequence>
                <token type-id="KW_BEGIN"/>
                <iteration>
                    <one-of tokens="KW_COMMIT,KW_NULL"/>
                </iteration>
                <token type-id="KW_END"/>
                """);
        SequenceElementType declarations = (SequenceElementType) child(root, 0);
        IterationElementType statements = (IterationElementType) child(root, 2);
        OneOfElementType statement = (OneOfElementType) statements.iteratedElement;

        for (boolean withDeclaration : new boolean[]{true, false}) {
            CompositeElement ast = tree(root);
            trivia(ast);
            if (withDeclaration) {
                CompositeElement declareAst = tree(declarations);
                leaf(declareAst, child(declarations, 0));
                ast.rawAddChildren(declareAst);
                trivia(ast);
            }
            ASTNode begin = leaf(ast, child(root, 1));
            trivia(ast);
            CompositeElement body = tree(statements);
            CompositeElement commit = tree(statement);
            leaf(commit, statement.children[0].elementType);
            body.rawAddChildren(commit);
            ast.rawAddChildren(body);
            leaf(ast, child(root, 3));

            assertEquals(1, NextLeafAstUtil.sequenceIndex(begin, root).orElseThrow());
            assertLeafs(begin, statement.children[0].elementType, statement.children[1].elementType);
        }
    }

    @Test
    public void repeatedReferencesMapToTheCorrectOccurrence() throws Exception {
        NamedElementType root = grammar("""
                <element ref-id="atom"/>
                <token type-id="KW_BEGIN" optional="true"/>
                <element ref-id="atom"/>
                <token type-id="KW_END"/>
                """, """
                <element-def id="atom" description="Atom"><token type-id="STRING"/></element-def>
                """);
        SequenceElementType atom = (SequenceElementType) child(root, 0);
        CompositeElement ast = tree(root);
        CompositeElement first = tree(atom);
        leaf(first, child(atom, 0));
        ast.rawAddChildren(first);
        trivia(ast);
        CompositeElement second = tree(atom);
        ASTNode source = leaf(second, child(atom, 0));
        ast.rawAddChildren(second);

        assertLeafs(source, child(root, 3));
    }

    @Test
    public void unmappedLeafDoesNotRestartTheSequence() throws Exception {
        NamedElementType root = grammar("""
                <token type-id="KW_BEGIN"/>
                <token type-id="KW_END"/>
                """, """
                <element-def id="stray" description="Stray"><token type-id="KW_NULL"/></element-def>
                """);
        NamedElementType stray = root.bundle.getNamedElementType("stray");
        ASTNode source = leaf(tree(root), child(stray, 0));
        Set<LeafElementType> result = NextLeafResolver.nextPossibleLeafs(source, context());

        assertTrue(result.isEmpty());
    }

    @Test
    public void abortedTraversalDiscardsCollectedCandidates() throws Exception {
        NamedElementType root = grammar("""
                <token type-id="KW_END"/>
                """, """
                <element-def id="stray" description="Stray">
                    <token type-id="KW_BEGIN"/>
                    <token type-id="KW_NULL" optional="true"/>
                </element-def>
                """);
        NamedElementType stray = root.bundle.getNamedElementType("stray");
        for (ElementTypeBase parentType : new ElementTypeBase[]{root, root.bundle.unknownElementType}) {
            CompositeElement strayAst = tree(stray);
            ASTNode source = leaf(strayAst, child(stray, 0));
            assertLeafs(source, child(stray, 1));

            // Neither an unmapped occurrence nor an unsupported parent may expose partial results.
            tree(parentType).rawAddChildren(strayAst);
            assertTrue(NextLeafResolver.nextPossibleLeafs(source, context()).isEmpty());
        }
    }

    @Test
    public void collectedCandidatesUseExactGrammarLeafIdentity() throws Exception {
        NamedElementType root = grammar("""
                <token type-id="KW_BEGIN"/>
                <token type-id="KW_NULL"/>
                """, """
                <element-def id="other" description="Other"><token type-id="KW_NULL"/></element-def>
                """);
        ASTNode source = leaf(tree(root), child(root, 0));
        NamedElementType other = root.bundle.getNamedElementType("other");

        Set<LeafElementType> result = NextLeafResolver.nextPossibleLeafs(source, context());
        assertEquals(Set.of(child(root, 1)), result);
        assertFalse(result.contains(child(other, 0)));
    }

    @Test
    public void recursiveFirstLeafCaptureContinuesWithOtherAlternativesAndReleasesItsGuard() throws Exception {
        NamedElementType root = grammar("""
                <token type-id="KW_BEGIN"/>
                <element ref-id="choice"/>
                <token type-id="KW_END"/>
                """, """
                <element-def id="choice" description="Choice">
                    <one-of>
                        <element ref-id="choice"/>
                        <token type-id="KW_NULL"/>
                        <token type-id="KW_COMMIT"/>
                    </one-of>
                </element-def>
                """);
        NamedElementType choice = (NamedElementType) child(root, 1);
        OneOfElementType alternatives = (OneOfElementType) child(choice, 0);
        ASTNode source = leaf(tree(root), child(root, 0));

        // Repeated collection must release the recursion guard between queries.
        assertLeafs(source, alternatives.children[1].elementType, alternatives.children[2].elementType);
        assertLeafs(source, alternatives.children[1].elementType, alternatives.children[2].elementType);
    }

    @Test
    public void separatedIterationRequiresSeparatorBeforeAnotherItem() throws Exception {
        NamedElementType root = grammar("""
                <iteration separator="CHR_COMMA"><token type-id="KW_NULL"/></iteration>
                <token type-id="KW_END"/>
                """);
        IterationElementType iteration = (IterationElementType) child(root, 0);
        CompositeElement ast = tree(root);
        CompositeElement items = tree(iteration);
        ast.rawAddChildren(items);
        ASTNode item = leaf(items, iteration.iteratedElement);
        assertLeafs(item, iteration.separatorTokens[0], child(root, 1));

        ASTNode separator = leaf(items, iteration.separatorTokens[0]);
        assertLeafs(separator, iteration.iteratedElement);
    }

    @Test
    public void iterationCountsDoNotRestrictCompletion() throws Exception {
        for (boolean separated : new boolean[]{false, true}) {
            NamedElementType root = grammar("<iteration" + (separated ? " separator=\"CHR_COMMA\"" : "") + """
                     elements-count="2" min-iterations="2">
                        <token type-id="KW_NULL"/>
                    </iteration>
                    <token type-id="KW_END"/>
                    """);
            IterationElementType iteration = (IterationElementType) child(root, 0);
            CompositeElement ast = tree(root);
            CompositeElement items = tree(iteration);
            ast.rawAddChildren(items);

            // Completion offers repetition and exit below, at, and above the declared count.
            for (int count = 1; count <= 3; count++) {
                if (separated && count > 1) {
                    ASTNode separator = leaf(items, iteration.separatorTokens[0]);
                    assertLeafs(separator, iteration.iteratedElement);
                }
                ASTNode item = leaf(items, iteration.iteratedElement);
                ElementTypeBase continuation = separated ? iteration.separatorTokens[0] : iteration.iteratedElement;
                assertLeafs(item, continuation, child(root, 1));
            }
        }
    }

    @Test
    public void wrapperAllowsEmptyContentsAndRequiresItsEndBeforeExiting() throws Exception {
        NamedElementType root = grammar("""
                <wrapper template="PARENTHESES"><token type-id="STRING" optional="true"/></wrapper>
                <token type-id="KW_COMMIT"/>
                """);
        WrapperElementType wrapper = (WrapperElementType) child(root, 0);
        CompositeElement ast = tree(root);
        CompositeElement wrapped = tree(wrapper);
        ast.rawAddChildren(wrapped);
        ASTNode begin = leaf(wrapped, wrapper.getBeginTokenElement());
        assertLeafs(begin, wrapper.wrappedElement, wrapper.getEndTokenElement());

        ASTNode contents = leaf(wrapped, wrapper.wrappedElement);
        assertLeafs(contents, wrapper.getEndTokenElement());
        ASTNode end = leaf(wrapped, wrapper.getEndTokenElement());
        assertLeafs(end, child(root, 1));
    }

    @Test
    public void optionalWrappingRequiresEndOnlyWhenOpened() throws Exception {
        NamedElementType root = grammar("""
                <sequence optional-wrapping="PARENTHESES"><token type-id="KW_NULL"/></sequence>
                <token type-id="KW_COMMIT"/>
                """);
        SequenceElementType sequence = (SequenceElementType) child(root, 0);
        for (boolean wrapped : new boolean[]{true, false}) {
            CompositeElement ast = tree(root);
            CompositeElement contents = tree(sequence);
            ast.rawAddChildren(contents);
            if (wrapped) leaf(contents, sequence.wrapping.beginElement);
            ASTNode source = leaf(contents, child(sequence, 0));
            assertLeafs(source, wrapped ? sequence.wrapping.endElement : child(root, 1));
            if (wrapped) {
                ASTNode end = leaf(contents, sequence.wrapping.endElement);
                assertLeafs(end, child(root, 1));
            }
        }
    }

    @Test
    public void nestedWrapperEndsDoNotExitTheOuterWrapper() throws Exception {
        NamedElementType root = grammar("""
                <wrapper template="PARENTHESES">
                    <wrapper template="PARENTHESES"><token type-id="STRING"/></wrapper>
                </wrapper>
                <token type-id="KW_COMMIT"/>
                """);
        WrapperElementType outer = (WrapperElementType) child(root, 0);
        WrapperElementType inner = (WrapperElementType) outer.wrappedElement;
        CompositeElement ast = tree(root);
        CompositeElement outerAst = tree(outer);
        ast.rawAddChildren(outerAst);
        leaf(outerAst, outer.getBeginTokenElement());
        CompositeElement innerAst = tree(inner);
        outerAst.rawAddChildren(innerAst);
        leaf(innerAst, inner.getBeginTokenElement());
        leaf(innerAst, inner.wrappedElement);
        ASTNode innerEnd = leaf(innerAst, inner.getEndTokenElement());

        assertLeafs(innerEnd, outer.getEndTokenElement());
    }

    @Test
    public void qualifiedIdentifierUsesOnlyPrefixThroughTheSource() throws Exception {
        NamedElementType root = grammar("""
                <qualified-identifier>
                    <variant>
                        <object-ref type="SCHEMA" optional="true"/>
                        <object-ref type="TABLE"/>
                        <object-ref type="COLUMN"/>
                    </variant>
                </qualified-identifier>
                <token type-id="KW_END"/>
                """);
        QualifiedIdentifierElementType identifier = (QualifiedIdentifierElementType) child(root, 0);
        LeafElementType[] variant = identifier.variants.get(0);
        CompositeElement ast = tree(root);
        CompositeElement name = tree(identifier);
        ast.rawAddChildren(name);
        ASTNode schema = leaf(name, variant[0]);
        assertLeafs(schema, identifier.separatorToken);
        leaf(name, identifier.separatorToken);
        leaf(name, variant[1]);
        trivia(name);
        ASTNode secondDot = leaf(name, identifier.separatorToken);
        ASTNode column = leaf(name, variant[2]);

        assertLeafs(secondDot, variant[2]);
        assertLeafs(column, child(root, 1));
    }

    @Test
    public void finishingOneOfDoesNotRestartOtherAlternatives() throws Exception {
        NamedElementType root = grammar("""
                <one-of tokens="KW_NULL,KW_COMMIT"/>
                <token type-id="KW_END"/>
                """);
        OneOfElementType alternatives = (OneOfElementType) child(root, 0);
        CompositeElement ast = tree(root);
        CompositeElement choice = tree(alternatives);
        ast.rawAddChildren(choice);
        ASTNode source = leaf(choice, alternatives.children[0].elementType);
        assertLeafs(source, child(root, 1));
    }

    @Test
    public void leavingOptionalTailClearsOnlyTheCompletedNamedScope() throws Exception {
        NamedElementType root = grammar("""
                <element ref-id="scoped"/>
                <token type-id="KW_COMMIT" optional="true" branch-check="-outer"/>
                <token type-id="KW_END" branch-check="-scope"/>
                """, """
                <element-def id="scoped" description="Scoped">
                    <token type-id="KW_BEGIN"/>
                    <token type-id="STRING" optional="true"/>
                </element-def>
                """);
        NamedElementType scoped = (NamedElementType) child(root, 0);
        CompositeElement ast = tree(root);
        CompositeElement scopedAst = tree(scoped);
        ast.rawAddChildren(scopedAst);
        ASTNode source = leaf(scopedAst, child(scoped, 0));
        ElementLookupContext context = context();
        context.addBranchMarker(scopedAst, new Branch("outer"));
        context.addBranchMarker(source, new Branch("scope"));
        Set<LeafElementType> result = NextLeafResolver.nextPossibleLeafs(source, context);

        assertEquals(Set.of(child(scoped, 1), child(root, 2)), result);
    }

    @Test
    public void versionChecksSkipUnavailableChildren() throws Exception {
        NamedElementType root = grammar("""
                <token type-id="KW_BEGIN"/>
                <token type-id="KW_COMMIT" version="99"/>
                <token type-id="KW_END"/>
                """);
        ASTNode source = leaf(tree(root), child(root, 0));
        assertLeafs(source, child(root, 2));
    }

    @Test
    public void nestedFirstLeafCaptureSkipsUnavailableRequiredChildren() throws Exception {
        NamedElementType root = grammar("""
                <token type-id="KW_BEGIN"/>
                <sequence>
                    <token type-id="KW_COMMIT" version="99"/>
                    <token type-id="KW_END"/>
                </sequence>
                <token type-id="KW_NULL"/>
                """);
        SequenceElementType nested = (SequenceElementType) child(root, 1);
        ASTNode source = leaf(tree(root), child(root, 0));

        for (int version : new int[]{19, 99}) {
            LeafElementType expected = (LeafElementType) child(nested, version < 99 ? 1 : 0);
            assertEquals(Set.of(expected), NextLeafResolver.nextPossibleLeafs(source, new ElementLookupContext(version)));
            assertEquals(Set.of(expected.tokenType), nested.cache.captureFirstPossibleTokens(new ElementLookupContext(version)));
        }
    }

    @Test
    public void nestedFirstLeafCaptureSkipsBranchExcludedRequiredChildren() throws Exception {
        NamedElementType root = grammar("""
                <token type-id="KW_BEGIN"/>
                <sequence>
                    <token type-id="KW_COMMIT" branch-check="-scope"/>
                    <token type-id="KW_END"/>
                </sequence>
                <token type-id="KW_NULL"/>
                """);
        SequenceElementType nested = (SequenceElementType) child(root, 1);
        ASTNode source = leaf(tree(root), child(root, 0));

        for (boolean excluded : new boolean[]{false, true}) {
            ElementLookupContext leafContext = context();
            ElementLookupContext tokenContext = context();
            if (excluded) {
                leafContext.addBranchMarker(source, new Branch("scope"));
                tokenContext.addBranchMarker(source, new Branch("scope"));
            }
            LeafElementType expected = (LeafElementType) child(nested, excluded ? 1 : 0);
            assertEquals(Set.of(expected), NextLeafResolver.nextPossibleLeafs(source, leafContext));
            assertEquals(Set.of(expected.tokenType), nested.cache.captureFirstPossibleTokens(tokenContext));
        }
    }

    @Test
    public void statementBoundaryStopsBeforeOuterCandidates() throws Exception {
        NamedElementType root = grammar("""
                <sequence attributes="STATEMENT"><token type-id="KW_NULL"/></sequence>
                <token type-id="KW_END"/>
                """);
        SequenceElementType statement = (SequenceElementType) child(root, 0);
        CompositeElement ast = tree(root);
        CompositeElement statementAst = tree(statement);
        ast.rawAddChildren(statementAst);
        ASTNode source = leaf(statementAst, child(statement, 0));
        ElementLookupContext context = context();
        context.addBreakOnAttribute(STATEMENT);

        Set<LeafElementType> result = NextLeafResolver.nextPossibleLeafs(source, context);
        assertTrue(result.isEmpty());
        assertLeafs(source, child(root, 1));
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

    private static CompositeElement tree(ElementTypeBase type) {
        return new CompositeElement(type);
    }

    private static ASTNode leaf(CompositeElement parent, IElementType type) {
        LeafPsiElement leaf = new LeafPsiElement(type, type.toString());
        parent.rawAddChildren(leaf);
        return leaf;
    }

    private static void trivia(CompositeElement parent) {
        var tokens = PSQLLanguage.INSTANCE.getSharedTokenTypes();
        leaf(parent, tokens.whiteSpace);
        leaf(parent, tokens.lineComment);
        leaf(parent, tokens.whiteSpace);
    }

    private static ElementLookupContext context() {
        return new ElementLookupContext(19);
    }

    private static void assertLeafs(ASTNode source, ElementTypeBase... expected) {
        Set<LeafElementType> result = NextLeafResolver.nextPossibleLeafs(source, context());
        assertEquals(Set.copyOf(Arrays.asList(expected)), result);
    }
}

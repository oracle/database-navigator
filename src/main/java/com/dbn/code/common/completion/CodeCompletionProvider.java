package com.dbn.code.common.completion;

import com.dbn.code.common.completion.options.filter.CodeCompletionFilterSettings;
import com.dbn.code.common.lookup.CodeCompletionLookupItem;
import com.dbn.common.routine.Consumer;
import com.dbn.common.util.Naming;
import com.dbn.connection.ConnectionHandler;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.ElementTypeBundle;
import com.dbn.language.common.element.cache.ElementLookupContext;
import com.dbn.language.common.element.cache.ElementTypeLookupCache;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.IdentifierElementType;
import com.dbn.language.common.element.impl.LeafElementType;
import com.dbn.language.common.element.impl.QualifiedIdentifierVariant;
import com.dbn.language.common.element.impl.TokenElementType;
import com.dbn.language.common.element.parser.Branch;
import com.dbn.language.common.element.path.AstNode;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.common.psi.IdentifierPsiElement;
import com.dbn.language.common.psi.LeafPsiElement;
import com.dbn.language.common.psi.PsiUtil;
import com.dbn.language.common.psi.QualifiedIdentifierPsiElement;
import com.dbn.language.common.psi.TokenPsiElement;
import com.dbn.language.common.psi.lookup.LookupAdapters;
import com.dbn.language.common.psi.lookup.PsiLookupAdapter;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectBundle;
import com.dbn.object.common.DBObjectPsiElement;
import com.dbn.object.common.DBVirtualObject;
import com.dbn.object.common.ObjectTypeFilter;
import com.dbn.object.filter.custom.ObjectFilterAttribute;
import com.dbn.object.type.DBObjectType;
import com.dbn.vfs.file.DBObjectFilterExpressionFile;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.FileElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.dbn.connection.ConnectionHandler.isLiveConnection;
import static com.dbn.object.DBSynonym.unwrap;

public class CodeCompletionProvider extends CompletionProvider<CompletionParameters> {
    public static final CodeCompletionProvider INSTANCE = new CodeCompletionProvider();

    private CodeCompletionProvider() {
        super();
    }

    @Override
    protected void addCompletions(
            @NotNull CompletionParameters parameters,
            @NotNull ProcessingContext processingContext,
            @NotNull CompletionResultSet result) {
        PsiFile originalFile = parameters.getOriginalFile();
        if (!(originalFile instanceof DBLanguagePsiFile)) return;

        if (handleFilterExpressionFile(result, originalFile)) return;

        DBLanguagePsiFile file = (DBLanguagePsiFile) originalFile;

        int caretOffset = parameters.getOffset();
        PsiElement elementAtCaret = file.findElementAt(caretOffset);
        if (elementAtCaret instanceof PsiComment) return;

        LeafPsiElement leafAtCaret = caretOffset == 0 ? null : PsiUtil.lookupLeafAtOffset(file, caretOffset-1);
        LeafPsiElement leafBeforeCaret = leafAtCaret == null || leafAtCaret.isCharacterToken() ?
                PsiUtil.lookupLeafBeforeOffset(file, caretOffset) :
                PsiUtil.lookupLeafBeforeOffset(file, leafAtCaret.getTextOffset());

        if (!shouldAddCompletions(leafAtCaret, leafBeforeCaret)) return;


        CodeCompletionContext context = new CodeCompletionContext(file, parameters, result);
        int invocationCount = parameters.getInvocationCount();
        if (invocationCount > 1) context.setExtended(true);

        CodeCompletionLookupConsumer consumer = new CodeCompletionLookupConsumer(context);
        collectCompletionVariants(consumer, leafBeforeCaret);
    }

    private static boolean handleFilterExpressionFile(@NotNull CompletionResultSet result, PsiFile psiFile) {
        VirtualFile virtualFile = psiFile.getVirtualFile();
        if (virtualFile instanceof DBObjectFilterExpressionFile) {
            DBObjectFilterExpressionFile expressionFile = (DBObjectFilterExpressionFile) virtualFile;
            List<ObjectFilterAttribute> attributesTypes = expressionFile.getFilter().getDefinition().getAttributes();
            attributesTypes.forEach(a -> result.addElement(a.getLookupItem()));

            Arrays.asList("AND", "OR", "IS", "IN", "NOT", "NULL", "LIKE").forEach(s -> result.addElement(new CodeCompletionLookupItem(s, null, s, "keyword", true)));
            return true;
        }
        return false;
    }

    private boolean shouldAddCompletions(LeafPsiElement leafAtOffset, LeafPsiElement leafBeforeCaret) {
        if (leafAtOffset instanceof TokenPsiElement) {
            TokenPsiElement tokenPsiElement = (TokenPsiElement) leafAtOffset;
            TokenType tokenType = tokenPsiElement.getTokenType();
            if (tokenType.isNumeric()) return false;
            if (tokenType.isLiteral()) return false;
        }

        return true;
    }

    private void collectCompletionVariants(CodeCompletionLookupConsumer consumer, LeafPsiElement leafBeforeCaret) {
        if (leafBeforeCaret == null) {
            collectRootCompletionVariants(consumer);
        } else {
            leafBeforeCaret = (LeafPsiElement) leafBeforeCaret.getOriginalElement();
            collectElementRelativeVariants(leafBeforeCaret, consumer);
        }
    }

    private static void collectRootCompletionVariants(CodeCompletionLookupConsumer consumer) {
        CodeCompletionContext context = consumer.getContext();
        DBLanguagePsiFile file = context.getFile();

        ElementTypeBundle elementTypeBundle = file.getElementTypeBundle();
        ElementTypeLookupCache<?> lookupCache = elementTypeBundle.getRootElementType().cache;
        ElementLookupContext lookupContext = new ElementLookupContext(context.getDatabaseVersion());
        Set<LeafElementType> firstPossibleLeafs = lookupCache.captureFirstPossibleLeafs(lookupContext);

        for (LeafElementType firstPossibleLeaf : firstPossibleLeafs) {
            if (firstPossibleLeaf instanceof TokenElementType) {
                TokenElementType tokenElementType = (TokenElementType) firstPossibleLeaf;
                consumer.accept(tokenElementType);
            }
        }
    }

    private static void collectElementRelativeVariants(LeafPsiElement element, CodeCompletionLookupConsumer consumer) {
        CodeCompletionContext context = consumer.getContext();

        IdentifierPsiElement parentIdentifierPsiElement = null;

        DBObject parentObject = null;
        PsiElement parent = element.getParent();
        if (parent instanceof QualifiedIdentifierPsiElement) {
            QualifiedIdentifierPsiElement qualifiedIdentifier = (QualifiedIdentifierPsiElement) parent;
            ElementType separator = qualifiedIdentifier.elementType.getSeparatorToken();

            if (element.elementType == separator){
                BasePsiElement parentPsiElement = element.getPrevElement();
                if (parentPsiElement instanceof IdentifierPsiElement) {
                    parentIdentifierPsiElement = (IdentifierPsiElement) parentPsiElement;
                    parentObject = parentIdentifierPsiElement.getUnderlyingObject();

                    if (parentObject != null) {
                        for (QualifiedIdentifierVariant parseVariant : qualifiedIdentifier.getParseVariants()){
                            boolean match = parseVariant.matchesPsiElement(qualifiedIdentifier);
                            if (match) {
                                int index = qualifiedIdentifier.getIndexOf(parentIdentifierPsiElement);
                                LeafElementType leafElementType = parseVariant.getLeaf(index + 1);
                                context.addCompletionCandidate(leafElementType);
                            }
                        }
                    }
                }
            }
        } else if (element.elementType.getTokenType() == element.getLanguage().getSharedTokenTypes().getChrDot()) {
            LeafPsiElement parentPsiElement = element.getPrevLeaf();
            if (parentPsiElement != null) {
                if (parentPsiElement instanceof IdentifierPsiElement || parentPsiElement.isVirtualObject()) {
                    parentObject = parentPsiElement.getUnderlyingObject();
                }
            }
        } else if (parent instanceof BasePsiElement) {
            BasePsiElement basePsiElement = (BasePsiElement) parent;
            ElementTypeBase elementType = basePsiElement.elementType;
            if (elementType.isWrappingBegin((LeafElementType) element.elementType)) {
                Set<LeafElementType> candidates = elementType.cache.getFirstPossibleLeafs();
                for (LeafElementType candidate : candidates) {
                    context.addCompletionCandidate(candidate);
                }
            }
        }

        if (!context.hasCompletionCandidates()) {
            LeafElementType elementType = (LeafElementType) element.elementType;
            AstNode node = new AstNode(element.getNode());
            ElementLookupContext lookupContext = computeParseBranches(element.getNode(), context.getDatabaseVersion());
            if (!context.isNewLine()) {
                lookupContext.addBreakOnAttribute(ElementTypeAttribute.STATEMENT);
            }
            Set<LeafElementType> candidates = elementType.getNextPossibleLeafs(node, lookupContext);
            for (LeafElementType candidate : candidates) {
                context.addCompletionCandidate(candidate);
            }
        }

        context.setParentIdentifierPsiElement(parentIdentifierPsiElement);
        context.setParentObject(parentObject);

        collectTokenElements(consumer);
        collectIdentifierElements(element, consumer);
        context.awaitCompletion();
    }

    private static void collectTokenElements(CodeCompletionLookupConsumer consumer) {
        CodeCompletionContext context = consumer.getContext();
        context.queue(() -> {
            Collection<LeafElementType> completionCandidates = context.getCompletionCandidates();
            for (LeafElementType elementType : completionCandidates) {
                if (elementType instanceof TokenElementType) {
                    TokenElementType tokenElementType = (TokenElementType) elementType;
                    //consumer.setAddParenthesis(addParenthesis && tokenType.isFunction());
                    consumer.accept(tokenElementType);
                }
            }
        });
    }

    private static void collectIdentifierElements(LeafPsiElement element, CodeCompletionLookupConsumer consumer) {
        CodeCompletionContext context = consumer.getContext();
        IdentifierPsiElement parentIdentifierPsiElement = context.getParentIdentifierPsiElement();
        DBObject parentObject = context.getParentObject();

        Collection<LeafElementType> completionCandidates = context.getCompletionCandidates();
        for (LeafElementType elementType : completionCandidates) {
            if (!(elementType instanceof IdentifierElementType)) continue;


            IdentifierElementType identifier = (IdentifierElementType) elementType;
            if (identifier.isReference()) {
                DBObjectType objectType = identifier.getObjectType();
                if (parentIdentifierPsiElement == null) {
                    if (identifier.isObject()) {
                        context.queue(() -> collectObjectElements(element, consumer, identifier, objectType));

                    } else if (identifier.isAlias()) {
                        context.queue(() -> collectAliasElements(element, consumer, objectType));

                    } else if (identifier.isVariable()) {
                        context.queue(() -> collectVariableElements(element, consumer, objectType));
                    }
                }
                if (parentObject != null && (context.isLiveConnection() || parentObject instanceof DBVirtualObject)) {
                    context.queue(() -> parentObject.collectChildObjects(objectType, consumer));
                }
            } else if (identifier.isDefinition()) {
                if (identifier.isAlias()) {
                    context.queue(() -> buildAliasDefinitionNames(element, consumer));
                }
            }
        }
    }

    private static void collectObjectElements(LeafPsiElement<?> element, CodeCompletionLookupConsumer consumer, IdentifierElementType identifierElementType, DBObjectType objectType) {
        CodeCompletionContext context = consumer.getContext();
        CodeCompletionFilterSettings filterSettings = context.getCodeCompletionFilterSettings();
        PsiLookupAdapter lookupAdapter = LookupAdapters.objectDefinition(objectType);
        lookupAdapter.collectInParentScopeOf(element, psiElement -> {
            if (psiElement instanceof IdentifierPsiElement) {
                IdentifierPsiElement identifierPsiElement = (IdentifierPsiElement) psiElement;
                PsiElement referencedPsiElement = identifierPsiElement.resolve();
                if (referencedPsiElement instanceof DBObjectPsiElement) {
                    DBObjectPsiElement objectPsiElement = (DBObjectPsiElement) referencedPsiElement;
                    consumer.accept(objectPsiElement);
                } else {
                    consumer.accept(identifierPsiElement);
                }
            }
        });

        BasePsiElement scope = element.getEnclosingScopeElement();
        if (scope != null) {
            collectObjectMatchingScope(consumer, identifierElementType, filterSettings, scope, context);
        }
    }

    private static void collectAliasElements(LeafPsiElement<?> scopeElement, CodeCompletionLookupConsumer consumer, DBObjectType objectType) {
        PsiLookupAdapter lookupAdapter = LookupAdapters.aliasDefinition(objectType);
        lookupAdapter.collectInParentScopeOf(scopeElement, consumer);
    }

    private static void collectVariableElements(LeafPsiElement<?> scopeElement, CodeCompletionLookupConsumer consumer, DBObjectType objectType) {
        PsiLookupAdapter lookupAdapter = LookupAdapters.variableDefinition(objectType);
        lookupAdapter.collectInParentScopeOf(scopeElement, consumer);
    }

    @NotNull
    private static ElementLookupContext computeParseBranches(ASTNode astNode, double databaseVersion) {
        ElementLookupContext lookupContext = new ElementLookupContext(databaseVersion);
        while (astNode != null && !(astNode instanceof FileElement)) {
            IElementType elementType = astNode.getElementType();
            if (elementType instanceof ElementTypeBase) {
                ElementTypeBase basicElementType = (ElementTypeBase) elementType;
                Branch branch = basicElementType.branch;
                if (branch != null) {
                    lookupContext.addBranchMarker(astNode, branch);
                }
            }
            ASTNode prevAstNode = astNode.getTreePrev();
            if (prevAstNode == null) {
                prevAstNode = astNode.getTreeParent();
            }
            astNode = prevAstNode;
        }
        return lookupContext;
    }

    private static void buildAliasDefinitionNames(BasePsiElement aliasElement, CodeCompletionLookupConsumer consumer) {
        IdentifierPsiElement aliasedObject = PsiUtil.lookupObjectPriorTo(aliasElement, DBObjectType.ANY);
        if (aliasedObject == null) return;
        if (!aliasedObject.isObject()) return;

        CharSequence unquotedText = aliasedObject.getUnquotedText();
        if (unquotedText.length() == 0) return;

        String[] aliasNames = Naming.createAliasNames(unquotedText);
        BasePsiElement scope = aliasElement.getEnclosingScopeElement();

        for (int i = 0; i< aliasNames.length; i++) {
            while (true) {
                PsiLookupAdapter lookupAdapter = LookupAdapters.aliasDefinition(null, DBObjectType.ANY, aliasNames[i]);
                boolean isExisting = scope != null && lookupAdapter.findInScope(scope) != null;
                boolean isKeyword = aliasElement.getLanguageDialect().isReservedWord(aliasNames[i]);
                if (isKeyword || isExisting) {
                    aliasNames[i] = Naming.nextNumberedIdentifier(aliasNames[i], false);
                } else {
                    break;
                }
            }
        }
        consumer.accept(aliasNames);
    }

    private static void collectObjectMatchingScope(
            Consumer<? super DBObject> consumer,
            IdentifierElementType identifierElementType,
            ObjectTypeFilter filter,
            @NotNull  BasePsiElement sourceScope,
            CodeCompletionContext context) {
        ConnectionHandler connection = context.getConnection();
        if (!isLiveConnection(connection)) return;

        DBObjectType objectType = identifierElementType.getObjectType();
        PsiElement sourceElement = context.getElementAtCaret();

        DBObjectBundle objectBundle = connection.getObjectBundle();
        if (sourceElement.getParent() instanceof QualifiedIdentifierPsiElement && sourceElement.getParent().getFirstChild() != sourceElement) {
            QualifiedIdentifierPsiElement qualifiedIdentifierPsiElement = (QualifiedIdentifierPsiElement) sourceElement.getOriginalElement().getParent();
            DBObject parentObject = qualifiedIdentifierPsiElement.lookupParentObjectFor(identifierElementType);

            parentObject = unwrap(parentObject);
            if (parentObject == null) return;

            DBSchema currentSchema = PsiUtil.getDatabaseSchema(sourceScope);
            objectBundle.lookupChildObjectsOfType(
                    consumer,
                    parentObject,
                    objectType,
                    filter,
                    currentSchema);

        } else if (!identifierElementType.isLocalReference()){
            Set<DBObject> parentObjects = LeafPsiElement.identifyPotentialParentObjects(objectType, filter, sourceScope, null);
            if (parentObjects == null || parentObjects.isEmpty()) {
                if (filter.acceptsRootObject(objectType)) {
                    objectBundle.lookupObjectsOfType(
                            consumer,
                            objectType);
                }
            } else {
                for (DBObject parentObject : parentObjects) {
                    parentObject = unwrap(parentObject);
                    if (parentObject == null) continue;

                    DBSchema currentSchema = PsiUtil.getDatabaseSchema(sourceScope);
                    objectBundle.lookupChildObjectsOfType(
                            consumer,
                            parentObject,
                            objectType,
                            filter,
                            currentSchema);
                }
            }
        }
    }
}

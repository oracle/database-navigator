package com.dbn.language.common.psi;

import com.dbn.code.common.style.formatting.FormattingAttributes;
import com.dbn.common.Capture;
import com.dbn.common.consumer.ListCollector;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.language.common.QuotePair;
import com.dbn.language.common.element.impl.IdentifierElementType;
import com.dbn.language.common.element.impl.LeafElementType;
import com.dbn.language.common.element.impl.QualifiedIdentifierVariant;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.element.util.IdentifierType;
import com.dbn.language.common.psi.lookup.IdentifierLookupAdapter;
import com.dbn.language.common.psi.lookup.LookupAdapters;
import com.dbn.language.common.psi.lookup.ObjectDefinitionLookupAdapter;
import com.dbn.language.common.psi.lookup.ObjectReferenceLookupAdapter;
import com.dbn.language.common.psi.lookup.PsiLookupAdapter;
import com.dbn.language.common.resolve.AliasObjectResolver;
import com.dbn.language.common.resolve.SurroundingVirtualObjectResolver;
import com.dbn.language.common.resolve.UnderlyingObjectResolver;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectBundle;
import com.dbn.object.common.DBObjectPsiCache;
import com.dbn.object.common.DBObjectPsiElement;
import com.dbn.object.common.DBVirtualObject;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Set;
import java.util.function.Consumer;

import static com.dbn.common.thread.ThreadMonitor.isDispatchThread;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.connection.ConnectionHandler.isLiveConnection;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.object.DBSynonym.unwrap;

public abstract class IdentifierPsiElement extends LeafPsiElement<IdentifierElementType> {
    private PsiResolveResult ref;
    private final Capture<DBObjectRef<?>> underlyingObject = new Capture<>();

    public IdentifierPsiElement(ASTNode astNode, IdentifierElementType elementType) {
        super(astNode, elementType);
    }

    @Override
    public ItemPresentation getPresentation() {
        return this;
    }

    public boolean isQuoted() {
        CharSequence chars = getChars();
        if (chars.length() > 1) {
            // optimized lookup
            if (QuotePair.isPossibleBeginQuote(chars.charAt(0))) {
                return getIdentifierQuotes().isQuoted(chars);
            }
        }
        return false;
    }

    @Override
    public String getName() {
        return getText();
    }

    @Override
    public FormattingAttributes getFormattingAttributes() {
        return super.getFormattingAttributes();
    }

    /**
     * ******************************************************
     * ItemPresentation                *
     * *******************************************************
     */
    @Override
    public String getPresentableText() {
        return Strings.toUpperCase(getUnquotedText()) + " (" + getObjectType() + ")";
    }

    @Override
    @Nullable
    public Icon getIcon(boolean open) {
        DBObjectType type = getObjectType();
        return type.getIcon();
    }

    @Override
    @Nullable
    public TextAttributesKey getTextAttributesKey() {
        return null;
    }


    /**
     * ******************************************************
     * Lookup routines                 *
     * *******************************************************
     */
    @Override
    @Nullable
    public BasePsiElement findPsiElement(PsiLookupAdapter lookupAdapter, int scopeCrossCount) {
        if (lookupAdapter instanceof IdentifierLookupAdapter) {
            IdentifierLookupAdapter identifierLookupAdapter = (IdentifierLookupAdapter) lookupAdapter;
            if (identifierLookupAdapter.matchesName(this)) {
                /*PsiElement parentPsiElement = getParent();
                if (parentPsiElement instanceof QualifiedIdentifierPsiElement) {
                    QualifiedIdentifierPsiElement qualifiedIdentifierPsiElement = (QualifiedIdentifierPsiElement) parentPsiElement;
                    QualifiedIdentifierElementType qualifiedIdentifierElementType = qualifiedIdentifierPsiElement.getElementType();
                    if (!qualifiedIdentifierElementType.containsObjectType(identifierLookupAdapter.getObjectType())) {
                        return null;
                    }
                }*/
                return identifierLookupAdapter.matches(this) ? this : null;
            }
        }
        return null;

    }

    @Override
    public void collectPsiElements(PsiLookupAdapter lookupAdapter, int scopeCrossCount, @NotNull Consumer<BasePsiElement> consumer) {
        if (lookupAdapter instanceof IdentifierLookupAdapter) {
            IdentifierLookupAdapter identifierLookupAdapter = (IdentifierLookupAdapter) lookupAdapter;
            if (identifierLookupAdapter.matchesName(this)) {
                if (lookupAdapter.matches(this)) {
                    consumer.accept(this);
                }
            }
        }
    }

    @Override
    public void collectSubjectPsiElements(@NotNull Consumer<IdentifierPsiElement> consumer) {
        if (elementType.is(ElementTypeAttribute.SUBJECT)) {
            consumer.accept(this);
        }
    }

    @Override
    public void collectExecVariablePsiElements(@NotNull Consumer<ExecVariablePsiElement> consumer) {
    }

    /*********************************************************
     *                     Miscellaneous                     *
     *********************************************************/

    public boolean isObject() {
        return elementType.isObject();
    }

    public boolean isAlias() {
        return elementType.isAlias();
    }

    public boolean isVariable() {
        return elementType.isVariable();
    }

    public boolean isDefinition() {
        return elementType.isDefinition();
    }

    public boolean isSubject() {
        return elementType.isSubject();
    }

    public boolean isReference() {
        return elementType.isReference();
    }
    
    public boolean isReferenceable() {
        return elementType.isReferenceable();
    }

    public boolean isObjectOfType(DBObjectType objectType) {
        return elementType.isObjectOfType(objectType);
    }

    public boolean isLocalReference() {
        return elementType.isLocalReference();
    }

    public boolean isQualifiedIdentifierMember() {
        return getParent() instanceof QualifiedIdentifierPsiElement;
    }

    @NotNull
    public DBObjectType getObjectType() {
        if (ref != null) {
            DBObjectType objectType = ref.getObjectType();
            if (objectType != DBObjectType.UNKNOWN) {
                return objectType;
            }
        }
        return elementType.getObjectType();
    }

    public String getObjectTypeName() {
        return elementType.getObjectTypeName();
    }

    /**
     * Looks-up whatever underlying database object may be referenced from this identifier.
     * - if this references to a synonym, the DBObject behind the synonym is returned.
     * - if this is an alias reference or definition, it returns the underlying DBObject of the aliased identifier.
     *
     * @return real underlying database object behind the identifier.
     */
    @Override
    @Nullable
    public DBObject getUnderlyingObject() {
        DBObject object = DBObjectRef.get(underlyingObject.get());
        if (object == null || underlyingObject.isOutdated(getSignature())) {
            underlyingObject.capture(getSignature(), () -> DBObjectRef.of(loadUnderlyingObject()));
            object = DBObjectRef.get(underlyingObject.get());
        }
        return unwrap(object);
    }

    private Object getSignature() {
        return ref == null ? getChars() : ref.getSignature();
    }


    private DBObject loadUnderlyingObject() {
        UnderlyingObjectResolver underlyingObjectResolver = elementType.getUnderlyingObjectResolver();
        if (underlyingObjectResolver != null) {
            return underlyingObjectResolver.resolve(this);
        }

        IdentifierPsiElement originalElement = (IdentifierPsiElement) getOriginalElement();
        PsiElement psiReferenceElement = originalElement.resolve();
        if (psiReferenceElement != null && psiReferenceElement != this) {
            if (psiReferenceElement instanceof DBObjectPsiElement) {
                DBObjectPsiElement objectPsiElement = (DBObjectPsiElement) psiReferenceElement;
                return objectPsiElement.getObject();
            }

            if (psiReferenceElement instanceof IdentifierPsiElement) {
                IdentifierPsiElement identifierPsiElement = (IdentifierPsiElement) psiReferenceElement;
                return identifierPsiElement.getUnderlyingObject();
            }
        }

        if (isAlias() && isDefinition()) {
            return AliasObjectResolver.getInstance().resolve(this);
        }

        DBObject underlyingObject = SurroundingVirtualObjectResolver.getInstance().resolve(this);
        if (underlyingObject != null) return underlyingObject;

        return null;
    }

    @Override
    public NamedPsiElement findNamedPsiElement(String id) {
        return null;
    }

    @Override
    public BasePsiElement findPsiElementBySubject(ElementTypeAttribute attribute, CharSequence subjectName, DBObjectType subjectType) {
        if (elementType.is(attribute) && elementType.is(ElementTypeAttribute.SUBJECT)) {
            if (subjectType == getObjectType() && Strings.equalsIgnoreCase(subjectName, this.getChars())) {
                return this;
            }
        }
        return null;
    }

    /********************************************************
     *                      Variant builders                *
     *******************************************************/

    private Object[] buildAliasRefVariants() {
        SequencePsiElement statement = findEnclosingElement(ElementTypeAttribute.STATEMENT);
        BasePsiElement sourceScope = getEnclosingScopeElement();
        DBObjectType objectType = getObjectType();
        PsiLookupAdapter lookupAdapter = LookupAdapters.aliasDefinition(objectType);
        ListCollector<BasePsiElement> consumer = ListCollector.basic();
        lookupAdapter.collectInScope(statement, consumer);

        return consumer.isEmpty() ? new Object[0] : consumer.elements().toArray();
    }

    /********************************************************
     *                      Rersolvers                      *
     ********************************************************/

    private void resolveWithinQualifiedIdentifierElement(QualifiedIdentifierPsiElement qualifiedIdentifier) {
        int index = qualifiedIdentifier.getIndexOf(this);

        BasePsiElement parentObjectElement = null;
        DBObject parentObject = null;
        if (index > 0) {
            IdentifierPsiElement parentElement = qualifiedIdentifier.getLeafAtIndex(index - 1);
            if (parentElement.resolve() != null) {
                parentObjectElement = parentElement.isObject() || parentElement.isVariable() ? parentElement : PsiUtil.resolveAliasedEntityElement(parentElement);
                parentObject = parentObjectElement != null ? parentElement.getUnderlyingObject() : null;
            } else {
                return;
            }
        }

        for (QualifiedIdentifierVariant parseVariant : qualifiedIdentifier.getParseVariants()) {
            LeafElementType leafElementType = parseVariant.getLeaf(index);

            if (leafElementType instanceof IdentifierElementType) {
                IdentifierElementType elementType = (IdentifierElementType) leafElementType;
                DBObjectType objectType = elementType.getObjectType();

                CharSequence refText = ref.getText();
                if (parentObject == null || parentObject == getFile().getUnderlyingObject()) {  // index == 0
                    if (elementType.isObject()) {
                        resolveWithScopeParentLookup(objectType, elementType);
                    } else if (elementType.isAlias()) {
                        PsiLookupAdapter lookupAdapter = LookupAdapters.aliasDefinition(this, objectType, refText);
                        BasePsiElement referencedElement = lookupAdapter.findInParentScopeOf(this);
                        if (updateReference(null, elementType, referencedElement)) return;

                    } else if (elementType.isVariable()) {
                        PsiLookupAdapter lookupAdapter = LookupAdapters.variableDefinition(this, DBObjectType.ANY, refText);
                        BasePsiElement referencedElement = lookupAdapter.findInParentScopeOf(this);
                        if (updateReference(null, elementType, referencedElement)) return;

                    }
                } else { // index > 0
                    IdentifierElementType parentElementType = (IdentifierElementType) parseVariant.getLeaf(index - 1);
                    if (parentObject.isOfType(parentElementType.getObjectType())) {
                        String objectName = refText.toString();
                        DBObject childObject = parentObject.getChildObject(objectType, objectName, false);

                        if (childObject == null && objectType.isOverloadable()) {
                            // TODO support multiple references in PsiResolveResult
                            childObject = parentObject.getChildObject(objectType, objectName, (short) 1, false);
                        }
                        if (updateReference(parentObjectElement, elementType, childObject)) return;

                    }
                }
            }
        }
    }

    private void resolveWithScopeParentLookup(DBObjectType objectType, IdentifierElementType elementType) {
        CharSequence refText = ref.getText();
        if (isPrecededByDot()) {
            LeafPsiElement prevLeaf = getPrevLeaf();
            if (prevLeaf != null) {
                LeafPsiElement parentPsiElement = prevLeaf.getPrevLeaf();
                if (parentPsiElement != null) {
                    DBObject object = parentPsiElement.getUnderlyingObject();
                    if (object != null && object != getFile().getUnderlyingObject()) {
                        DBObject referencedObject = object.getChildObject(objectType, refText.toString(), (short) 0, false);
                        if (updateReference(null, elementType, referencedObject)) return;
                    }
                }
            }
        }

        if (elementType.isObject()) {
            if (!elementType.isDefinition()){
                PsiLookupAdapter lookupAdapter = new ObjectDefinitionLookupAdapter(this, objectType, refText);
                PsiElement referencedElement = lookupAdapter.findInParentScopeOf(this);
                if (updateReference(null, elementType, referencedElement)) return;
            }

            if (elementType.isLocalReference()) {
                PsiLookupAdapter lookupAdapter = new ObjectReferenceLookupAdapter(this, objectType, refText);
                PsiElement referencedElement = lookupAdapter.findInParentScopeOf(this);
                if (updateReference(null, elementType, referencedElement)) return;
            }

            ConnectionHandler connection = getConnection();
            if (!elementType.isLocalReference() && isLiveConnection(connection)) {
                String objectName = refText.toString();
                Set<DBObject> parentObjects = identifyPotentialParentObjects(objectType, null, this, this);
                for (DBObject parentObject : parentObjects) {
                    parentObject = unwrap(parentObject);
                    if (parentObject == null) continue;

                    DBObject childObject = parentObject.getChildObject(objectType, objectName, false);
                    if (childObject == null && objectType.isOverloadable()) {
                        childObject = parentObject.getChildObject(objectType, objectName, (short) 1, false);
                    }

                    if (updateReference(null, elementType, childObject)) return;
                }

                DBObjectBundle objectBundle = connection.getObjectBundle();
                DBObject childObject = objectBundle.getObject(objectType, objectName, (short) 0);
                if (updateReference(null, elementType, childObject)) {
                    return;
                }

                DBSchema schema = getSchema();
                if (schema != null && objectType.isSchemaObject()) {
                    childObject = schema.getChildObject(objectType, objectName, false);

                    if (childObject == null && objectType.isOverloadable()) {
                        childObject = schema.getChildObject(objectType, objectName, (short) 1, false);
                    }

                    if (updateReference(null, elementType, childObject)) return;
                }
            }

        } else if (elementType.isAlias()) {
            PsiLookupAdapter lookupAdapter = LookupAdapters.aliasDefinition(this, objectType, refText);
            BasePsiElement referencedElement = lookupAdapter.findInParentScopeOf(this);
            updateReference(null, elementType, referencedElement);
        } else if (elementType.isVariable()) {
            if (elementType.isReference()) {
                PsiLookupAdapter lookupAdapter = LookupAdapters.variableDefinition(this, DBObjectType.ANY, refText);
                BasePsiElement referencedElement = lookupAdapter.findInParentScopeOf(this);
                updateReference(null, elementType, referencedElement);
            }
        }
    }

    public boolean isPrecededByDot() {
        LeafPsiElement prevLeaf = getPrevLeaf();
        if (prevLeaf instanceof TokenPsiElement) {
            TokenPsiElement tokenPsiElement = (TokenPsiElement) prevLeaf;
            return tokenPsiElement.getTokenType() == tokenPsiElement.getLanguage().getSharedTokenTypes().getChrDot();
        }
        return false;
    }

    private boolean updateReference(@Nullable BasePsiElement parent, IdentifierElementType elementType, DBObject referenceObject) {
        if (!isValidReference(referenceObject)) return false;

        ref.setParent(parent);
        ref.setReference(DBObjectPsiCache.asPsiElement(referenceObject));
        this.elementType = elementType;
        return true;
    }

    private boolean updateReference(@Nullable BasePsiElement parent, IdentifierElementType elementType, PsiElement referencedElement) {
        if (!isValidReference(referencedElement)) return false;

        ref.setParent(parent);
        ref.setReference(referencedElement);
        this.elementType = elementType;
        return true;
    }

    private boolean isValidReference(DBObject referencedObject) {
        if (referencedObject instanceof DBVirtualObject) {
            DBVirtualObject object = (DBVirtualObject) referencedObject;
            BasePsiElement underlyingPsiElement = Failsafe.guarded(null, object, o -> o.getUnderlyingPsiElement());
            if (underlyingPsiElement != null && underlyingPsiElement.containsPsiElement(this)) {
                return false;
            }
        }
        return referencedObject != null;
    }

    private boolean isValidReference(PsiElement referencedElement) {
        if (referencedElement == null || referencedElement == this) return false;

        // check if inside same scope
        if (referencedElement instanceof IdentifierPsiElement) {
            IdentifierPsiElement identifierPsiElement = (IdentifierPsiElement) referencedElement;
            if (identifierPsiElement.isReference() && identifierPsiElement.isReferenceable()) {
                return identifierPsiElement.getEnclosingScopeElement() == getEnclosingScopeElement();
            }
        }
        return true;
    }

    @NotNull
    @Override
    public PsiElement getNavigationElement() {
        return super.getNavigationElement();
    }

    /*********************************************************
     *                       PsiReference                    *
     ********************************************************/

    @Override
    @Nullable
    public PsiElement resolve() {
        if (isResolving()) return ref.getReference();

        // alias definitions do not have references.
        // underlying object is determined on runtime
        if (isDefinition() && (isAlias() || (isVariable() && !isSubject()))) return null;

        ConnectionHandler connection = getConnection();
        if ((connection == null || connection.isVirtual()) && isObject() && isDefinition()) return null;

        ref = nvl(ref, () -> new PsiResolveResult(this));

        if (isDispatchThread()) return ref.getReference();
        if (!ref.isDirty()) return ref.getReference();


        boolean cancelled = false;
        try {
            ref.preResolve();
            CharSequence text = ref.getText();
            if (text != null && text.length() > 0) {
                if (getParent() instanceof QualifiedIdentifierPsiElement) {
                    QualifiedIdentifierPsiElement qualifiedIdentifier = (QualifiedIdentifierPsiElement) getParent();
                    resolveWithinQualifiedIdentifierElement(qualifiedIdentifier);
                } else {
                    resolveWithScopeParentLookup(getObjectType(), elementType);
                }
            }
        } catch (ProcessCanceledException e){
            conditionallyLog(e);
            cancelled = true;
        } finally {
            ref.postResolve(cancelled);
        }

        return ref.getReference();
    }

    public void resolveAs(DBObject object) {
        ref = nvl(ref, () -> new PsiResolveResult(this));
        try {
            ref.preResolve();
            ref.setReference(DBObjectPsiCache.asPsiElement(object));
        } finally {
            ref.postResolve(false);
        }
    }

    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        return element != this && ref != null && element == ref.getReference();
    }

    public CharSequence getUnquotedText() {
        CharSequence text = getChars();
        if (isQuoted() && text.length() > 1) {
            return text.subSequence(1, text.length() - 1);
        }
        return text;
    }

    @Override
    public boolean textMatches(@Nullable CharSequence text) {
        if (text == null) return false;
        CharSequence chars = getChars();
        if (isQuoted())  {
            return chars.length() == text.length() + 2 && Strings.indexOfIgnoreCase(chars, text, 0) == 1;
        } else {
            return Strings.equalsIgnoreCase(chars, text);
        }
    }

    @Override
    public boolean isSoft() {
        return isDefinition();
    }

    @Override
    public boolean hasErrors() {
        return false;
    }

    @Override
    public boolean matches(BasePsiElement basePsiElement, MatchType matchType) {
        if (basePsiElement instanceof IdentifierPsiElement) {
            IdentifierPsiElement identifierPsiElement = (IdentifierPsiElement) basePsiElement;
            return matchType == MatchType.SOFT || Strings.equalsIgnoreCase(identifierPsiElement.getChars(), getChars());
        }

        return false;
    }

    public boolean isResolved() {
        return ref != null && ref.getReference() != null;
    }

    public boolean isResolving() {
        return ref != null && ref.isResolving();
    }

    public int getResolveAttempts() {
        return ref == null ? 0 : ref.getResolveAttempts();
    }

    public IdentifierType getIdentifierType() {
        return elementType.identifierType;
    }

    public void findQualifiedUsages(Consumer<BasePsiElement> consumer) {
        BasePsiElement scopePsiElement = getEnclosingScopeElement();
        if (scopePsiElement == null) return;

        IdentifierLookupAdapter identifierLookupAdapter = new IdentifierLookupAdapter(this, null, null, null, getChars());
        identifierLookupAdapter.collectInElement(scopePsiElement, basePsiElement -> {
            QualifiedIdentifierPsiElement qualifiedIdentifierPsiElement =
                    (QualifiedIdentifierPsiElement) basePsiElement.findEnclosingElement(QualifiedIdentifierPsiElement.class);

            if (qualifiedIdentifierPsiElement != null && qualifiedIdentifierPsiElement.getElementsCount() > 1) {
                consumer.accept(qualifiedIdentifierPsiElement);
            }
        });
    }

    @Nullable
    public QualifiedIdentifierPsiElement getParentQualifiedIdentifier() {
        return findEnclosingElement(QualifiedIdentifierPsiElement.class);
    }
}

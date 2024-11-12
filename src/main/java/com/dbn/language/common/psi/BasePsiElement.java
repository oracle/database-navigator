package com.dbn.language.common.psi;

import com.dbn.code.common.style.formatting.FormattingAttributes;
import com.dbn.code.common.style.formatting.FormattingDefinition;
import com.dbn.code.common.style.formatting.FormattingProviderPsiElement;
import com.dbn.common.dispose.AlreadyDisposedException;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.editor.BasicTextEditor;
import com.dbn.common.navigation.NavigationInstructions;
import com.dbn.common.ref.WeakRefCache;
import com.dbn.common.thread.Read;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.connection.context.DatabaseContextBase;
import com.dbn.database.interfaces.DatabaseCompatibilityInterface;
import com.dbn.editor.DatabaseFileEditorManager;
import com.dbn.editor.ddl.DDLFileEditor;
import com.dbn.editor.session.SessionBrowser;
import com.dbn.editor.session.ui.SessionBrowserForm;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.QuoteDefinition;
import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.element.util.IdentifierCategory;
import com.dbn.language.common.psi.lookup.ObjectLookupAdapter;
import com.dbn.language.common.psi.lookup.ObjectReferenceLookupAdapter;
import com.dbn.language.common.psi.lookup.PsiLookupAdapter;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectPsiElement;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.common.DBVirtualObject;
import com.dbn.object.factory.VirtualObjectFactory;
import com.dbn.object.type.DBObjectType;
import com.dbn.vfs.file.DBConsoleVirtualFile;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.dbn.vfs.file.DBSessionStatementVirtualFile;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.ide.util.EditSourceUtil;
import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.source.tree.FileElement;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.dbn.common.util.Unsafe.cast;

public abstract class BasePsiElement<T extends ElementTypeBase> extends ASTWrapperPsiElement implements DatabaseContextBase, ItemPresentation, FormattingProviderPsiElement {
    private static final WeakRefCache<BasePsiElement, DBVirtualObject> underlyingObjectCache = WeakRefCache.weakKey();
    private static final WeakRefCache<BasePsiElement, FormattingAttributes> formattingAttributesCache = WeakRefCache.weakKey();
    private static final WeakRefCache<BasePsiElement, BasePsiElement> enclosingScopePsiElements = WeakRefCache.weakKeyValue();

    // TODO: check if any other visitor relevant
    public static final PsiElementVisitors visitors = PsiElementVisitors.create(
            "SpellCheckingInspection",
            "ParserDiagnosticsUtil",
            "UpdateCopyrightAction");

    public T elementType;

    public enum MatchType {
        STRONG,
        CACHED,
        SOFT,
    }

    protected BasePsiElement(ASTNode node, T elementType) {
        super(node);
        this.elementType = elementType;
    }

    @Nullable
    public static BasePsiElement from(PsiElement element) {
        while (element != null) {
            if (element instanceof PsiFile) return null;
            if (element instanceof BasePsiElement) return (BasePsiElement) element;
            element = element.getParent();
        }
        return null;
    }

    @Override
    public PsiElement getParent() {
        ASTNode parentNode = getNode().getTreeParent();
        return parentNode == null ? null : parentNode.getPsi();
    }

    public FormattingAttributes getFormattingAttributes() {
        FormattingDefinition formatting = elementType.getFormatting();
        if (formatting == null) return null;

        return formattingAttributesCache.get(this, e -> {
            FormattingAttributes attributes = e.elementType.getFormatting().getAttributes();
            return FormattingAttributes.copy(attributes);
        });
    }

    @Override
    public FormattingAttributes getFormattingAttributesRecursive(boolean left) {
        FormattingAttributes formattingAttributes = getFormattingAttributes();
        if (formattingAttributes == null) {
            PsiElement psiElement = left ? getFirstChild() : getLastChild();
            if (psiElement instanceof BasePsiElement) {
                BasePsiElement basePsiElement = (BasePsiElement) psiElement;
                return basePsiElement.getFormattingAttributesRecursive(left);
            }
        }
        return formattingAttributes;
    }

    public boolean containsLineBreaks() {
        return Strings.containsLineBreak(getNode().getChars());
    }

    @Override
    public PsiElement getFirstChild() {
        ASTNode firstChildNode = getNode().getFirstChildNode();
        return firstChildNode == null ? null : firstChildNode.getPsi();
    }

    @Override
    public PsiElement getNextSibling() {
        ASTNode treeNext = getNode().getTreeNext();
        return treeNext == null ? null : treeNext.getPsi();
    }

    @Override
    public PsiFile getContainingFile() {
        return Read.call(this, e -> e.getSuperContainingFile());
    }

    private PsiFile getSuperContainingFile() {
        return super.getContainingFile();
    }

    @Override
    public BasePsiElement getOriginalElement() {
        PsiFile containingFile = getContainingFile();
        PsiFile originalFile = containingFile.getOriginalFile();
        if (originalFile == containingFile) {
            return this;
        }
        int startOffset = getTextOffset();

        PsiElement psiElement = originalFile.findElementAt(startOffset);
        while (psiElement != null) {
            int elementStartOffset = psiElement.getTextOffset();
            if (elementStartOffset < startOffset) {
                break;
            }
            if (psiElement instanceof BasePsiElement) {
                BasePsiElement basePsiElement = (BasePsiElement) psiElement;
                boolean isSameElement = basePsiElement.elementType == elementType;
                boolean isIdentifier = basePsiElement instanceof IdentifierPsiElement && this instanceof IdentifierPsiElement;
                if ((isSameElement || isIdentifier) && elementStartOffset == startOffset) {
                    return basePsiElement;
                }
            }
            psiElement = psiElement.getParent();
        }

        return this;
    }

    public boolean isOriginalElement() {
        PsiFile containingFile = getContainingFile();
        PsiFile originalFile = containingFile.getOriginalFile();
        return originalFile == containingFile;

    }

    public boolean isInjectedContext() {
        DBLanguagePsiFile file = getFile();
        return file.isInjectedContext();
    }

    public String getReferenceQualifiedName() {
        return isVirtualObject() ? "virtual " + elementType.virtualObjectType.getName() : "[unknown element]";
    }

    public abstract int approximateLength();

    @NotNull
    public DBLanguagePsiFile getFile() {
        PsiElement parent = getParent();
        while (parent != null) {
            if (parent instanceof DBLanguagePsiFile) return (DBLanguagePsiFile) parent;
            parent = parent.getParent();
        }
        throw new AlreadyDisposedException(null);
    }

    @Nullable
    public ConnectionHandler getConnection() {
        DBLanguagePsiFile file = Failsafe.guarded(null, this, e -> e.getFile());
        return file == null ? null : file.getConnection();
    }

    @Nullable
    public SchemaId getSchemaId() {
        ConnectionHandler connection = getConnection();
        if (connection == null) return null;

        DBLanguagePsiFile file = getFile();
        return file.getSchemaId();
    }

    public long getFileModificationStamp() {
        return getFile().getModificationStamp();
    }

    public String toString() {
        //return elementType.is(ElementTypeAttribute.SCOPE_DEMARCATION);
        return hasErrors() ?
                "[INVALID] " + elementType.getName() :
                elementType.getName() +
                        (elementType.scopeDemarcation ? " SCOPE_DEMARCATION" : "") +
                        (elementType.scopeIsolation ? " SCOPE_ISOLATION" : "");
    }

    @Override
    public void acceptChildren(@NotNull PsiElementVisitor visitor) {
        PsiElement psiChild = getFirstChild();
        if (psiChild == null) return;

        ASTNode child = psiChild.getNode();
        while (child != null) {
            PsiElement childPsi = child.getPsi();
            if (childPsi != null) {
                childPsi.accept(visitor);
            }
            child = child.getTreeNext();
        }
    }

    @NotNull
    @Override
    public SearchScope getUseScope() {
        return new LocalSearchScope(getFile());
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitors.isSupported(visitor)) {
            super.accept(visitor);
        }
    }

    @Override
    public String getText() {
        return Read.call(this, e -> e.getSuperText());
    }

    public CharSequence getChars() {
        return getNode().getChars();
    }

    private String getSuperText() {
        return super.getText();
    }


    @Override
    public PsiElement findElementAt(int offset) {
        return super.findElementAt(offset);
    }

    public PsiElement getLastChildIgnoreWhiteSpace() {
        PsiElement psiElement = this.getLastChild();
        while (psiElement instanceof PsiWhiteSpace) {
            psiElement = psiElement.getPrevSibling();
        }
        return psiElement;
    }

    @Nullable
    public BasePsiElement getPrevElement() {
        PsiElement preElement = getPrevSibling();
        while (preElement instanceof PsiWhiteSpace || preElement instanceof PsiComment) {
            preElement = preElement.getPrevSibling();
        }

        if (preElement instanceof BasePsiElement) {
            BasePsiElement previous = (BasePsiElement) preElement;
            while (previous.getLastChild() instanceof BasePsiElement) {
                previous = (BasePsiElement) previous.getLastChild();
            }
            return previous;
        }
        return null;
    }

    @Nullable
    public LeafPsiElement getPrevLeaf() {
        PsiElement previousElement = getPrevSibling();
        while (previousElement instanceof PsiWhiteSpace || previousElement instanceof PsiComment) {
            previousElement = previousElement.getPrevSibling();
        }

        // is first in parent
        if (previousElement == null) {
            PsiElement parent = getParent();
            if (parent instanceof BasePsiElement) {
                BasePsiElement basePsiElement = (BasePsiElement) parent;
                return basePsiElement.getPrevLeaf();
            }
        } else if (previousElement instanceof LeafPsiElement) {
            return (LeafPsiElement) previousElement;
        } else if (previousElement instanceof BasePsiElement) {
            BasePsiElement basePsiElement = (BasePsiElement) previousElement;
            PsiElement lastChild = basePsiElement.getLastChild();
            while (lastChild != null) {
                if (lastChild instanceof LeafPsiElement) {
                    return (LeafPsiElement) lastChild;
                }
                lastChild = lastChild.getLastChild();
            }
        }
        return null;
    }

    protected BasePsiElement getNextElement() {
        PsiElement nextElement = getNextSibling();
        while (nextElement instanceof PsiWhiteSpace || nextElement instanceof PsiComment || nextElement instanceof PsiErrorElement) {
            nextElement = nextElement.getNextSibling();
        }
        BasePsiElement next = (BasePsiElement) nextElement;
        while (next != null && next.getFirstChild() instanceof BasePsiElement) {
            next = (BasePsiElement) next.getFirstChild();
        }
        return next;
    }

    public boolean isVirtualObject() {
        return elementType.isVirtualObject();
    }

    @Override
    public void navigate(boolean requestFocus) {
        if (!isValid()) return;

        OpenFileDescriptor descriptor = (OpenFileDescriptor) EditSourceUtil.getDescriptor(this);
        if (descriptor == null) return;

        VirtualFile virtualFile = getFile().getVirtualFile();
        Project project = getProject();
        if (virtualFile == null) return;

        if (virtualFile instanceof DBSourceCodeVirtualFile) {
            DBSourceCodeVirtualFile sourceCodeFile = (DBSourceCodeVirtualFile) virtualFile;
            DBEditableObjectVirtualFile databaseFile = sourceCodeFile.getMainDatabaseFile();
            DatabaseFileEditorManager editorManager = DatabaseFileEditorManager.getInstance(project);
            if (!editorManager.isFileOpen(databaseFile)) {
                DBSchemaObject object = databaseFile.getObject();
                editorManager.openEditor(object, null, false, requestFocus);
            }
            BasicTextEditor textEditor = Editors.getTextEditor(sourceCodeFile);
            if (textEditor != null) {
                Editor editor = textEditor.getEditor();
                descriptor.navigateIn(editor);
                if (requestFocus) Editors.focusEditor(editor);
            }
            return;
        }

        if (virtualFile instanceof DBConsoleVirtualFile) {
            DBConsoleVirtualFile consoleVirtualFile = (DBConsoleVirtualFile) virtualFile;
            BasicTextEditor textEditor = Editors.getTextEditor(consoleVirtualFile);
            if (textEditor != null) {
                Editor editor = textEditor.getEditor();
                descriptor.navigateIn(editor);
                if (requestFocus) Editors.focusEditor(editor);
            }
            return;
        }

        if (virtualFile instanceof DBSessionStatementVirtualFile) {
            DBSessionStatementVirtualFile sessionBrowserStatementFile = (DBSessionStatementVirtualFile) virtualFile;
            SessionBrowser sessionBrowser = sessionBrowserStatementFile.getSessionBrowser();
            SessionBrowserForm editorForm = sessionBrowser.getBrowserForm();
            EditorEx viewer = editorForm.getDetailsForm().getCurrentSqlPanel().getViewer();
            if (viewer != null) {
                descriptor.navigateIn(viewer);
                if (requestFocus) Editors.focusEditor(viewer);
            }
            return;
        }

        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        FileEditor[] fileEditors = editorManager.getSelectedEditors();
        for (FileEditor fileEditor : fileEditors) {
            if (fileEditor instanceof DDLFileEditor) {
                DDLFileEditor textEditor = (DDLFileEditor) fileEditor;
                if (textEditor.getVirtualFile().equals(virtualFile)) {
                    Editor editor = textEditor.getEditor();
                    descriptor.navigateIn(editor);
                    if (requestFocus) Editors.focusEditor(editor);
                    return;
                }

            }
        }

        super.navigate(requestFocus);
    }

    public void navigateInEditor(@NotNull FileEditor fileEditor, NavigationInstructions instructions) {
        OpenFileDescriptor descriptor = (OpenFileDescriptor) EditSourceUtil.getDescriptor(this);
        if (descriptor == null) return;

        Editor editor = Editors.getEditor(fileEditor);
        if (editor == null) return;

        if (instructions.isScroll()) descriptor.navigateIn(editor);
        if (instructions.isFocus()) Editors.focusEditor(editor);
        //TODO instruction.isOpen();
    }

    /*********************************************************
     *                   Lookup routines                     *
     *********************************************************/
    public void collectObjectPsiElements(Set<DBObjectType> objectTypes, IdentifierCategory identifierCategory, Consumer<BasePsiElement> consume) {
        for (DBObjectType objectType : objectTypes) {
            PsiLookupAdapter lookupAdapter = new ObjectLookupAdapter(null, identifierCategory, objectType, null);
            lookupAdapter.collectInElement(this, consume);
        }
    }

    public void collectObjectReferences(DBObjectType objectType, Consumer<DBObject> consumer) {
        PsiLookupAdapter lookupAdapter = new ObjectReferenceLookupAdapter(null, objectType, null);
        lookupAdapter.collectInElement(this, basePsiElement -> {
            if (basePsiElement instanceof IdentifierPsiElement) {
                IdentifierPsiElement identifierPsiElement = (IdentifierPsiElement) basePsiElement;
                PsiElement reference = identifierPsiElement.resolve();
                if (reference instanceof DBObjectPsiElement) {
                    DBObjectPsiElement objectPsiElement = (DBObjectPsiElement) reference;
                    consumer.accept(objectPsiElement.ensureObject());
                }
            }
        });
    }

    public abstract @Nullable BasePsiElement findPsiElement(PsiLookupAdapter lookupAdapter, int scopeCrossCount);

    public abstract void collectPsiElements(PsiLookupAdapter lookupAdapter, int scopeCrossCount, @NotNull Consumer<BasePsiElement> consumer);

    public abstract void collectExecVariablePsiElements(@NotNull Consumer<ExecVariablePsiElement> consumer);

    public abstract void collectSubjectPsiElements(@NotNull Consumer<IdentifierPsiElement> consumer);


    public void collectVirtualObjectPsiElements(DBObjectType objectType, Consumer<BasePsiElement> consumer) {
        if (elementType.isVirtualObject()) {
            DBObjectType virtualObjectType = elementType.virtualObjectType;
            if (objectType == virtualObjectType) {
                consumer.accept(this);
            }
        }
    }

    public abstract NamedPsiElement findNamedPsiElement(String id);
    public abstract BasePsiElement findFirstPsiElement(ElementTypeAttribute attribute);
    public abstract BasePsiElement findFirstPsiElement(Class<? extends ElementType> clazz);
    public abstract BasePsiElement findFirstLeafPsiElement();
    public abstract BasePsiElement findPsiElementBySubject(ElementTypeAttribute attribute, CharSequence subjectName, DBObjectType subjectType);
    public abstract BasePsiElement findPsiElementByAttribute(ElementTypeAttribute attribute);


    public boolean containsPsiElement(BasePsiElement basePsiElement) {
        return this == basePsiElement;
    }

    /*********************************************************
     *                       ItemPresentation                *
     *********************************************************/

    @Nullable
    public <E extends BasePsiElement> E findEnclosingElement(ElementTypeAttribute attribute) {
        return findEnclosingElement(true, e -> e.elementType.is(attribute));
    }

    @Nullable
    public <E extends BasePsiElement> E findEnclosingVirtualObjectElement(DBObjectType objectType) {
        return findEnclosingElement(true, e -> e.getVirtualObjectType() == objectType);
    }

    @Nullable
    public NamedPsiElement findEnclosingNamedElement() {
        return findEnclosingElement(false, e -> e instanceof NamedPsiElement);
    }

    @Nullable
    public SequencePsiElement findEnclosingSequenceElement() {
        return findEnclosingElement(false, e -> e instanceof SequencePsiElement);
    }


    @Nullable
    public <E extends BasePsiElement> E getEnclosingScopeElement() {
        return cast(enclosingScopePsiElements.computeIfAbsent(this, e -> e.findEnclosingScopeElement()));
    }

    @Nullable
    private <E extends BasePsiElement> E findEnclosingScopeElement() {
        return findEnclosingElement(true, e -> e.isScopeBoundary());
    }

    @Nullable
    public <E extends BasePsiElement> E findEnclosingElement(Class<E> psiClass) {
        return findEnclosingElement(false, e -> psiClass.isAssignableFrom(e.getClass()));
    }

    @Nullable
    public <E extends BasePsiElement> E findEnclosingElement(boolean includeThis, Predicate<BasePsiElement<?>> predicate) {
        PsiElement element = includeThis ? this : getParent();
        while (element != null) {
            if (element instanceof PsiFile) break;
            if (element instanceof BasePsiElement) {
                BasePsiElement<?> basePsiElement = (BasePsiElement<?>) element;
                if (predicate.test(basePsiElement)) return (E) element;
            }
            element = element.getParent();
        }
        return null;
    }

    public boolean isParentOf(BasePsiElement basePsiElement) {
        PsiElement element = basePsiElement.getParent();
        while (element != null && !(element instanceof PsiFile)) {
            if (element == this) {
                return true;
            }
            element = element.getParent();
        }
        return false;
    }

    public boolean isMatchingScope(SequencePsiElement sourceScope) {
        return true;
       /* if (sourceScope == null) return true; // no scope constraints
        SequencePsiElement scope = getEnclosingScopePsiElement();
        return scope == sourceScope || scope.isParentOf(sourceScope);*/
    }

    public boolean isScopeBoundary() {
        return elementType.scopeDemarcation || elementType.scopeIsolation || getNode().getTreeParent() instanceof FileElement;
    }

    @Nullable
    public DBObjectType getVirtualObjectType() {
        return elementType.virtualObjectType;
    }

    /*********************************************************
     *                       ItemPresentation                *
     *********************************************************/
    @Override
    public String getPresentableText() {
        ElementType elementType = getSpecificElementType();
        return elementType.getDescription();
    }

    public ElementType getSpecificElementType() {
        return getSpecificElementType(false);
    }

    public ElementType getSpecificElementType(boolean override) {
        return resolveSpecificElementType(override);
    }

    protected ElementType resolveSpecificElementType(boolean override) {
        ElementType elementType = this.elementType;
        if (elementType.is(ElementTypeAttribute.GENERIC)) {

            BasePsiElement specificElement = override ?
                    findFirstPsiElement(ElementTypeAttribute.SPECIFIC_OVERRIDE) :
                    findFirstPsiElement(ElementTypeAttribute.SPECIFIC);
            if (specificElement != null) {
                elementType = specificElement.elementType;
            }
        }
        return elementType;
    }

    public boolean is(ElementTypeAttribute attribute) {
        if (elementType.is(attribute)) return true;

        if (attribute.isSpecific()) {
            ElementType specificElementType = getSpecificElementType();
            if (specificElementType != null) {
                return specificElementType.is(attribute);
            }
        }
        return false;
    }

    @Override
    @Nullable
    public String getLocationString() {
        return null;
    }

    @Override
    @Nullable
    public Icon getIcon(boolean open) {
        return getSpecificElementType().getIcon();
    }

    @Nullable
    public TextAttributesKey getTextAttributesKey() {
        return null;
    }

    public abstract boolean hasErrors();

    @Override
    @NotNull
    public DBLanguage getLanguage() {
        return getLanguageDialect().getBaseLanguage();
    }

    @NotNull
    public DBLanguageDialect getLanguageDialect() {
        return elementType.getLanguageDialect();
    }

    public abstract boolean matches(@Nullable BasePsiElement basePsiElement, MatchType matchType);

    public DBObject getUnderlyingObject() {
        if (!isVirtualObject()) return null;

        return underlyingObjectCache.compute(this, (k, v) -> {
            if (v != null && v.isValid()) return v;

            Project project = getProject();
            VirtualObjectFactory factory = VirtualObjectFactory.getInstance(project);
            return factory.createVirtualObject(k);
        });
    }

    public QuoteDefinition getIdentifierQuotes() {
        ConnectionHandler connection = getConnection();
        if (connection == null) return QuoteDefinition.DEFAULT_IDENTIFIER_QUOTE_DEFINITION;

        DatabaseCompatibilityInterface compatibility = connection.getCompatibilityInterface();
        return compatibility.getIdentifierQuotes();
    }

    public boolean matchesTextRange(TextRange textRange) {
        return Objects.equals(textRange, getTextRange());
    }

}

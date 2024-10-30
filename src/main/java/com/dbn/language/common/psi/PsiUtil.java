package com.dbn.language.common.psi;

import com.dbn.common.consumer.SetCollector;
import com.dbn.common.thread.Read;
import com.dbn.common.util.Documents;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.psi.lookup.IdentifierLookupAdapter;
import com.dbn.language.common.psi.lookup.LookupAdapters;
import com.dbn.language.common.psi.lookup.PsiLookupAdapter;
import com.dbn.object.DBSchema;
import com.dbn.object.type.DBObjectType;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.util.Unsafe.cast;

public class PsiUtil {

    public static DBSchema getDatabaseSchema(PsiElement psiElement) {
        DBSchema currentSchema = null;
        if (psiElement instanceof BasePsiElement) {
            BasePsiElement basePsiElement = (BasePsiElement) psiElement;
            currentSchema = basePsiElement.getSchema();
        }

        if (currentSchema != null) return currentSchema;

        VirtualFile virtualFile = getVirtualFileForElement(psiElement);
        if (virtualFile == null) return null;

        FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(psiElement.getProject());
        ConnectionHandler connection = contextManager.getConnection(virtualFile);
        if (connection == null) return null;

        SchemaId schemaId = contextManager.getDatabaseSchema(virtualFile);
        if (schemaId == null) return null;

        return connection.getSchema(schemaId);
    }

    @Nullable
    public static VirtualFile getVirtualFileForElement(@NotNull PsiElement psiElement) {
        if (isNotValid(psiElement)) return null;

        PsiFile psiFile = Read.call(psiElement, e -> e.getContainingFile().getOriginalFile());
        return psiFile.getVirtualFile();
    }

    @Nullable
    public static BasePsiElement resolveAliasedEntityElement(IdentifierPsiElement aliasElement) {
        PsiElement psiElement = aliasElement.isReference() ? aliasElement.resolve() : aliasElement; 
        if (psiElement instanceof BasePsiElement) {
            BasePsiElement basePsiElement = (BasePsiElement) psiElement;
            BasePsiElement scope = basePsiElement.findEnclosingNamedElement();

            DBObjectType objectType = aliasElement.getObjectType();
            BasePsiElement objectPsiElement = null;
            if (scope != null) {
                IdentifierLookupAdapter lookupInput = new IdentifierLookupAdapter(aliasElement, null, null, objectType, null);

                objectPsiElement = lookupInput.findInScope(scope);
                if (objectPsiElement == null) {
                    scope = scope.findEnclosingSequenceElement();
                    if (scope != null) {
                        objectPsiElement = lookupInput.findInScope(scope);
                    }
                }
            }

            if (objectPsiElement != null) {
                SetCollector<BasePsiElement> virtualObjectPsiElements = SetCollector.linked();
                scope.collectVirtualObjectPsiElements(objectType, virtualObjectPsiElements);
                for (BasePsiElement virtualObjectPsiElement : virtualObjectPsiElements.elements()) {
                    if (virtualObjectPsiElement.containsPsiElement(objectPsiElement))
                        return virtualObjectPsiElement;

                }
            }

            return objectPsiElement;

        }
        return null;
    }

    @Nullable
    public static IdentifierPsiElement lookupObjectPriorTo(@NotNull BasePsiElement element, DBObjectType objectType) {
        SequencePsiElement scope = element.findEnclosingSequenceElement();
        if (scope == null) return null;

        Iterator<PsiElement> children = PsiUtil.getChildrenIterator(scope);
        while (children.hasNext()) {
            PsiElement child = children.next();
            if (child instanceof BasePsiElement) {
                BasePsiElement basePsiElement = (BasePsiElement) child;
                PsiLookupAdapter lookupInput = LookupAdapters.object(objectType);
                BasePsiElement objectPsiElement = lookupInput.findInScope(basePsiElement);
                if (objectPsiElement instanceof IdentifierPsiElement) {
                    return (IdentifierPsiElement) objectPsiElement;
                }
            }
            if (child == element) break;
        }
        return null;
    }

    @Nullable
    public static ExecutablePsiElement lookupExecutableAtCaret(@NotNull Editor editor, boolean lenient) {
        // GTK: PsiElement psiElement = PsiFile.findElementA(offset)

        int offset = editor.getCaretModel().getOffset();

        PsiFile file = Documents.getFile(editor);
        if (file == null) return null;

        PsiElement current;
        if (lenient) {
            int lineStart = editor.getCaretModel().getVisualLineStart();
            int lineEnd = editor.getCaretModel().getVisualLineEnd();
            current = file.findElementAt(lineStart);
            while (ignore(current)) {
                offset = current.getTextOffset() + current.getTextLength();
                if (offset >= lineEnd) break;
                current = file.findElementAt(offset);
            }
        } else {
            current = file.findElementAt(offset);
        }

        if (current == null) return null;

        PsiElement parent = current.getParent();
        while (parent != null && !(parent instanceof PsiFile)) {
            if (parent instanceof ExecutablePsiElement){
                ExecutablePsiElement executable = (ExecutablePsiElement) parent;
                if (!executable.isNestedExecutable()) return executable;
            }
            parent = parent.getParent();
        }

        return null;
    }

    @Nullable
    public static BasePsiElement lookupElementAtOffset(@NotNull PsiFile file, ElementTypeAttribute typeAttribute, int offset) {
        PsiElement element = file.findElementAt(offset);
        while (element != null && !(element instanceof PsiFile)) {
            if (element instanceof BasePsiElement) {
                BasePsiElement basePsiElement = (BasePsiElement) element;
                if (basePsiElement.getElementType().is(typeAttribute)) {
                    return basePsiElement;
                }
            }
            element = element.getParent();
        }
        return null;
    }

/*    @Nullable
    public static LeafPsiElement lookupLeafBeforeOffset0(PsiFile file, int originalOffset) {
        int offset = originalOffset;
        if (offset > 0 && offset == file.getTextLength()) {
            offset--;
        }
        PsiElement element = file.findElementAt(offset);
        while (element != null && offset >= 0) {
            int elementEndOffset = element.getTextOffset() + element.getTextLength();
            PsiElement parent = element.getParent();
            if (elementEndOffset <= originalOffset && parent instanceof LeafPsiElement) {
                LeafPsiElement leafPsiElement = (LeafPsiElement) parent;
                if (leafPsiElement instanceof IdentifierPsiElement) {
                    if (elementEndOffset < originalOffset) {
                        return leafPsiElement;
                    }
                } else {
                    return (LeafPsiElement) parent;
                }
            }
            offset = element.getTextOffset() - 1;
            element = file.findElementAt(offset);
        }
        return null;
    }*/

    private static boolean ignore(PsiElement element) {
        return element instanceof PsiWhiteSpace || element instanceof PsiComment;
    }


    @Nullable
    public static LeafPsiElement lookupLeafAtOffset(@NotNull PsiFile file, int offset) {
        PsiElement element = file.findElementAt(offset);
        if (element != null && element.getParent() instanceof LeafPsiElement) {
            return (LeafPsiElement) element.getParent();
        }
        return null;
    }

    @Nullable
    public static LeafPsiElement lookupLeafBeforeOffset(@NotNull PsiFile file, int offset) {
        if (offset > 0) {
            offset--;
            PsiElement element = file.findElementAt(offset);
            while (element != null && offset >= 0) {
                if (element.getParent() instanceof LeafPsiElement) {
                    return (LeafPsiElement) element.getParent();
                }
                offset = element.getTextOffset() - 1;
                element = file.findElementAt(offset);
            }
        }
        return null;
    }

    public static void moveCaretOutsideExecutable(Editor editor) {
        ExecutablePsiElement executablePsiElement = lookupExecutableAtCaret(editor, false);
        if (executablePsiElement != null) {
            int offset = executablePsiElement.getTextOffset();
            editor.getCaretModel().moveToOffset(offset);
        }
    }

    private static Iterator<PsiElement> getChildrenIterator(@NotNull PsiElement element) {
        return new Iterator<>() {
            private PsiElement current = element.getFirstChild();

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public PsiElement next() {
                PsiElement oldCurrent = current;
                current = current.getNextSibling();
                return oldCurrent;
            }

            @Override
            public void remove() {

            }
        };
    }

    public static int getChildCount(@NotNull PsiElement element) {
        int count = 0;
        PsiElement current = element.getFirstChild();
        while (current != null) {
            count ++ ;
            current = current.getNextSibling();
        }
        return count;
    }

    public static PsiElement getNextSibling(@NotNull PsiElement psiElement) {
        PsiElement nextPsiElement = psiElement.getNextSibling();
        while (ignore(nextPsiElement)) {
            nextPsiElement = nextPsiElement.getNextSibling();
        }
        return nextPsiElement;
    }

    @Nullable
    public static PsiElement getFirstLeaf(@NotNull PsiElement psiElement) {
        PsiElement childPsiElement = psiElement.getFirstChild();
        if (isNotValid(childPsiElement)) return psiElement;
        if (ignore(childPsiElement)) return getNextLeaf(childPsiElement);
        return getFirstLeaf(childPsiElement);
    }

    @Nullable
    public static PsiElement getNextLeaf(@Nullable PsiElement psiElement) {
        if (isNotValid(psiElement)) return null;

        PsiElement nextElement = psiElement.getNextSibling();
        if (nextElement == null) return getNextLeaf(psiElement.getParent());
        if (ignore(nextElement)) return getNextLeaf(nextElement);

        return getFirstLeaf(nextElement);
    }

    @Nullable
    public static PsiFile getPsiFile(Editor editor) {
        if (isNotValid(editor)) return null;

        Project project = editor.getProject();
        if (isNotValid(project)) return null;

        Document document = editor.getDocument();
        return getPsiFile(project, document);
    }

    @Nullable
    public static PsiFile getPsiFile(Project project, Document document) {
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        return Read.call(documentManager, m -> m.getPsiFile(document));
    }


    @Nullable
    public static <T extends PsiFile> T getPsiFile(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        if (isNotValid(project)) return null;
        if (isNotValid(virtualFile)) return null;

        PsiManager psiManager = PsiManager.getInstance(project);
        return Read.call(() -> cast(psiManager.findFile(virtualFile)));
    }


    @Nullable
    public static BasePsiElement<?> getBasePsiElement(@Nullable PsiElement element) {
        while (element != null && !(element instanceof PsiFile)) {
            if (element instanceof BasePsiElement) {
                return (BasePsiElement<?>) element;
            }
            element = element.getParent();
        }

        return null;
    }

    @Nullable
    public static ElementType getElementType(PsiElement psiElement) {
        if (psiElement instanceof BasePsiElement) {
            BasePsiElement<?> basePsiElement = (BasePsiElement<?>) psiElement;
            return basePsiElement.getElementType();
        }
        return null;
    }

    public static Language getLanguage(@NotNull PsiElement element) {
        Language language = element.getLanguage();
        if (language instanceof DBLanguageDialect) {
            DBLanguageDialect languageDialect = (DBLanguageDialect) language;
            language = languageDialect.getBaseLanguage();
        }
        return language;
    }

    public static boolean isWhiteSpaceOrComment(PsiElement element) {
        return element instanceof PsiWhiteSpace || element instanceof PsiComment;
    }
}

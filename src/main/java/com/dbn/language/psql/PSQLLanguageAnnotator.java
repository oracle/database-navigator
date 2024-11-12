package com.dbn.language.psql;

import com.dbn.code.psql.color.PSQLTextAttributesKeys;
import com.dbn.code.sql.color.SQLTextAttributesKeys;
import com.dbn.connection.ConnectionHandler;
import com.dbn.editor.DBContentType;
import com.dbn.editor.code.SourceCodeManager;
import com.dbn.editor.code.options.CodeEditorGeneralSettings;
import com.dbn.language.common.DBLanguageAnnotator;
import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.navigation.NavigateToDefinitionAction;
import com.dbn.language.common.navigation.NavigateToObjectAction;
import com.dbn.language.common.navigation.NavigateToSpecificationAction;
import com.dbn.language.common.navigation.NavigationAction;
import com.dbn.language.common.navigation.NavigationGutterRenderer;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.common.psi.ChameleonPsiElement;
import com.dbn.language.common.psi.ExecutablePsiElement;
import com.dbn.language.common.psi.IdentifierPsiElement;
import com.dbn.language.common.psi.NamedPsiElement;
import com.dbn.language.common.psi.TokenPsiElement;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.type.DBObjectType;
import com.dbn.options.ProjectSettings;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.language.common.element.util.ElementTypeAttribute.OBJECT_DECLARATION;
import static com.dbn.language.common.element.util.ElementTypeAttribute.OBJECT_SPECIFICATION;
import static com.dbn.language.common.element.util.ElementTypeAttribute.ROOT;
import static com.dbn.language.common.element.util.ElementTypeAttribute.SUBJECT;
import static com.intellij.lang.annotation.HighlightSeverity.ERROR;

public class PSQLLanguageAnnotator extends DBLanguageAnnotator {

    protected boolean isSupported(PsiElement psiElement) {
        return psiElement instanceof ChameleonPsiElement ||
                psiElement instanceof TokenPsiElement ||
                psiElement instanceof IdentifierPsiElement ||
                psiElement instanceof NamedPsiElement;
    }

    @Override
    public void annotateElement(@NotNull PsiElement psiElement, @NotNull AnnotationHolder holder) {
        if (psiElement instanceof BasePsiElement) {
            BasePsiElement basePsiElement = (BasePsiElement) psiElement;

            ElementType elementType = basePsiElement.elementType;
            if (elementType.is(OBJECT_SPECIFICATION) || elementType.is(OBJECT_DECLARATION)) {
                annotateSpecDeclarationNavigable(basePsiElement, holder);
            }

            if (basePsiElement instanceof TokenPsiElement) {
                annotateFlavoredToken(cast(psiElement), holder);

            } else if (basePsiElement instanceof IdentifierPsiElement) {
                annotateIdentifier(cast(psiElement), holder);

            } else if (basePsiElement instanceof NamedPsiElement) {
                NamedPsiElement namedPsiElement = (NamedPsiElement) basePsiElement;
                if (namedPsiElement.hasErrors()) {
                    String message = "Invalid " + namedPsiElement.elementType.getDescription();
                    createAnnotation(holder, namedPsiElement, ERROR, null, message);
                }
            }

            if (basePsiElement instanceof ExecutablePsiElement) {
                annotateExecutable(cast(psiElement), holder);
            }
        } else if (psiElement instanceof ChameleonPsiElement) {
            createSilentAnnotation(holder, psiElement, SQLTextAttributesKeys.CHAMELEON);
        }
    }

    private static void annotateIdentifier(@NotNull IdentifierPsiElement identifierPsiElement, final AnnotationHolder holder) {
        ConnectionHandler connection = identifierPsiElement.getConnection();
        if (connection == null) return;
        if (connection.isVirtual()) return;
        if (identifierPsiElement.isInjectedContext()) return;

        if (identifierPsiElement.isReference()) {
            identifierPsiElement.resolve();
        }

        if (identifierPsiElement.isAlias()) {
            if (identifierPsiElement.isReference())
                annotateAliasRef(identifierPsiElement, holder); else
                annotateAliasDef(identifierPsiElement, holder);
        }

/*
        if (identifierPsiElement.isObject() && identifierPsiElement.isReference()) {
            annotateObject(identifierPsiElement, holder);
        } else

*/
    }

    private static void annotateAliasRef(IdentifierPsiElement aliasReference, @NotNull AnnotationHolder holder) {
        /*if (aliasReference.resolve() == null) {
            Annotation annotation = holder.createWarningAnnotation(aliasReference, "Unknown identifier");
            annotation.setTextAttributes(PSQLTextAttributesKeys.UNKNOWN_IDENTIFIER);
        } else {
            Annotation annotation = holder.createInfoAnnotation(aliasReference, null);
            annotation.setTextAttributes(PSQLTextAttributesKeys.ALIAS);
        }*/

        createSilentAnnotation(holder, aliasReference, PSQLTextAttributesKeys.ALIAS);
    }

    private static void annotateAliasDef(IdentifierPsiElement aliasDefinition, @NotNull AnnotationHolder holder) {
        /*Set<PsiElement> aliasDefinitions = new HashSet<PsiElement>();
        SequencePsiElement sourceScope = aliasDefinition.getEnclosingScopePsiElement();
        sourceScope.collectAliasDefinitionPsiElements(aliasDefinitions, aliasDefinition.getUnquotedText(), DBObjectType.ANY, null);
        if (aliasDefinitions.size() > 1) {
            holder.createWarningAnnotation(aliasDefinition, "Duplicate alias definition: " + aliasDefinition.getUnquotedText());
        }*/

        createSilentAnnotation(holder, aliasDefinition, SQLTextAttributesKeys.ALIAS);
    }

    private static void annotateObject(@NotNull IdentifierPsiElement objectReference, AnnotationHolder holder) {
        PsiElement reference = objectReference.resolve();
        /*ConnectionHandler connection = objectReference.getCache();
        if (reference == null && connection != null && connection.getConnectionStatus().isValid()) {
            Annotation annotation = holder.createErrorAnnotation(objectReference.getAstNode(),
                    "Unknown " + objectReference.getObjectTypeName());
            annotation.setTextAttributes(PSQLTextAttributesKeys.UNKNOWN_IDENTIFIER);
        }*/
    }

    private static void annotateSpecDeclarationNavigable(@NotNull BasePsiElement basePsiElement, AnnotationHolder holder) {
        if (basePsiElement.isInjectedContext()) return;

        BasePsiElement subjectPsiElement = basePsiElement.findFirstPsiElement(SUBJECT);
        if (subjectPsiElement instanceof IdentifierPsiElement) {
            IdentifierPsiElement identifierPsiElement = (IdentifierPsiElement) subjectPsiElement;
            DBObjectType objectType = identifierPsiElement.getObjectType();
            ElementType elementType = basePsiElement.elementType;

            if (identifierPsiElement.isObject() && objectType.getGenericType() == DBObjectType.METHOD) {

                DBContentType targetContentType =
                        elementType.is(OBJECT_DECLARATION) ? DBContentType.CODE_SPEC :
                        elementType.is(OBJECT_SPECIFICATION) ? DBContentType.CODE_BODY : null;

                if (targetContentType != null && identifierPsiElement.getFile() instanceof PSQLFile) {
                    PSQLFile file = (PSQLFile) identifierPsiElement.getFile();
                    DBSchemaObject object = (DBSchemaObject) file.getUnderlyingObject();
                    VirtualFile virtualFile = file.getVirtualFile();

                    ProjectSettings projectSettings = ProjectSettings.get(basePsiElement.getProject());
                    CodeEditorGeneralSettings codeEditorGeneralSettings = projectSettings.getCodeEditorSettings().getGeneralSettings();

                    if (codeEditorGeneralSettings.isShowSpecDeclarationNavigationGutter()) {
                        if (object == null || (virtualFile != null && virtualFile.isInLocalFileSystem())) {
                            ElementTypeAttribute targetAttribute =
                                    elementType.is(OBJECT_DECLARATION) ? OBJECT_SPECIFICATION :
                                    elementType.is(OBJECT_SPECIFICATION) ? OBJECT_DECLARATION : null;

                            if (targetAttribute != null) {
                                BasePsiElement rootPsiElement = identifierPsiElement.findEnclosingElement(ROOT);

                                BasePsiElement targetElement = rootPsiElement == null ? null :
                                        rootPsiElement.findPsiElementBySubject(targetAttribute,
                                                identifierPsiElement.getChars(),
                                                identifierPsiElement.getObjectType());

                                if (targetElement != null && targetElement.isValid()) {
                                    NavigationAction navigationAction = targetContentType == DBContentType.CODE_BODY ?
                                            new NavigateToDefinitionAction(null, targetElement, objectType) :
                                            new NavigateToSpecificationAction(null, targetElement, objectType);
                                    NavigationGutterRenderer gutterRenderer = new NavigationGutterRenderer(navigationAction, GutterIconRenderer.Alignment.RIGHT);
                                    createGutterAnnotation(holder, basePsiElement, gutterRenderer);
                                }
                            }
                        } else if (object.getContentType() == DBContentType.CODE_SPEC_AND_BODY) {
                            SourceCodeManager codeEditorManager = SourceCodeManager.getInstance(object.getProject());


                            BasePsiElement targetElement = codeEditorManager.getObjectNavigationElement(object, targetContentType, identifierPsiElement.getObjectType(), identifierPsiElement.getChars());
                            if (targetElement != null && targetElement.isValid()) {
                                NavigationAction navigationAction = targetContentType == DBContentType.CODE_BODY ?
                                        new NavigateToDefinitionAction(object, targetElement, objectType) :
                                        new NavigateToSpecificationAction(object, targetElement, objectType);
                                NavigationGutterRenderer gutterRenderer = new NavigationGutterRenderer(navigationAction, GutterIconRenderer.Alignment.RIGHT);

                                createGutterAnnotation(holder, basePsiElement, gutterRenderer);
                            }
                        }
                    }

                    if (codeEditorGeneralSettings.isShowObjectsNavigationGutter()) {
                        NavigateToObjectAction navigateToObjectAction = new NavigateToObjectAction(identifierPsiElement.getUnderlyingObject(), objectType);
                        NavigationGutterRenderer gutterRenderer = new NavigationGutterRenderer(navigateToObjectAction, GutterIconRenderer.Alignment.LEFT);
                        createGutterAnnotation(holder, basePsiElement, gutterRenderer);
                    }
                }
            }
        }
    }
}

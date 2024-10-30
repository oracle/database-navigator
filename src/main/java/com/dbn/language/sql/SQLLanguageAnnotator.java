package com.dbn.language.sql;

import com.dbn.code.sql.color.SQLTextAttributesKeys;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionHandlerStatus;
import com.dbn.language.common.DBLanguageAnnotator;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.psi.ChameleonPsiElement;
import com.dbn.language.common.psi.ExecutablePsiElement;
import com.dbn.language.common.psi.IdentifierPsiElement;
import com.dbn.language.common.psi.NamedPsiElement;
import com.dbn.language.common.psi.TokenPsiElement;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.connection.ConnectionHandler.isLiveConnection;
import static com.intellij.lang.annotation.HighlightSeverity.ERROR;
import static com.intellij.lang.annotation.HighlightSeverity.WARNING;

public class SQLLanguageAnnotator extends DBLanguageAnnotator {

    protected void annotateElement(@NotNull PsiElement psiElement, @NotNull AnnotationHolder holder) {
        if (psiElement instanceof ExecutablePsiElement) {
            annotateExecutable((ExecutablePsiElement) psiElement, holder);

        } else if (psiElement instanceof ChameleonPsiElement) {
            annotateChameleon(psiElement, holder);

        } else if (psiElement instanceof TokenPsiElement) {
            annotateFlavoredToken(cast(psiElement), holder);

        } else if (psiElement instanceof IdentifierPsiElement) {
            annotateIdentifier(cast(psiElement), holder);
        }

        if (psiElement instanceof NamedPsiElement) {
            NamedPsiElement namedPsiElement = (NamedPsiElement) psiElement;
            if (namedPsiElement.hasErrors()) {
                String message = "Invalid " + namedPsiElement.elementType.getDescription();
                createAnnotation(holder, namedPsiElement, ERROR, null, message);
            }
        }
    }

    protected boolean isSupported(PsiElement psiElement) {
        return psiElement instanceof ChameleonPsiElement ||
                psiElement instanceof TokenPsiElement ||
                psiElement instanceof IdentifierPsiElement ||
                psiElement instanceof NamedPsiElement;
    }

    private static void annotateIdentifier(@NotNull IdentifierPsiElement identifierPsiElement, final AnnotationHolder holder) {
        ConnectionHandler connection = identifierPsiElement.getConnection();
        if (connection == null) return;
        if (connection.isVirtual()) return;

        DBLanguageDialect languageDialect = identifierPsiElement.getLanguageDialect();
        if (languageDialect.isReservedWord(identifierPsiElement.getText())) {
            createSilentAnnotation(holder, identifierPsiElement, SQLTextAttributesKeys.IDENTIFIER);
        }
        if (identifierPsiElement.isObject()) {
            annotateObject(identifierPsiElement, holder);
        } else if (identifierPsiElement.isAlias()) {
            if (identifierPsiElement.isReference())
                annotateAliasRef(identifierPsiElement, holder); else
                annotateAliasDef(identifierPsiElement, holder);
        }
    }

    private static void annotateAliasRef(@NotNull IdentifierPsiElement aliasReference, AnnotationHolder holder) {
        if (aliasReference.resolve() == null && aliasReference.getResolveAttempts() > 3) {
            createAnnotation(holder, aliasReference, WARNING, SQLTextAttributesKeys.UNKNOWN_IDENTIFIER, "Unknown identifier");
        } else {
            createSilentAnnotation(holder, aliasReference, SQLTextAttributesKeys.ALIAS);
        }
    }

    private static void annotateAliasDef(IdentifierPsiElement aliasDefinition, @NotNull AnnotationHolder holder) {
        /*Set<BasePsiElement> aliasDefinitions = new HashSet<BasePsiElement>();
        BasePsiElement scope = aliasDefinition.getEnclosingScopePsiElement();
        scope.collectAliasDefinitionPsiElements(aliasDefinitions, aliasDefinition.getUnquotedText(), DBObjectType.ANY);
        if (aliasDefinitions.size() > 1) {
            holder.createWarningAnnotation(aliasDefinition, "Duplicate alias definition: " + aliasDefinition.getUnquotedText());
        }*/

        createSilentAnnotation(holder, aliasDefinition, SQLTextAttributesKeys.ALIAS);
    }

    private static void annotateObject(@NotNull IdentifierPsiElement objectReference, AnnotationHolder holder) {
        if (!objectReference.isResolving() && !objectReference.isDefinition()) {
            PsiElement reference = objectReference.resolve();
            if (reference == null && objectReference.getResolveAttempts() > 3 && checkConnection(objectReference)) {
                if (!objectReference.getLanguageDialect().getParserTokenTypes().isFunction(objectReference.getText())) {
                    createAnnotation(holder, objectReference, WARNING, SQLTextAttributesKeys.UNKNOWN_IDENTIFIER, "Unknown identifier");
                }
            }
        }
    }

    private static boolean checkConnection(@NotNull IdentifierPsiElement objectReference) {
        ConnectionHandler connection = objectReference.getConnection();
        return isLiveConnection(connection) &&
                connection.canConnect() &&
                connection.isValid() &&
                !connection.getConnectionStatus().is(ConnectionHandlerStatus.LOADING);
    }

    private static void annotateChameleon(PsiElement psiElement, AnnotationHolder holder) {
        ChameleonPsiElement executable = (ChameleonPsiElement) psiElement;
/*
        if (!executable.isNestedExecutable()) {
            StatementExecutionProcessor executionProcessor = executable.getExecutionProcessor();
            if (executionProcessor != null) {
                Annotation annotation = holder.createInfoAnnotation(psiElement, null);
                annotation.setGutterIconRenderer(new StatementGutterRenderer(executionProcessor));
            }
        }
*/
    }
}

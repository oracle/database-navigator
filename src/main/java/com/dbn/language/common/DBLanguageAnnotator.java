package com.dbn.language.common;

import com.dbn.code.sql.color.SQLTextAttributesKeys;
import com.dbn.common.compatibility.Compatibility;
import com.dbn.common.thread.ThreadMonitor;
import com.dbn.common.thread.ThreadProperty;
import com.dbn.language.common.psi.ExecutablePsiElement;
import com.dbn.language.common.psi.TokenPsiElement;
import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.connection.mapping.FileConnectionContextManager.hasConnectivityContext;
import static com.dbn.debugger.DatabaseDebuggerManager.isDebugConsole;
import static com.intellij.lang.annotation.HighlightSeverity.INFORMATION;

public abstract class DBLanguageAnnotator implements Annotator {

    /**
     * Token references may have specific flavor (e.g. keyword used as function).
     * This will adjust text attributes accordingly
     */
    protected static void annotateFlavoredToken(@NotNull TokenPsiElement tokenPsiElement, AnnotationHolder holder) {
        TokenTypeCategory flavor = tokenPsiElement.elementType.getFlavor();
        if (flavor == null) return;

        TextAttributesKey textAttributes = SQLTextAttributesKeys.IDENTIFIER;
        switch (flavor) {
            case DATATYPE: textAttributes = SQLTextAttributesKeys.DATA_TYPE; break;
            case FUNCTION: textAttributes = SQLTextAttributesKeys.FUNCTION; break;
            case KEYWORD: textAttributes = SQLTextAttributesKeys.KEYWORD; break;
            case LITERAL: textAttributes = SQLTextAttributesKeys.STRING; break;
        }
        createSilentAnnotation(holder, tokenPsiElement, textAttributes);
    }

    protected static void annotateExecutable(@NotNull ExecutablePsiElement executablePsiElement, AnnotationHolder holder) {
        if (executablePsiElement.isInjectedContext()) return;

        if (executablePsiElement.isNestedExecutable()) return;
        if (!executablePsiElement.isValid()) return;

        DBLanguagePsiFile psiFile = executablePsiElement.getFile();
        VirtualFile file = psiFile.getVirtualFile();
        if (file instanceof LightVirtualFile) return;
        if (isDebugConsole(file)) return;
        if (!hasConnectivityContext(file)) return;

        createGutterAnnotation(holder, executablePsiElement, executablePsiElement.getStatementGutterRenderer());
    }

    public final void annotate(@NotNull PsiElement psiElement, @NotNull AnnotationHolder holder) {
        if (!isSupported(psiElement)) return;

        ThreadMonitor.surround(
                psiElement.getProject(), null,
                ThreadProperty.CODE_ANNOTATING,
                () -> annotateElement(psiElement, holder));
    }

    protected abstract void annotateElement(PsiElement psiElement, AnnotationHolder holder);

    protected abstract boolean isSupported(PsiElement psiElement);


    protected static void createGutterAnnotation(AnnotationHolder holder, @Compatibility PsiElement element, GutterIconRenderer gutterRenderer) {
        holder.newSilentAnnotation(INFORMATION)
                .gutterIconRenderer(gutterRenderer)
                .create();
    }

    protected static void createSilentAnnotation(AnnotationHolder holder, @Compatibility PsiElement element, @Nullable TextAttributesKey attributes) {
        AnnotationBuilder builder = holder.newSilentAnnotation(INFORMATION);
        withTextAttributes(builder, attributes);
        builder.create();
    }

    protected static void createAnnotation(AnnotationHolder holder, @Compatibility PsiElement element, @NotNull HighlightSeverity severity, @Nullable TextAttributesKey attributes, String message) {
        AnnotationBuilder builder = holder.newAnnotation(severity, message).needsUpdateOnTyping(true);
        withTextAttributes(builder, attributes);
        builder.create();
    }

    private static void withTextAttributes(AnnotationBuilder builder, @Nullable TextAttributesKey attributes) {
        if (attributes != null) builder.textAttributes(attributes);
    }
}

package com.dbn.language.common.psi;

import com.dbn.code.common.style.options.CodeStyleCaseOption;
import com.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dbn.common.latent.Latent;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.util.Cloneable;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.execution.statement.StatementGutterRenderer;
import com.dbn.execution.statement.processor.StatementExecutionProcessor;
import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.impl.NamedElementType;
import com.dbn.object.type.DBObjectType;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import static com.dbn.common.util.Strings.cachedUpperCase;
import static com.dbn.language.common.element.util.ElementTypeAttribute.ACTION;
import static com.dbn.language.common.element.util.ElementTypeAttribute.COMPILABLE_BLOCK;
import static com.dbn.language.common.element.util.ElementTypeAttribute.DATA_DEFINITION;
import static com.dbn.language.common.element.util.ElementTypeAttribute.OBJECT_DECLARATION;
import static com.dbn.language.common.element.util.ElementTypeAttribute.QUERY;
import static com.dbn.language.common.element.util.ElementTypeAttribute.SCHEMA_CHANGE;
import static com.dbn.language.common.element.util.ElementTypeAttribute.SUBJECT;
import static com.dbn.language.common.element.util.ElementTypeAttribute.TRANSACTIONAL;
import static com.dbn.language.common.element.util.ElementTypeAttribute.TRANSACTIONAL_CANDIDATE;
import static com.dbn.language.common.element.util.ElementTypeAttribute.TRANSACTION_CONTROL;

public class ExecutablePsiElement extends NamedPsiElement implements Cloneable<ExecutablePsiElement> {
    private WeakRef<StatementExecutionProcessor> executionProcessor;
    private final Latent<SchemaId> contextSchema =                          Latent.mutable(() -> getFileModificationStamp(), () -> resolveContextSchema());
    private final Latent<ElementType> specificElementType =                 Latent.mutable(() -> getFileModificationStamp(), () -> resolveSpecificElementType(false));
    private final Latent<ElementType> specificOverrideElementType =         Latent.mutable(() -> getFileModificationStamp(), () -> resolveSpecificElementType(true));
    private final Latent<StatementGutterRenderer> statementGutterRenderer = Latent.mutable(() -> getFileModificationStamp(), () -> createStatementGutterRenderer());

    public String prepareStatementText(){
        PsiElement lastChild = getLastChild();
        while (lastChild != null && !(lastChild instanceof BasePsiElement)) {
            lastChild = lastChild.getPrevSibling();
        }
        BasePsiElement basePsiElement = (BasePsiElement) lastChild;
        String text = getText();
        if (basePsiElement != null && basePsiElement.elementType instanceof NamedElementType) {
            NamedElementType namedElementType = (NamedElementType) basePsiElement.elementType;
            if (namedElementType.isTruncateOnExecution()) {
                return text.substring(0, text.length() - basePsiElement.getTextLength());
            }
        }
        return text;
    }
    public ExecutablePsiElement(ASTNode astNode, NamedElementType elementType) {
        super(astNode, elementType);
    }

    @Override
    public ElementType getSpecificElementType() {
        return getSpecificElementType(false);
    }

    @Override
    public ElementType getSpecificElementType(boolean override) {
        return override ?
                specificOverrideElementType.get() :
                specificElementType.get();
    }

    public boolean isQuery() {
        return getSpecificElementType().is(QUERY);
    }

    public boolean isTransactional() {
        return is(TRANSACTIONAL) || getSpecificElementType().is(TRANSACTIONAL);
    }

    public boolean isTransactionalCandidate() {
        return is(TRANSACTIONAL_CANDIDATE) || getSpecificElementType().is(TRANSACTIONAL_CANDIDATE);
    }

    public boolean isTransactionControl() {
        return getSpecificElementType().is(TRANSACTION_CONTROL);
    }

    public boolean isSchemaChange() {
        return is(SCHEMA_CHANGE) ||
                getSpecificElementType(true).is(SCHEMA_CHANGE) ||
                getSpecificElementType(false).is(SCHEMA_CHANGE);
    }

    @Nullable
    public SchemaId getSchemaChangeTargetId() {
        BasePsiElement subjectPsiElement = findFirstPsiElement(SUBJECT);
        if (subjectPsiElement != null) {
            ConnectionHandler connection = getConnection();
            if (connection != null) {
                return connection.getSchemaId(subjectPsiElement.getText());
            }
        }
        return null;
    }

    @Nullable
    public ExecutablePsiElement resolveSchemaChangeExecutable() {
        PsiElement psiElement = getPrevSibling();
        while (psiElement != null && psiElement != this) {
            if (psiElement instanceof ExecutablePsiElement) {
                ExecutablePsiElement executablePsiElement = (ExecutablePsiElement) psiElement;
                if (executablePsiElement.isSchemaChange()) {
                    return executablePsiElement;
                }
            }
            psiElement = psiElement.getPrevSibling();
        }
        return null;
    }

    @Nullable
    private SchemaId resolveContextSchema() {
        ExecutablePsiElement executablePsiElement = resolveSchemaChangeExecutable();
        if (executablePsiElement != null) {
            return executablePsiElement.getSchemaChangeTargetId();
        }
        return null;
    }

    @NotNull
    private StatementGutterRenderer createStatementGutterRenderer() {
        return new StatementGutterRenderer(this);
    }

    public StatementGutterRenderer getStatementGutterRenderer() {
        return statementGutterRenderer.get();
    }

    public SchemaId getContextSchema() {
        return contextSchema.get();
    }



    public boolean isNestedExecutable() {
        PsiElement parent = getParent();
        while (parent != null && !(parent instanceof RootPsiElement)) {
            if (parent instanceof ExecutablePsiElement && parent.getTextOffset() != getTextOffset()) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    @Override
    public boolean hasErrors() {
        return false;
    }

    public StatementExecutionProcessor getExecutionProcessor() {
        return executionProcessor == null ? null : executionProcessor.get();
    }

    public void setExecutionProcessor(StatementExecutionProcessor executionProcessor) {
        this.executionProcessor = WeakRef.of(executionProcessor);
    }

    @Override
    public ExecutablePsiElement clone() {
        return (ExecutablePsiElement) super.clone();
    }



    /*********************************************************
     *                    ItemPresentation                   *
     *********************************************************/
    @Override
    public String getPresentableText() {
        ElementType elementType = getSpecificElementType();
        String subject = null;
        String action = "";
        String subjectType = "";
        if (is(DATA_DEFINITION)) {
            IdentifierPsiElement subjectPsiElement = (IdentifierPsiElement) findFirstPsiElement(SUBJECT);
            if (subjectPsiElement != null) {
                subject = subjectPsiElement.getUnquotedText().toString();
            }
            BasePsiElement actionPsiElement = findFirstPsiElement(ACTION);
            if (actionPsiElement != null) {
                action = actionPsiElement.getText() + " ";
                if (subjectPsiElement != null) {
                    BasePsiElement compilableBlockPsiElement = findFirstPsiElement(COMPILABLE_BLOCK);
                    if (compilableBlockPsiElement != null) {
                        DBObjectType objectType = subjectPsiElement.getObjectType();
                        subjectType = cachedUpperCase(objectType.getName()) + " ";
                        if (compilableBlockPsiElement.is(OBJECT_DECLARATION)) {
                            subjectType += "BODY ";
                        }
                    }
                }
            }
        } else {
            subject = createSubjectList();
        }
        if (subject != null && isValid()) {
            CodeStyleCaseSettings caseSettings = getLanguage().codeStyleSettings(getProject()).getCaseSettings();
            CodeStyleCaseOption keywordCaseOption = caseSettings.getKeywordCaseOption();
            CodeStyleCaseOption objectCaseOption = caseSettings.getObjectCaseOption();
            action = keywordCaseOption.format(action);
            subjectType = keywordCaseOption.format(subjectType);
            subject = objectCaseOption.format(subject);
            return elementType.getDescription() + " (" + action + subjectType + subject + ")";
        } else {
            return elementType.getDescription();
        }
    }

    @Override
    @Nullable
    public Icon getIcon(boolean open) {
        return super.getIcon(open);
    }

    @Override
    @Nullable
    public TextAttributesKey getTextAttributesKey() {
        return null;
    }
}

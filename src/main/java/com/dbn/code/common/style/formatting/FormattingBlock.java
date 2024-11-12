package com.dbn.code.common.style.formatting;

import com.dbn.code.common.style.options.CodeStyleFormattingSettings;
import com.dbn.code.common.style.options.DBLCodeStyleSettings;
import com.dbn.code.common.style.presets.CodeStyleDefaultPresets;
import com.dbn.code.common.style.presets.CodeStylePreset;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.PsiElementRef;
import com.dbn.language.common.SharedTokenTypeBundle;
import com.dbn.language.common.SimpleTokenType;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.impl.WrapperElementType;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.common.psi.ChameleonPsiElement;
import com.dbn.language.common.psi.IdentifierPsiElement;
import com.dbn.language.common.psi.NamedPsiElement;
import com.dbn.language.common.psi.PsiUtil;
import com.dbn.language.common.psi.TokenPsiElement;
import com.intellij.formatting.Alignment;
import com.intellij.formatting.Block;
import com.intellij.formatting.ChildAttributes;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.Wrap;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.impl.source.tree.TreeElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FormattingBlock implements Block {
    private final PsiElementRef psiElementRef;
    private final CodeStyleSettings codeStyleSettings;
    private final DBLCodeStyleSettings codeStyleCustomSettings;
    private static final List<Block> EMPTY_LIST = new ArrayList<>(0);
    private volatile List<Block> childBlocks;
    private final FormattingBlock parentBlock;
    private final int index;

    public FormattingBlock(
            CodeStyleSettings codeStyleSettings,
            DBLCodeStyleSettings codeStyleCustomSettings,
            PsiElement psiElement,
            FormattingBlock parentBlock,
            int index) {
        this.psiElementRef = PsiElementRef.of(psiElement);
        this.parentBlock = parentBlock;
        this.index = index;
        this.codeStyleSettings = codeStyleSettings;
        this.codeStyleCustomSettings = codeStyleCustomSettings;
    }

    private FormattingAttributes getFormattingAttributes() {
        PsiElement psiElement = getPsiElement();
        if (psiElement instanceof BasePsiElement) {
            BasePsiElement basePsiElement = (BasePsiElement) psiElement;
            return basePsiElement.getFormattingAttributes();
        }
        return null;
    }


    private Indent getIndentAttribute() {
        return (Indent) getAttribute(FormattingAttributes.Type.INDENT);
    }

    private Wrap getWrapAttribute() {
        return (Wrap) getAttribute(FormattingAttributes.Type.WRAP);
    }

    private Spacing getSpacingBeforeAttribute() {
        return (Spacing) getAttribute(FormattingAttributes.Type.SPACING_BEFORE);
    }

    private Spacing getSpacingAfterAttribute() {
        return (Spacing) getAttribute(FormattingAttributes.Type.SPACING_AFTER);
    }


    private Object getAttribute(FormattingAttributes.Type type) {
        PsiElement psiElement = getPsiElement();
        if (psiElement instanceof FormattingProviderPsiElement) {
            FormattingProviderPsiElement providerPsiElement = (FormattingProviderPsiElement) psiElement;
            FormattingAttributes attributes = providerPsiElement.getFormattingAttributes();
            Object attribute = FormattingAttributes.getAttribute(attributes, type);
            if (attribute != null) {
                return attribute;
            }

            if (type == FormattingAttributes.Type.SPACING_BEFORE || type == FormattingAttributes.Type.SPACING_AFTER) {
                PsiElement parent = providerPsiElement.getParent();
                PsiElement child = type.isLeft() ? parent.getFirstChild() : parent.getLastChild();
                if (child != providerPsiElement) {
                    attributes = providerPsiElement.getFormattingAttributesRecursive(type.isLeft());
                    attribute = FormattingAttributes.getAttribute(attributes, type);
                    if (attribute != null) {
                        return attribute;
                    }
                }
            }
        }
        return null;
    }


    @Override
    @Nullable
    public Indent getIndent() {
        PsiElement psiElement = getPsiElement();
        if (psiElement instanceof PsiComment) {
            return null;
        }

        if (psiElement.getParent() instanceof DBLanguagePsiFile) {
            return Indent.getAbsoluteNoneIndent();
        }

        Indent indent = getIndentAttribute();
        if (indent == null) {
            if (psiElement instanceof BasePsiElement) {
                BasePsiElement basePsiElement = (BasePsiElement) psiElement;
                BasePsiElement parentPsiElement = getParentPsiElement(basePsiElement);
                if (parentPsiElement != null && parentPsiElement.elementType instanceof WrapperElementType) {
                    WrapperElementType wrapperElementType = (WrapperElementType) parentPsiElement.elementType;
                    SharedTokenTypeBundle sharedTokenTypes = parentPsiElement.getLanguage().getSharedTokenTypes();
                    if (wrapperElementType.getBeginTokenElement().tokenType == sharedTokenTypes.getChrLeftParenthesis()) {
                        //FormattingBlock parentStatementBlock = getParentBlock(this, ElementTypeAttribute.STATEMENT);
                        //Indent parentStatementIndent = parentStatementBlock.getIndent();
                        //return Indent.getIndent(Indent.Type.SPACES, -1, false, false);
                        return Indent.getNoneIndent();
                    }
                }
            }

            return Indent.getNoneIndent();
        } else {
            return indent;
        }
    }

    @Override
    @Nullable
    public Wrap getWrap() {
        PsiElement psiElement = getPsiElement();
        if (psiElement instanceof PsiComment) {
            return null;
        }

        if (psiElement instanceof BasePsiElement) {
            BasePsiElement basePsiElement = (BasePsiElement) psiElement;

            Wrap wrap = getWrapAttribute();
            if (wrap != null) {
                return wrap;
            }

/*            FormattingAttributes attributes = getFormattingAttributes();
            if (attributes != null && attributes.getWrap() != null) {
                Wrap wrap = attributes.getWrap();

                if (wrap == CodeStylePreset.WRAP_IF_LONG && basePsiElement.lookupEnclosingNamedPsiElement().approximateLength() > codeStyleSettings.RIGHT_MARGIN) {
                    wrap = CodeStylePreset.WRAP_ALWAYS;
                }
                return wrap;
            }*/


            CodeStyleFormattingSettings formattingSettings = codeStyleCustomSettings.getFormattingSettings();
            CodeStylePreset codeStylePreset = formattingSettings.getPreset(basePsiElement);
            if (codeStylePreset != null) {
                return codeStylePreset.getWrap(basePsiElement, codeStyleSettings);
            }

            for (CodeStylePreset preset : CodeStyleDefaultPresets.PRESETS) {
                if (preset.accepts(basePsiElement)) {
                    return preset.getWrap(basePsiElement, codeStyleSettings);
                }
            }
        }


        return CodeStylePreset.WRAP_NONE;
    }

    @Override
    @Nullable
    public Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
        FormattingBlock leftBlock = (FormattingBlock) child1;
        FormattingBlock rightBlock = (FormattingBlock) child2;

        if (leftBlock == null) {
            return null;
        }

        PsiElement leftPsiElement = leftBlock.getPsiElement();
        PsiElement rightPsiElement = rightBlock.getPsiElement();

        if (leftPsiElement instanceof PsiComment || rightPsiElement instanceof PsiComment) {
            return null;
        }

        DBLanguage language = getLanguage();
        if (language != null) {

            // DOT, COMMA spacing
            SharedTokenTypeBundle sharedTokenTypes = language.getSharedTokenTypes();
            SimpleTokenType chrDot = sharedTokenTypes.getChrDot();
            SimpleTokenType chrComma = sharedTokenTypes.getChrComma();
            SimpleTokenType leftParenthesis = sharedTokenTypes.getChrLeftParenthesis();
            SimpleTokenType rightParenthesis = sharedTokenTypes.getChrRightParenthesis();

            if (is(leftPsiElement, chrDot)) return SpacingDefinition.NO_SPACE.getValue();
            if (is(rightPsiElement, chrDot)) return SpacingDefinition.NO_SPACE.getValue();

            if (is(leftPsiElement, chrComma)) return SpacingDefinition.MIN_ONE_SPACE.getValue();
            if (is(rightPsiElement, chrComma)) return SpacingDefinition.NO_SPACE.getValue();

            if (is(leftPsiElement, leftParenthesis)) return SpacingDefinition.NO_SPACE.getValue();
            if (is(rightPsiElement, rightParenthesis)) return SpacingDefinition.NO_SPACE.getValue();

        }

        Spacing spacingAfter = leftBlock.getSpacingAfterAttribute();
        if (spacingAfter != null) {
            return spacingAfter;
        }

        Spacing spacingBefore = rightBlock.getSpacingBeforeAttribute();
        if (spacingBefore != null) {
            return spacingBefore;
        }


        if (rightPsiElement instanceof BasePsiElement) {
            BasePsiElement rightBasePsiElement = (BasePsiElement) rightPsiElement;

            if (rightBasePsiElement instanceof TokenPsiElement || rightBasePsiElement instanceof NamedPsiElement) {
                return null;
            }


            CodeStyleFormattingSettings formattingSettings = codeStyleCustomSettings.getFormattingSettings();
            CodeStylePreset codeStylePreset = formattingSettings.getPreset(rightBasePsiElement);
            if (codeStylePreset != null) {
                return codeStylePreset.getSpacing(rightBasePsiElement, codeStyleSettings);
            }

            for (CodeStylePreset preset : CodeStyleDefaultPresets.PRESETS) {
                if (preset.accepts(rightBasePsiElement)) {
                    return preset.getSpacing(rightBasePsiElement, codeStyleSettings);
                }
            }
        }
        return SpacingDefinition.ONE_SPACE.getValue();
    }

    private static boolean is(PsiElement psiElement, TokenType tokenType) {
        if (psiElement instanceof TreeElement) {
            TreeElement treeElement = (TreeElement) psiElement;
            return treeElement.getElementType() == tokenType;
        }
        return false;
    }

    @Nullable
    private DBLanguage getLanguage() {
        PsiElement psiElement = getPsiElement();
        if (psiElement instanceof DBLanguagePsiFile) {
            DBLanguagePsiFile databasePsiFile = (DBLanguagePsiFile) psiElement;
            return databasePsiFile.getDBLanguage();
        }

        if (psiElement instanceof BasePsiElement) {
            BasePsiElement basePsiElement = (BasePsiElement) psiElement;
            return basePsiElement.getLanguage();
        }
        return null;
    }

    private static BasePsiElement getParentPsiElement(PsiElement psiElement) {
        PsiElement parentPsiElement = psiElement.getParent();
        if (parentPsiElement instanceof BasePsiElement) {
            return (BasePsiElement) parentPsiElement;
        }
        return null;
    }

    private static FormattingBlock getParentBlock(FormattingBlock block, ElementTypeAttribute typeAttribute) {
        if (block.parentBlock != null) {
            PsiElement psiElement = block.parentBlock.getPsiElement();
            if (psiElement instanceof BasePsiElement) {
                BasePsiElement basePsiElement = (BasePsiElement) psiElement;
                if (basePsiElement.elementType.is(typeAttribute)) {
                    return block.parentBlock;
                }
                return getParentBlock(block.parentBlock, typeAttribute);
            }
        }
        return null;
    }


    private static ElementType getParentElementType(PsiElement psiElement) {
        BasePsiElement parentPsiElement = getParentPsiElement(psiElement);
        if (parentPsiElement != null) {
            return parentPsiElement.elementType;
        }
        return null;
    }

    @Override
    @NotNull
    public TextRange getTextRange() {
        return getPsiElement().getTextRange();
    }

    @Override
    @NotNull
    public List<Block> getSubBlocks() {
        if (childBlocks == null) {
            synchronized (this) {
                if (childBlocks == null) {
                    initChildBlocks();
                }
            }
        }
        return childBlocks;
    }

    private void initChildBlocks() {
        PsiElement psiElement = getPsiElement();
        PsiElement child = psiElement.getFirstChild();
        while (child != null) {
            if (!(child instanceof PsiWhiteSpace) /*&& !(child instanceof PsiErrorElement)*/ && child.getTextLength() > 0) {
                if (childBlocks == null) childBlocks = new ArrayList<>();
                DBLCodeStyleSettings codeStyleCustomSettings = getCodeStyleSettings(child);
                FormattingBlock childBlock = new FormattingBlock(codeStyleSettings, codeStyleCustomSettings, child, this, index);
                childBlocks.add(childBlock);
            }
            child = child.getNextSibling();
        }

        if (childBlocks == null) childBlocks = EMPTY_LIST;
    }

    private DBLCodeStyleSettings getCodeStyleSettings(PsiElement child) {
        DBLCodeStyleSettings codeStyleCustomSettings = this.codeStyleCustomSettings;
        if (child instanceof ChameleonPsiElement) {
            ChameleonPsiElement element = (ChameleonPsiElement) child;
            DBLanguage language = (DBLanguage) PsiUtil.getLanguage(element);
            PsiElement psiElement = getPsiElement();
            codeStyleCustomSettings = language.codeStyleSettings(psiElement.getProject());
        }
        return codeStyleCustomSettings;
    }

    @Override
    @Nullable
    public Alignment getAlignment() {
        return Alignment.createAlignment();
    }

    @Override
    @NotNull
    public ChildAttributes getChildAttributes(final int newChildIndex) {
        List<Block> subBlocks = getSubBlocks();
        if (newChildIndex > subBlocks.size() - 1) {
            return new ChildAttributes(Indent.getNoneIndent(), Alignment.createAlignment());
        } else {
            Block child = getSubBlocks().get(newChildIndex);
            return new ChildAttributes(child.getIndent(), child.getAlignment());
        }
    }

    @Override
    public boolean isIncomplete() {
        PsiElement psiElement = getPsiElement();
        if (psiElement instanceof BasePsiElement) {
            BasePsiElement basePsiElement = (BasePsiElement) psiElement;
            return basePsiElement.hasErrors();
        }
        return false;
    }

    private boolean isPreviousIncomplete() {
        Block previous = getPreviousBlockInParent();
        return previous != null && previous.isIncomplete();
    }

    private boolean isParentIncomplete() {
        return parentBlock != null && parentBlock.isIncomplete();
    }

    @Override
    public boolean isLeaf() {
        PsiElement psiElement = getPsiElement();
        return psiElement instanceof IdentifierPsiElement ||
                psiElement instanceof TokenPsiElement ||
                psiElement instanceof PsiWhiteSpace;
    }


    public String toString() {
        return getPsiElement().toString();
    }


    @NotNull
    public PsiElement getPsiElement() {
        return psiElementRef.ensure();
    }

    private Block getPreviousBlockInParent() {
        if (parentBlock != null) {
            return index > 0 ? parentBlock.getSubBlocks().get(index - 1) : null;
        }
        return null;
    }
}

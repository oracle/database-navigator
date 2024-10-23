package com.dbn.language.common.parameter;

import com.dbn.code.common.style.options.CodeStyleCaseOption;
import com.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dbn.code.psql.style.PSQLCodeStyle;
import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.impl.IterationElementType;
import com.dbn.language.common.element.impl.TokenElementType;
import com.dbn.language.common.element.impl.WrapperElementType;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.common.psi.PsiUtil;
import com.dbn.object.DBColumn;
import com.dbn.object.common.DBObject;
import com.intellij.lang.parameterInfo.CreateParameterInfoContext;
import com.intellij.lang.parameterInfo.ParameterInfoHandler;
import com.intellij.lang.parameterInfo.ParameterInfoUIContext;
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ColumnParameterInfoHandler implements ParameterInfoHandler<BasePsiElement, BasePsiElement> {

    @Nullable
    @Override
    public BasePsiElement findElementForParameterInfo(@NotNull CreateParameterInfoContext context) {
        BasePsiElement handlerPsiElement = lookupHandlerElement(context.getFile(), context.getOffset());
        BasePsiElement providerPsiElement = lookupProviderElement(handlerPsiElement);
        if (handlerPsiElement != null && providerPsiElement != null) {
            context.setItemsToShow(new Object[]{providerPsiElement});

            int offset = context.getOffset();
            BasePsiElement iterationPsiElement = handlerPsiElement.findFirstPsiElement(IterationElementType.class);
            if (iterationPsiElement != null) {
                IterationElementType iterationElementType = (IterationElementType) iterationPsiElement.getElementType();
                PsiElement paramPsiElement = iterationPsiElement.getFirstChild();
                BasePsiElement iteratedPsiElement = null;
                while (paramPsiElement != null) {
                    ElementType elementType = PsiUtil.getElementType(paramPsiElement);
                    if (elementType instanceof TokenElementType) {
                        TokenElementType tokenElementType = (TokenElementType) elementType;
                        if (iterationElementType.isSeparator(tokenElementType.getTokenType())){
                            if (paramPsiElement.getTextOffset() >= offset) {
                                break;
                            }
                        }
                    }
                    if (elementType == iterationElementType.getIteratedElementType()) {
                        iteratedPsiElement = (BasePsiElement) paramPsiElement;
                    }

                    paramPsiElement = paramPsiElement.getNextSibling();
                }
                return iteratedPsiElement;
            } else {
                return handlerPsiElement;
            }
        }
        return null;
    }


    @Nullable
    private static BasePsiElement lookupHandlerElement(PsiFile file, int offset) {
        if (file != null) {
            PsiElement psiElement = file.findElementAt(offset);
            while (psiElement != null && !(psiElement instanceof PsiFile)) {
                if (psiElement instanceof BasePsiElement) {
                    ElementType elementType = PsiUtil.getElementType(psiElement);
                    if (elementType instanceof WrapperElementType) {
                        WrapperElementType wrapperElementType = (WrapperElementType) elementType;
                        if (wrapperElementType.is(ElementTypeAttribute.COLUMN_PARAMETER_HANDLER)) {
                            return (BasePsiElement) psiElement;
                        } else {
                            return null;
                        }
                    }
                }
                psiElement = psiElement.getParent();
            }
        }
        return null;
    }

    @Nullable
    private static BasePsiElement lookupProviderElement(@Nullable BasePsiElement handlerPsiElement) {
        if (handlerPsiElement != null) {
            BasePsiElement statementPsiElement = handlerPsiElement.findEnclosingElement(ElementTypeAttribute.STATEMENT);
            if (statementPsiElement != null) {
                return statementPsiElement.findFirstPsiElement(ElementTypeAttribute.COLUMN_PARAMETER_PROVIDER);
            }
        }
        return null;
    }

    @Override
    public void showParameterInfo(@NotNull BasePsiElement element, @NotNull CreateParameterInfoContext context) {
        context.showHint(element, context.getOffset(), this);
    }

    @Nullable
    @Override
    public BasePsiElement findElementForUpdatingParameterInfo(@NotNull UpdateParameterInfoContext context) {
        int offset = context.getOffset();
        BasePsiElement handlerPsiElement = lookupHandlerElement(context.getFile(), offset);
        if (handlerPsiElement != null) {
            BasePsiElement iterationPsiElement = handlerPsiElement.findFirstPsiElement(IterationElementType.class);
            if (iterationPsiElement != null) {
                IterationElementType iterationElementType = (IterationElementType) iterationPsiElement.getElementType();
                PsiElement paramPsiElement = iterationPsiElement.getFirstChild();
                int paramIndex = -1;
                BasePsiElement iteratedPsiElement = null;
                while (paramPsiElement != null) {
                    ElementType elementType = PsiUtil.getElementType(paramPsiElement);
                    if (elementType instanceof TokenElementType) {
                        TokenElementType tokenElementType = (TokenElementType) elementType;
                        if (iterationElementType.isSeparator(tokenElementType.getTokenType())){
                            if (paramPsiElement.getTextOffset() >= offset) {
                                break;
                            }
                        }
                    } else if (paramPsiElement instanceof BasePsiElement) {
                        iteratedPsiElement = (BasePsiElement) paramPsiElement;
                        paramIndex++;
                    }

                    paramPsiElement = paramPsiElement.getNextSibling();
                }
                context.setCurrentParameter(paramIndex);
                return iteratedPsiElement == null ? handlerPsiElement : iteratedPsiElement;
            } else {
                return handlerPsiElement;
            }


        }
        return null;
    }

    @Override
    public void updateParameterInfo(@NotNull BasePsiElement parameter, @NotNull UpdateParameterInfoContext context) {
        BasePsiElement wrappedPsiElement = getWrappedPsiElement(context);
        if (wrappedPsiElement != null) {
            IterationElementType iterationElementType = (IterationElementType) wrappedPsiElement.getElementType();
            int index = 0;
            PsiElement paramPsiElement = wrappedPsiElement.getFirstChild();
            while (paramPsiElement != null) {
                ElementType elementType = PsiUtil.getElementType(paramPsiElement);
                if (elementType == iterationElementType.getIteratedElementType()) {
                    if (paramPsiElement == parameter) {
                        context.setCurrentParameter(index);
                        return;
                    }
                    index++;
                }
                paramPsiElement = paramPsiElement.getNextSibling();
            }
            context.setCurrentParameter(index);
        }
    }

    public static BasePsiElement getWrappedPsiElement(UpdateParameterInfoContext context) {
        BasePsiElement basePsiElement = lookupHandlerElement(context.getFile(), context.getOffset());
        if (basePsiElement != null) {
            return basePsiElement.findFirstPsiElement(IterationElementType.class);
        }
        return null;
    }

    @Override
    public void updateUI(BasePsiElement handlerPsiElement, @NotNull ParameterInfoUIContext context) {
        if (handlerPsiElement.isValid()) {
            Project project = handlerPsiElement.getProject();
            CodeStyleCaseSettings caseSettings = PSQLCodeStyle.caseSettings(project);
            CodeStyleCaseOption datatypeCaseOption = caseSettings.getDatatypeCaseOption();
            CodeStyleCaseOption objectCaseOption = caseSettings.getObjectCaseOption();


            context.setUIComponentEnabled(true);
            StringBuilder text = new StringBuilder();
            int highlightStartOffset = 0;
            int highlightEndOffset = 0;
            int index = 0;
            int currentIndex = context.getCurrentParameterIndex();
            BasePsiElement iterationPsiElement = handlerPsiElement.findFirstPsiElement(IterationElementType.class);
            if (iterationPsiElement != null && iterationPsiElement.isValid()) {
                IterationElementType iterationElementType = (IterationElementType) iterationPsiElement.getElementType();
                PsiElement child = iterationPsiElement.getFirstChild();
                while (child != null) {
                    if (child instanceof BasePsiElement) {
                        BasePsiElement basePsiElement = (BasePsiElement) child;
                        if (basePsiElement.getElementType() == iterationElementType.getIteratedElementType()) {
                            boolean highlight = index == currentIndex || (index == 0 && currentIndex == -1);
                            if (highlight) {
                                highlightStartOffset = text.length();
                            }
                            if (text.length() > 0) {
                                text.append(", ");
                            }
                            text.append(datatypeCaseOption.format(basePsiElement.getText()));
                            DBObject object = basePsiElement.getUnderlyingObject();
                            if (object instanceof DBColumn) {
                                DBColumn column = (DBColumn) object;
                                String columnType = column.getDataType().getName();
                                text.append(" ");
                                text.append(objectCaseOption.format(columnType));
                            }

                            if (highlight) {
                                highlightEndOffset = text.length();
                            }
                            index++;
                        }
                    }

                    child = child.getNextSibling();
                }
            }



/*        for (DBArgument argument : providerPsiElement.getArguments()) {
            if (argument != providerPsiElement.getReturnArgument()) {
                boolean highlight = index == currentIndex || (index == 0 && currentIndex == -1);
                if (highlight) {
                    highlightStartOffset = text.length();
                }
                if (text.length() > 0) {
                    text.append(", ");
                }
                text.append(argument.getName().toLowerCase());
                //text.append(" ");
                //text.append(argument.getDataType().getQualifiedName());
                if (highlight) {
                    highlightEndOffset = text.length();
                }
                index++;
            }
        }*/
            boolean disable = highlightEndOffset == 0 && currentIndex > -1 && text.length() > 0;
            if (text.length() == 0) {
                text.append("<no parameters>");
            }
            context.setupUIComponentPresentation(text.toString(), highlightStartOffset, highlightEndOffset, disable, false, false, context.getDefaultParameterColor());
        }
    }

    @Override
    public void processFoundElementForUpdatingParameterInfo(@Nullable BasePsiElement basePsiElement, @NotNull UpdateParameterInfoContext context) {
        context.setParameterOwner(basePsiElement);
    }
}

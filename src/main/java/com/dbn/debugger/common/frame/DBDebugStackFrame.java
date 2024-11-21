/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.debugger.common.frame;

import com.dbn.code.common.style.DBLCodeStyleManager;
import com.dbn.code.common.style.options.CodeStyleCaseOption;
import com.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dbn.common.consumer.ListCollector;
import com.dbn.common.icon.Icons;
import com.dbn.common.latent.Latent;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Strings;
import com.dbn.debugger.DBDebugUtil;
import com.dbn.debugger.common.process.DBDebugProcess;
import com.dbn.editor.code.SourceCodeManager;
import com.dbn.execution.ExecutionTarget;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.common.psi.IdentifierPsiElement;
import com.dbn.language.common.psi.PsiUtil;
import com.dbn.language.psql.PSQLLanguage;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.type.DBObjectType;
import com.dbn.vfs.DBVirtualFile;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.ui.ColoredTextContainer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.xdebugger.XDebuggerBundle;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XValueChildrenList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dbn.common.util.Strings.cachedUpperCase;
import static com.dbn.common.util.Strings.toLowerCase;

@Slf4j
@Getter
public abstract class DBDebugStackFrame<P extends DBDebugProcess, V extends DBDebugValue> extends XStackFrame {
    private final P debugProcess;
    private final int frameIndex;
    private Map<String, V> valuesMap;

    private final Latent<VirtualFile> virtualFile = Latent.basic(() -> resolveVirtualFile());
    private final Latent<XSourcePosition> sourcePosition = Latent.basic(() -> resolveSourcePosition());
    private final Latent<IdentifierPsiElement> subject = Latent.basic(() -> resolveSubject());

    public DBDebugStackFrame(P debugProcess, int frameIndex) {
        this.debugProcess = debugProcess;
        this.frameIndex = frameIndex;
    }

    @Nullable
    private IdentifierPsiElement resolveSubject() {

        XSourcePosition sourcePosition = getSourcePosition();
        VirtualFile virtualFile = getVirtualFile();
        if (virtualFile == null) return null;

        Project project = getProject();
        if (virtualFile instanceof DBEditableObjectVirtualFile) {
            DBEditableObjectVirtualFile databaseFile = (DBEditableObjectVirtualFile) virtualFile;
            SourceCodeManager sourceCodeManager = SourceCodeManager.getInstance(project);
            sourceCodeManager.ensureSourcesLoaded(databaseFile.getObject(), true);
        }

        Document document = Documents.getDocument(virtualFile);
        DBLanguagePsiFile psiFile = (DBLanguagePsiFile) PsiUtil.getPsiFile(project, document);
        if (sourcePosition == null || psiFile == null || document == null) return null;

        int line = sourcePosition.getLine();
        if (document.getLineCount() <= line) return null;

        int offset = document.getLineEndOffset(line);
        PsiElement elementAtOffset = psiFile.findElementAt(offset);
        while (elementAtOffset instanceof PsiWhiteSpace || elementAtOffset instanceof PsiComment) {
            elementAtOffset = elementAtOffset.getNextSibling();
        }

        if (elementAtOffset instanceof BasePsiElement) {
            BasePsiElement basePsiElement = (BasePsiElement) elementAtOffset;
            BasePsiElement objectDeclarationPsiElement = basePsiElement.findEnclosingElement(ElementTypeAttribute.OBJECT_DECLARATION);
            if (objectDeclarationPsiElement != null) {
                return (IdentifierPsiElement) objectDeclarationPsiElement.findFirstPsiElement(ElementTypeAttribute.SUBJECT);
            }
        }

        return null;
    }

    protected abstract XSourcePosition resolveSourcePosition();

    protected abstract VirtualFile resolveVirtualFile();

    @NotNull
    @Override
    public abstract XDebuggerEvaluator getEvaluator();

    @Nullable
    @Override
    public final XSourcePosition getSourcePosition() {
        return sourcePosition.get();
    }

    @Nullable
    public final VirtualFile getVirtualFile() {
        return virtualFile.get();
    }

    public IdentifierPsiElement getSubject() {
        return subject.get();
    }

    public V getValue(String variableName) {
        return valuesMap == null ? null : valuesMap.get(toLowerCase(variableName));
    }

    public void setValue(String variableName, V value) {
        if (valuesMap == null) {
            valuesMap = new HashMap<>();
        }
        valuesMap.put(toLowerCase(variableName), value);
    }

    @Nullable
    protected abstract V createSuspendReasonDebugValue();

    @NotNull
    public abstract V createDebugValue(String variableName, V parentValue, List<String> childVariableNames, Icon icon);

    @Override
    public void computeChildren(@NotNull XCompositeNode node) {
        valuesMap = new HashMap<>();
        List<DBDebugValue> values = new ArrayList<>();

        V frameInfoValue = createSuspendReasonDebugValue();
        if (frameInfoValue != null) {
            values.add(frameInfoValue);
            valuesMap.put(frameInfoValue.getName(), frameInfoValue);
        }

        computeValues(values);

        Collections.sort(values);
        XValueChildrenList children = new XValueChildrenList();
        for (DBDebugValue value : values) {
            children.add(value.getVariableName(), value);
        }
        node.addChildren(children, true);
    }

    private void computeValues(List<DBDebugValue> values) {
        XSourcePosition sourcePosition = getSourcePosition();
        VirtualFile virtualFile = DBDebugUtil.getSourceCodeFile(sourcePosition);
        if (virtualFile == null) return;


        if (virtualFile instanceof DBEditableObjectVirtualFile) {
            virtualFile = ((DBEditableObjectVirtualFile) virtualFile).getMainContentFile();
        }

        Document document = Documents.getDocument(virtualFile);
        if (document == null) return;

        Project project = getProject();
        DBLanguagePsiFile psiFile = PsiUtil.getPsiFile(project, virtualFile);
        if (psiFile == null) return;

        int offset = document.getLineStartOffset(sourcePosition.getLine());
        CodeStyleCaseSettings codeStyleCaseSettings = DBLCodeStyleManager.getInstance(psiFile.getProject()).getCodeStyleCaseSettings(PSQLLanguage.INSTANCE);
        CodeStyleCaseOption objectCaseOption = codeStyleCaseSettings.getObjectCaseOption();

        psiFile.lookupVariableDefinition(offset, basePsiElement -> {
            String variableName = objectCaseOption.format(basePsiElement.getText());
            //DBObject object = basePsiElement.resolveUnderlyingObject();

            ListCollector<String> childVariableNames = ListCollector.unique();
            if (basePsiElement instanceof IdentifierPsiElement) {
                IdentifierPsiElement identifierPsiElement = (IdentifierPsiElement) basePsiElement;
                identifierPsiElement.findQualifiedUsages(qualifiedUsage -> {
                    String childVariableName = objectCaseOption.format(qualifiedUsage.getText());
                    childVariableNames.accept(childVariableName);
                });
            }

            String valueCacheKey = cachedUpperCase(variableName);
            if (!valuesMap.containsKey(valueCacheKey)) {
                Icon icon = basePsiElement.getIcon(true);
                List<String> childVariables = childVariableNames.isEmpty() ? null : childVariableNames.elements();
                V value = createDebugValue(variableName, null, childVariables, icon);
                values.add(value);
                valuesMap.put(valueCacheKey, value);
            }
        });
    }

    private Project getProject() {
        return getDebugProcess().getProject();
    }

    @Override
    public void customizePresentation(@NotNull ColoredTextContainer component) {
        XSourcePosition sourcePosition = getSourcePosition();
        VirtualFile virtualFile = DBDebugUtil.getSourceCodeFile(sourcePosition);

        DBSchemaObject object = DBDebugUtil.getObject(sourcePosition);
        if (object != null) {
            String frameName = object.getName();
            Icon frameIcon = object.getObjectType().getIcon();

            IdentifierPsiElement subject = getSubject();
            if (subject != null && !Strings.equalsIgnoreCase(subject.getChars(), frameName)) {
                DBObjectType objectType = subject.getObjectType();
                frameName = frameName + "." + subject.getChars();
                frameIcon = objectType.getIcon();
            }

            component.append(frameName, SimpleTextAttributes.REGULAR_ATTRIBUTES);
            component.append(" (line " + (sourcePosition.getLine() + 1) + ") ", SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
            component.setIcon(frameIcon);

        } else if (virtualFile != null){
            Icon frameIcon;
            if (virtualFile instanceof DBVirtualFile) {
                frameIcon = ((DBVirtualFile) virtualFile).getIcon();
            } else {
                frameIcon = virtualFile.getFileType().getIcon();
            }
            component.setIcon(frameIcon);
            component.append(virtualFile.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            component.append(" (line " + (sourcePosition.getLine() + 1) + ") ", SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
        } else if (getDebugProcess().getExecutionTarget() == ExecutionTarget.METHOD) {
            component.append("Anonymous block (method runner)", SimpleTextAttributes.GRAY_ATTRIBUTES);
            component.setIcon(Icons.FILE_SQL_DEBUG_CONSOLE);
        } else {
            component.append(XDebuggerBundle.message("invalid.frame"), SimpleTextAttributes.ERROR_ATTRIBUTES);
        }
    }


}

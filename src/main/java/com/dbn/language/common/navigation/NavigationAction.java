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

package com.dbn.language.common.navigation;

import com.dbn.common.action.BasicAction;
import com.dbn.editor.code.SourceCodeManager;
import com.dbn.language.common.PsiElementRef;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.util.NlsActions.ActionText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public abstract class NavigationAction extends BasicAction {
    private final PsiElementRef<BasePsiElement> navigationElement;
    private final DBObjectRef<DBObject> parentObjectRef;

    NavigationAction(@ActionText String text, @Nullable Icon icon, @Nullable DBObject parentObject, @NotNull BasePsiElement navigationElement) {
        super(text, null, icon);
        this.parentObjectRef = DBObjectRef.of(parentObject);
        this.navigationElement = PsiElementRef.of(navigationElement);
    }

    public DBObject getParentObject() {
        return DBObjectRef.get(parentObjectRef);
    }

    public BasePsiElement getNavigationElement() {
        return PsiElementRef.get(navigationElement);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        BasePsiElement navigationElement = getNavigationElement();
        if (navigationElement == null) return;

        DBObject parentObject = getParentObject();
        if (parentObject != null) {
            SourceCodeManager codeEditorManager = SourceCodeManager.getInstance(parentObject.getProject());
            codeEditorManager.navigateToObject((DBSchemaObject) parentObject, navigationElement);
        } else {
            navigationElement.navigate(true);
        }
    }
}

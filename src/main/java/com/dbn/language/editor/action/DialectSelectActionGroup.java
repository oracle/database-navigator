/*
 * Copyright 2026 Oracle and/or its affiliates
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
package com.dbn.language.editor.action;

import com.dbn.common.action.DefaultActionGroup;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.psi.PsiUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;

public class DialectSelectActionGroup extends DefaultActionGroup {
    public DialectSelectActionGroup(Project project, VirtualFile virtualFile) {
        add(new DialectSelectAction(null));
        addSeparator();

        PsiFile psiFile = PsiUtil.getPsiFile(project, virtualFile);
        if (!(psiFile instanceof DBLanguagePsiFile databasePsiFile)) return;

        DBLanguage<?> language = databasePsiFile.getDBLanguage();
        if (language == null) return;

        for (DBLanguageDialect dialect : language.getLanguageDialects()) {
            add(new DialectSelectAction(dialect));
        }
    }
}

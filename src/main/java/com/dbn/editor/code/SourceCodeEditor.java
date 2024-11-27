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

package com.dbn.editor.code;

import com.dbn.common.editor.BasicTextEditorImpl;
import com.dbn.editor.DBContentType;
import com.dbn.editor.EditorProviderId;
import com.dbn.editor.code.content.SourceCodeOffsets;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.common.psi.PsiUtil;
import com.dbn.language.psql.PSQLFile;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

public class SourceCodeEditor extends BasicTextEditorImpl<DBSourceCodeVirtualFile>{
    private final DBObjectRef<DBSchemaObject> object;

    public SourceCodeEditor(Project project, final DBSourceCodeVirtualFile sourceCodeFile, String name, EditorProviderId editorProviderId) {
        super(project, sourceCodeFile, name, editorProviderId);
        object = DBObjectRef.of(sourceCodeFile.getObject());
    }

    public DBSchemaObject getObject() {
        return DBObjectRef.get(object);
    }

    public int getHeaderEndOffset() {
        SourceCodeOffsets offsets = getVirtualFile().getOffsets();
        return offsets.getHeaderEndOffset();
    }

    public void navigateTo(DBObject object) {
        PsiFile file = PsiUtil.getPsiFile(getObject().getProject(), getVirtualFile());
        if (file instanceof PSQLFile) {
            PSQLFile psqlFile = (PSQLFile) file;
            BasePsiElement navigable = psqlFile.lookupObjectDeclaration(object.getObjectType(), object.getName());
            if (navigable == null) navigable = psqlFile.lookupObjectSpecification(object.getObjectType(), object.getName());
            if (navigable != null) navigable.navigate(true);
        }
    }

    public DBContentType getContentType() {
        return getVirtualFile().getContentType();
    }
}

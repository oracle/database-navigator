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

package com.dbn.vfs;

import com.dbn.common.project.ProjectRef;
import com.dbn.common.thread.Write;
import com.dbn.common.util.Documents;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguageParserDefinition;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectPsiCache;
import com.dbn.vfs.file.DBConsoleVirtualFile;
import com.dbn.vfs.file.DBObjectVirtualFile;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.SingleRootFileViewProvider;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.dispose.Failsafe.guarded;

public class DatabaseFileViewProvider extends SingleRootFileViewProvider {
    public static final Key<DatabaseFileViewProvider> CACHED_VIEW_PROVIDER = new Key<>("CACHED_VIEW_PROVIDER");
    private final ProjectRef project;

    public DatabaseFileViewProvider(@NotNull Project project, @NotNull VirtualFile virtualFile, boolean eventSystemEnabled) {
        super(PsiManager.getInstance(project), virtualFile, eventSystemEnabled);
        this.project = ProjectRef.of(project);
        virtualFile.putUserData(CACHED_VIEW_PROVIDER, this);
        //virtualFile.putUserData(FREE_THREADED, true);
    }

    public DatabaseFileViewProvider(@NotNull Project project, @NotNull VirtualFile virtualFile, boolean eventSystemEnabled, @NotNull Language language) {
        super(PsiManager.getInstance(project), virtualFile, eventSystemEnabled, language);
        this.project = ProjectRef.of(project);
        virtualFile.putUserData(CACHED_VIEW_PROVIDER, this);

        //virtualFile.putUserData(FREE_THREADED, true);
    }

    @Override
    public boolean isPhysical() {
        return super.isPhysical();
    }

    @Override
    @Nullable
    protected PsiFile getPsiInner(@NotNull Language language) {
        if (language instanceof DBLanguage || language instanceof DBLanguageDialect) {
            VirtualFile virtualFile = getVirtualFile();
            if (virtualFile instanceof DBConsoleVirtualFile) {
                // do not use psi facade
            } else  if (virtualFile instanceof DBObjectVirtualFile) {
                return guarded(null, virtualFile, f -> {
                    DBObjectVirtualFile objectFile = (DBObjectVirtualFile) f;
                    DBObject object = objectFile.getObject();
                    return DBObjectPsiCache.asPsiFile(object);
                });
            }

            Language baseLanguage = getBaseLanguage();
            return super.getPsiInner(baseLanguage);

/*
            // TODO cleanup
            PsiFile psiFile = super.getPsiInner(baseLanguage);
            if (psiFile == null) {
                DBParseableVirtualFile parseableFile = getParseableFile(virtualFile);
                    if (parseableFile != null) {
                        parseableFile.initializePsiFile(this, language);
                    }
            } else {
                return psiFile;
            }
*/
        }

        return super.getPsiInner(language);
    }

    @NotNull
    public DBLanguagePsiFile initializePsiFile(@NotNull DBLanguageDialect languageDialect) {
        DBLanguagePsiFile file = (DBLanguagePsiFile) getCachedPsi(languageDialect);
        if (file == null) {
            file = (DBLanguagePsiFile) getCachedPsi(languageDialect.getBaseLanguage());
        }
        if (file == null) {
            DBLanguageParserDefinition parserDefinition = languageDialect.getParserDefinition();
            file = (DBLanguagePsiFile) parserDefinition.createFile(this);
            forceCachedPsi(file);
            Document document = Documents.getDocument(file);// cache hard reference to document (??)
            if (isValid(document)) {
                // TODO non-physical fs assertion
                //FileDocumentManagerImpl.registerDocument(document, getVirtualFile());
            }
        }
        return file;
    }

    private void forceCachedPsi(DBLanguagePsiFile file) {
        Write.run(() -> super.forceCachedPsi(file));
    }

    private static DBParseableVirtualFile getParseableFile(VirtualFile virtualFile) {
        if (virtualFile instanceof DBParseableVirtualFile) {
            return (DBParseableVirtualFile) virtualFile;
        }

        if (virtualFile instanceof LightVirtualFile) {
            LightVirtualFile lightVirtualFile = (LightVirtualFile) virtualFile;
            VirtualFile originalFile = lightVirtualFile.getOriginalFile();
            if (originalFile != null && !originalFile.equals(virtualFile)) {
                return getParseableFile(originalFile);
            }
        }
        return null;
    }

    @NotNull
    @Override
    public SingleRootFileViewProvider createCopy(@NotNull VirtualFile copy) {
        return new DatabaseFileViewProvider(getProject(), copy, false, getBaseLanguage());
    }

    @NotNull
    public Project getProject() {
        return project.ensure();
    }

    @NotNull
    @Override
    public VirtualFile getVirtualFile() {
        VirtualFile virtualFile = super.getVirtualFile();
/*
        if (virtualFile instanceof SourceCodeFile)  {
            SourceCodeFile sourceCodeFile = (SourceCodeFile) virtualFile;
            return sourceCodeFile.getDatabaseFile();
        }
*/
        return virtualFile;
    }
}

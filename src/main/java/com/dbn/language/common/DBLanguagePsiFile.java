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

package com.dbn.language.common;

import com.dbn.common.dispose.StatefulDisposable;
import com.dbn.common.dispose.UnlistedDisposable;
import com.dbn.common.environment.EnvironmentType;
import com.dbn.common.thread.Read;
import com.dbn.common.ui.Presentable;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Editors;
import com.dbn.common.util.SlowOps;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.connection.context.DatabaseContextBase;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.dbn.connection.session.DatabaseSession;
import com.dbn.ddl.DDLFileAttachmentManager;
import com.dbn.language.common.element.ElementTypeBundle;
import com.dbn.language.common.element.cache.ElementLookupContext;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.common.psi.PsiElementVisitors;
import com.dbn.language.common.psi.PsiUtil;
import com.dbn.language.common.psi.lookup.LookupAdapters;
import com.dbn.language.common.psi.lookup.PsiLookupAdapter;
import com.dbn.language.sql.SQLLanguage;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectPsiCache;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.dbn.vfs.DBParseableVirtualFile;
import com.dbn.vfs.DBVirtualFile;
import com.dbn.vfs.DatabaseFileSystem;
import com.dbn.vfs.file.DBContentVirtualFile;
import com.dbn.vfs.file.DBObjectVirtualFile;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.ide.util.EditSourceUtil;
import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.ParserDefinition;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.SingleRootFileViewProvider;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.testFramework.LightVirtualFile;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.Set;
import java.util.function.Consumer;

import static com.dbn.common.dispose.Failsafe.guarded;
import static com.dbn.common.util.Documents.getDocument;
import static com.dbn.common.util.Documents.getEditors;

public abstract class DBLanguagePsiFile extends PsiFileImpl implements DatabaseContextBase, Presentable, StatefulDisposable, UnlistedDisposable {
    // TODO: check if any other visitor relevant
    public static final PsiElementVisitors visitors = PsiElementVisitors.create(
            "SpellCheckingInspection",
            "InjectedLanguageManager",
            "UpdateCopyrightAction");

    private final Language language;
    private final DBLanguageFileType fileType;
    private DBObjectRef<DBSchemaObject> underlyingObject;

    @Override
    public PsiElement getPrevSibling() {
        return null;
        //return super.getPrevSibling();
    }

    public DBLanguagePsiFile(FileViewProvider viewProvider, DBLanguageFileType fileType, DBLanguage language) {
        super(viewProvider);
        this.language = findLanguage(language);
        this.fileType = fileType;
        ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(language);
        if (parserDefinition == null) {
            throw new RuntimeException("PsiFileBase: language.getParserDefinition() returned null.");
        }
        VirtualFile virtualFile = viewProvider.getVirtualFile();
        if (virtualFile instanceof DBSourceCodeVirtualFile) {
            DBSourceCodeVirtualFile sourceCodeFile = (DBSourceCodeVirtualFile) virtualFile;
            this.underlyingObject = sourceCodeFile.getObjectRef();
        }

        IFileElementType nodeType = parserDefinition.getFileNodeType();
        //assert nodeType.getLanguage() == this.language;
        init(nodeType, nodeType);
/*        if (viewProvider instanceof SingleRootFileViewProvider) {
            SingleRootFileViewProvider singleRootFileViewProvider = (SingleRootFileViewProvider) viewProvider;
            singleRootFileViewProvider.forceCachedPsi(this);
        }*/
    }

    @Nullable
    @Override
    public Icon getIcon() {
        VirtualFile virtualFile = getVirtualFile();
        if (virtualFile instanceof DBVirtualFile) {
            DBVirtualFile databaseVirtualFile = (DBVirtualFile) virtualFile;
            return databaseVirtualFile.getIcon();
        }
        return virtualFile.getFileType().getIcon();
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    public void setUnderlyingObject(DBSchemaObject underlyingObject) {
        this.underlyingObject = DBObjectRef.of(underlyingObject);
    }

    public DBObject getUnderlyingObject() {
        VirtualFile virtualFile = getVirtualFile();
        if (virtualFile != null) {
            if (virtualFile instanceof DBObjectVirtualFile) {
                DBObjectVirtualFile<?> databaseObjectFile = (DBObjectVirtualFile<?>) virtualFile;
                return databaseObjectFile.getObject();
            }

            if (virtualFile instanceof DBSourceCodeVirtualFile) {
                DBSourceCodeVirtualFile sourceCodeFile = (DBSourceCodeVirtualFile) virtualFile;
                return sourceCodeFile.getObject();
            }

            DDLFileAttachmentManager instance = DDLFileAttachmentManager.getInstance(getProject());
            DBSchemaObject editableObject = instance.getMappedObject(virtualFile);
            if (editableObject != null) return editableObject;
        }

        return DBObjectRef.get(underlyingObject);
    }

    public DBLanguagePsiFile(Project project, DBLanguageFileType fileType, @NotNull DBLanguage<?> language) {
        this(createFileViewProvider(project), fileType, language);
    }

    private static SingleRootFileViewProvider createFileViewProvider(Project project) {
        return new SingleRootFileViewProvider(PsiManager.getInstance(project), new LightVirtualFile());
    }

    private Language findLanguage(Language baseLanguage) {
        FileViewProvider viewProvider = getViewProvider();
        Set<Language> languages = viewProvider.getLanguages();
        for (Language actualLanguage : languages) {
            if (actualLanguage.isKindOf(baseLanguage)) {
                return actualLanguage;
            }
        }
        throw new AssertionError(
                "Language " + baseLanguage + " doesn't participate in view provider " + viewProvider + ": " + new ArrayList<>(languages));
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitors.isSupported(visitor)) {
            visitor.visitFile(this);
        }
    }

    @Override
    public PsiElement getFirstChild() {
        return Read.call(this, f -> f.getSuperFirstChild());
    }

    private PsiElement getSuperFirstChild() {
        return super.getFirstChild();
    }

    @Nullable
    public DBLanguageDialect getLanguageDialect() {
        VirtualFile virtualFile = getVirtualFile();
        if (virtualFile instanceof DBContentVirtualFile) {
            DBContentVirtualFile contentFile = (DBContentVirtualFile) virtualFile;
            return contentFile.getLanguageDialect();
        }
        
        if (language instanceof DBLanguage) {
            DBLanguage<?> dbLanguage = (DBLanguage<?>) language;
            ConnectionHandler connection = getConnection();
            if (connection != null) {

                DBLanguageDialect languageDialect = connection.getLanguageDialect(dbLanguage);
                if (languageDialect != null){
                    return languageDialect;
                }
            } else {
                return dbLanguage.getLanguageDialects()[0];
            }
        } else if (language instanceof DBLanguageDialect) {
            return (DBLanguageDialect) language;
        }
        
        return null;
    }

    @Override
    public VirtualFile getVirtualFile() {
/*
        PsiFile originalFile = getOriginalFile();
        return originalFile == this ?
                super.getVirtualFile() :
                originalFile.getVirtualFile();
*/
        return Commons.nvl(super.getVirtualFile(), getViewProvider().getVirtualFile());
    }

    public boolean isInjectedContext() {
        return getVirtualFile() instanceof VirtualFileWindow;
    }

    private FileConnectionContextManager getContextManager() {
        return FileConnectionContextManager.getInstance(getProject());
    }

    @Override
    @Nullable
    public ConnectionHandler getConnection() {
        VirtualFile file = getVirtualFile();
        if (file != null && !getProject().isDisposed()) {
            FileConnectionContextManager contextManager = getContextManager();
            return contextManager.getConnection(file);
        }
        return null;
    }

    public void setConnection(ConnectionHandler connection) {
        VirtualFile file = getVirtualFile();
        if (file != null) {
            FileConnectionContextManager contextManager = getContextManager();
            contextManager.setConnection(file, connection);
        }
    }

    @Override
    @Nullable
    public SchemaId getSchemaId() {
        VirtualFile file = getVirtualFile();
        if (file != null) {
            FileConnectionContextManager contextManager = getContextManager();
            return contextManager.getDatabaseSchema(file);
        }
        return null;
    }

    public void setDatabaseSchema(SchemaId schema) {
        VirtualFile file = getVirtualFile();
        if (file != null) {
            FileConnectionContextManager contextManager = getContextManager();
            contextManager.setDatabaseSchema(file, schema);
        }
    }

    @Override
    public DatabaseSession getSession() {
        VirtualFile file = getVirtualFile();
        if (file != null && !getProject().isDisposed()) {
            FileConnectionContextManager contextManager = getContextManager();
            return contextManager.getDatabaseSession(file);
        }
        return null;
    }

    public void setDatabaseSession(DatabaseSession session) {
        VirtualFile file = getVirtualFile();
        if (file != null) {
            FileConnectionContextManager contextManager = getContextManager();
            contextManager.setDatabaseSession(file, session);
        }
    }

    @NotNull
    @Override
    public Language getLanguage() {
        return language;
    }

    public DBLanguage<?> getDBLanguage() {
        return language instanceof DBLanguage ? (DBLanguage<?>) language : null;
    }

    @Override
    public void navigate(boolean requestFocus) {
        if (navigateLocal()) return;

        VirtualFile file = getVirtualFile();
        if (!(file instanceof DBParseableVirtualFile) && canNavigate()) {
            super.navigate(requestFocus);
        }
    }

    private boolean navigateLocal() {
        Editor selectedEditor = Editors.getSelectedEditor(getProject());
        if (selectedEditor == null) return false;

        Document document = getDocument(getContainingFile());
        if (document == null) return false;

        Editor[] editors = getEditors(document);
        for (Editor editor : editors) {
            if (editor != selectedEditor) continue;

            OpenFileDescriptor descriptor = (OpenFileDescriptor) EditSourceUtil.getDescriptor(this);
            if (descriptor == null) continue;

            descriptor.navigateIn(selectedEditor);
            return true;
        }
        return false;
    }

    @Override
    @NotNull
    public DBLanguageFileType getFileType() {
        return fileType;
    }

    public ElementTypeBundle getElementTypeBundle() {
        DBLanguageDialect languageDialect = getLanguageDialect();
        languageDialect = Commons.nvl(languageDialect, SQLLanguage.INSTANCE.getMainLanguageDialect());
        return languageDialect.getParserDefinition().getParser().getElementTypes();
    }

    @Override
    public PsiDirectory getParent() {
        VirtualFile file = getVirtualFile();
        if (file.isInLocalFileSystem()) return Read.call(this, f -> f.getSuperParent());

        DBObject underlyingObject = getUnderlyingObject();
        if (underlyingObject == null) return null;

        DBObject parentObject = underlyingObject.getParentObject();
        if (parentObject == null) return guarded(null, underlyingObject, o -> o.getConnection().getPsiDirectory());

        return DBObjectPsiCache.asPsiDirectory(parentObject);
    }

    private PsiDirectory getSuperParent() {
        return super.getParent();
    }

    @Override
    public boolean isValid() {
        VirtualFile virtualFile = getViewProvider().getVirtualFile();
        if (virtualFile.getFileSystem() instanceof DatabaseFileSystem) {
            return virtualFile.isValid();
        } else {
            return Read.call(() -> SlowOps.checkValid(this, f -> f.isSuperValid()));
        }
    }

    private boolean isSuperValid() {
        return super.isValid();
    }

    public String getParseRootId() {
        VirtualFile virtualFile = getVirtualFile();
        if (virtualFile == null) return null;

        String parseRootId = virtualFile.getUserData(DBParseableVirtualFile.PARSE_ROOT_ID_KEY);
        if (parseRootId == null && virtualFile instanceof DBSourceCodeVirtualFile) {
            DBSourceCodeVirtualFile sourceCodeFile = (DBSourceCodeVirtualFile) virtualFile;
            parseRootId = sourceCodeFile.getParseRootId();
            if (parseRootId != null) {
                virtualFile.putUserData(DBParseableVirtualFile.PARSE_ROOT_ID_KEY, parseRootId);
            }
        }

        return parseRootId;
    }

    public double getDatabaseVersion() {
        ConnectionHandler connection = getConnection();
        return connection == null ? ElementLookupContext.MAX_DB_VERSION : connection.getDatabaseVersion();
    }

    @Nullable
    public static DBLanguagePsiFile createFromText(@NotNull Project project, String fileName, @NotNull DBLanguageDialect languageDialect, String text, ConnectionHandler activeConnection, SchemaId currentSchema) {
        PsiFileFactory psiFileFactory = PsiFileFactory.getInstance(project);
        PsiFile rawPsiFile = Read.call(psiFileFactory, f -> f.createFileFromText(fileName, languageDialect, text));
        if (rawPsiFile instanceof DBLanguagePsiFile) {
            DBLanguagePsiFile psiFile = (DBLanguagePsiFile) rawPsiFile;
            psiFile.setConnection(activeConnection);
            psiFile.setDatabaseSchema(currentSchema);
            return psiFile;
        }
        return null;
    }

    public void lookupVariableDefinition(int offset, Consumer<BasePsiElement> consumer) {
        BasePsiElement<?> scope = PsiUtil.lookupElementAtOffset(this, ElementTypeAttribute.SCOPE_DEMARCATION, offset);
        while (scope != null) {
            PsiLookupAdapter lookupAdapter = LookupAdapters.identifierDefinition(DBObjectType.ARGUMENT);
            scope.collectPsiElements(lookupAdapter, 0, consumer);

            lookupAdapter = LookupAdapters.variableDefinition(DBObjectType.ANY);
            scope.collectPsiElements(lookupAdapter, 1, consumer);

            PsiElement parent = scope.getParent();
            if (parent instanceof BasePsiElement) {
                BasePsiElement<?> basePsiElement = (BasePsiElement<?>) parent;
                scope = basePsiElement.findEnclosingElement(ElementTypeAttribute.SCOPE_DEMARCATION);
                if (scope == null) scope = basePsiElement.findEnclosingElement(ElementTypeAttribute.SCOPE_ISOLATION);
            } else {
                scope = null;
            }
        }
    }

    /********************************************************
     *                    Disposable                        *
     ********************************************************/
    @Getter
    @Setter
    private boolean disposed;

    @Override
    public void disposeInner() {
        markInvalidated();
        nullify();

        // TODO memory cleanup
        //markInvalidated();
/*
            FileElement treeElement = derefTreeElement();
            if (treeElement != null) {
                treeElement.detachFromFile();
            }
*/

    }

    @NotNull
    public EnvironmentType getEnvironmentType() {
        ConnectionHandler connection = getConnection();
        return connection == null ? EnvironmentType.DEFAULT :  connection.getEnvironmentType();
    }
}

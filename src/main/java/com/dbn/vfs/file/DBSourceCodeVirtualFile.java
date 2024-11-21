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

package com.dbn.vfs.file;

import com.dbn.common.thread.Background;
import com.dbn.common.thread.ThreadMonitor;
import com.dbn.common.thread.Write;
import com.dbn.common.util.ChangeTimestamp;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.session.DatabaseSession;
import com.dbn.database.DatabaseFeature;
import com.dbn.editor.DBContentType;
import com.dbn.editor.code.SourceCodeManager;
import com.dbn.editor.code.content.GuardedBlockMarkers;
import com.dbn.editor.code.content.GuardedBlockType;
import com.dbn.editor.code.content.SourceCodeContent;
import com.dbn.editor.code.content.SourceCodeOffsets;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.psi.PsiUtil;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.common.status.DBObjectStatus;
import com.dbn.vfs.DBParseableVirtualFile;
import com.dbn.vfs.DatabaseFileViewProvider;
import com.intellij.notebook.editor.BackedVirtualFile;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.sql.Timestamp;

import static com.dbn.common.util.GuardedBlocks.createGuardedBlocks;
import static com.dbn.common.util.GuardedBlocks.removeGuardedBlocks;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.vfs.file.status.DBFileStatus.LATEST;
import static com.dbn.vfs.file.status.DBFileStatus.MERGED;
import static com.dbn.vfs.file.status.DBFileStatus.MODIFIED;
import static com.dbn.vfs.file.status.DBFileStatus.OUTDATED;
import static com.dbn.vfs.file.status.DBFileStatus.REFRESHING;

@Slf4j
@Getter
@Setter
public class DBSourceCodeVirtualFile extends DBContentVirtualFile implements DBParseableVirtualFile, DocumentListener, BackedVirtualFile {

    private SourceCodeContent originalContent = new SourceCodeContent();
    private SourceCodeContent localContent = new SourceCodeContent();
    private SourceCodeContent databaseContent = null;

    private ChangeTimestamp databaseTimestamp = new ChangeTimestamp();

    private String sourceLoadError;

    public DBSourceCodeVirtualFile(final DBEditableObjectVirtualFile databaseFile, DBContentType contentType) {
        super(databaseFile, contentType);
        setCharset(databaseFile.getConnection().getSettings().getDetailSettings().getCharset());
    }

    @NotNull
    public SourceCodeOffsets getOffsets() {
        return localContent.getOffsets();
    }

    @Override
    public PsiFile initializePsiFile(DatabaseFileViewProvider fileViewProvider, DBLanguage<?> language) {
        ConnectionHandler connection = this.getConnection();
        String parseRootId = getParseRootId();
        if (parseRootId != null) {
            DBLanguageDialect languageDialect = connection.resolveLanguageDialect(language);
            if (languageDialect != null) {
                fileViewProvider.getVirtualFile().putUserData(PARSE_ROOT_ID_KEY, getParseRootId());
                DBLanguagePsiFile file = fileViewProvider.initializePsiFile(languageDialect);
                file.setUnderlyingObject(getObject());
                return file;
            }
        }
        return null;
    }

    @Override
    public DatabaseSession getSession() {
        return this.getConnection().getSessionBundle().getPoolSession();
    }

    public boolean isLoaded() {
        return localContent.isLoaded();
    }

    public String getParseRootId() {
        return getObject().getCodeParseRootId(contentType);
    }

    @Nullable
    public PsiFile getPsiFile() {
        Project project = getProject();
        return PsiUtil.getPsiFile(project, this);
    }

    public void refreshContentState() {
        if (!isLoaded()) return;
        if (is(REFRESHING)) return;

        try {
            set(REFRESHING, true);
            DBSchemaObject object = getObject();

            if (!is(LATEST) && !is(MERGED)) return;


            boolean checkSources = true;
            Project project = object.getProject();
            SourceCodeManager sourceCodeManager = SourceCodeManager.getInstance(project);

            ChangeTimestamp latestTimestamp = new ChangeTimestamp();
            if (isChangeMonitoringSupported()) {
                latestTimestamp = sourceCodeManager.loadChangeTimestamp(object, contentType);
                checkSources = databaseTimestamp.isOlderThan(latestTimestamp);
                databaseTimestamp = latestTimestamp;
            }

            databaseTimestamp = latestTimestamp;

            if (!checkSources) return;
            SourceCodeContent latestContent = sourceCodeManager.loadSourceFromDatabase(object, contentType);

            if (is(LATEST) && !latestContent.matches(originalContent, true)) {
                set(OUTDATED, true);
                databaseContent = latestContent;
            }

            if (is(MERGED) && !latestContent.matches(databaseContent, true)) {
                set(OUTDATED, true);
                databaseContent = latestContent;
            }

        } catch (SQLException e) {
            conditionallyLog(e);
            log.warn("Error refreshing source content state", e);
        } finally {
            set(REFRESHING, false);
        }
    }

    public boolean isChangedInDatabase(boolean reload) {
        if (!isLoaded()) return false;
        if (is(REFRESHING)) return false;


        if (reload || databaseTimestamp.isDirty()) {
            if (ThreadMonitor.isTimeSensitiveThread()) {
                Background.run(getProject(), () -> refreshContentState());
            } else {
                refreshContentState();
            }

        }
        return isNot(REFRESHING) && (is(OUTDATED) || is(MERGED));
    }

    public boolean isMergeRequired() {
        return is(MODIFIED) && is(OUTDATED);
    }

    public void markAsMerged() {
        set(MERGED, true);
    }

    @NotNull
    public Timestamp getDatabaseChangeTimestamp() {
        return databaseTimestamp.value();
    }

    @NotNull
    public CharSequence getOriginalContent() {
        return originalContent.getText();
    }

    @NotNull
    public CharSequence getContent() {
        return localContent.getText();
    }

    @Override
    public boolean isWritable() {
        return originalContent.isWritable();
    }

    public void loadSourceFromDatabase() throws SQLException {
        DBSchemaObject object = getObject();
        Project project = object.getProject();
        SourceCodeManager sourceCodeManager = SourceCodeManager.getInstance(project);
        SourceCodeContent newContent = sourceCodeManager.loadSourceFromDatabase(object, contentType);
        databaseTimestamp = sourceCodeManager.loadChangeTimestamp(object, contentType);

        updateFileContent(newContent, null);
        originalContent.setText(newContent.getText());
        originalContent.setWritable(newContent.isWritable());
        object.getStatus().set(contentType, DBObjectStatus.PRESENT, newContent.length() > 0);

        databaseContent = null;
        sourceLoadError = null;
        set(LATEST, true);
        setModified(false);
	}


    public void saveSourceToDatabase() throws SQLException {
        DBSchemaObject object = getObject();
        Project project = object.getProject();

        String oldContent = getOriginalContent().toString();
        String newContent = getContent().toString();
        object.executeUpdateDDL(contentType, oldContent, newContent);

        SourceCodeManager sourceCodeManager = SourceCodeManager.getInstance(project);
        databaseTimestamp = sourceCodeManager.loadChangeTimestamp(object, contentType);
        originalContent.setText(newContent);

        databaseContent = null;
        sourceLoadError = null;
        set(LATEST, true);
        setModified(false);
    }

    public void revertLocalChanges() {
        updateFileContent(null, originalContent.getText());
        databaseContent = null;
        sourceLoadError = null;
        set(LATEST, true);
        setModified(false);
    }

    private void updateFileContent(@Nullable SourceCodeContent newContent, @Nullable CharSequence newText) {
        if (newContent != null) {
            localContent = newContent;
        } else {
            localContent.setText(newText);
        }
        Document document = Documents.getDocument(DBSourceCodeVirtualFile.this);
        if (document == null) return;

        Documents.setText(document, localContent.getText());

        SourceCodeOffsets offsets = localContent.getOffsets();
        GuardedBlockMarkers guardedBlocks = offsets.getGuardedBlocks();
        if (guardedBlocks.isEmpty()) return;

        Write.run(getProject(), () -> {
            removeGuardedBlocks(document, GuardedBlockType.READONLY_DOCUMENT_SECTION);
            createGuardedBlocks(document, GuardedBlockType.READONLY_DOCUMENT_SECTION, guardedBlocks, null);
        });
    }

    private boolean isChangeMonitoringSupported() {
        return DatabaseFeature.OBJECT_CHANGE_MONITORING.isSupported(getObject());
    }

    @Override
    @NotNull
    public byte[] contentsToByteArray() {
        return localContent.getText().toString().getBytes(getCharset());
    }

    @Override
    public long getLength() {
        return localContent.length();
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, T value) {
        if (key == FileDocumentManagerImpl.HARD_REF_TO_DOCUMENT_KEY && contentType.isOneOf(DBContentType.CODE, DBContentType.CODE_BODY) ) {
            getMainDatabaseFile().putUserData(FileDocumentManagerImpl.HARD_REF_TO_DOCUMENT_KEY, (Document) value);
        }
        super.putUserData(key, value);
    }

    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        CharSequence oldContent = originalContent.getText();
        CharSequence newContent = event.getDocument().getCharsSequence();

        setModified(!Strings.equals(oldContent, newContent));
        localContent.setText(newContent);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        originalContent = new SourceCodeContent();
        localContent = new SourceCodeContent();
    }

    @NotNull
    @Override
    public VirtualFile getOriginFile() {
        // DBN-536 TODO check why below was needed
        return this;//getMainDatabaseFile();
    }
}

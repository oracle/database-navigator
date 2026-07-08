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

package com.dbn.diagnostics;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.file.util.FileSearchRequest;
import com.dbn.common.notification.NotificationSupport;
import com.dbn.common.state.StateContainer;
import com.dbn.common.thread.Read;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Files;
import com.dbn.common.util.Lists;
import com.dbn.common.util.Strings;
import com.dbn.diagnostics.data.DiagnosticCategory;
import com.dbn.diagnostics.data.ParserDiagnosticsFilter;
import com.dbn.diagnostics.data.ParserDiagnosticsResult;
import com.dbn.diagnostics.data.ParserDiagnosticsUtil;
import com.dbn.diagnostics.ui.ParserDiagnosticsForm;
import com.dbn.diagnostics.ui.ParserIssueReportDialog;
import com.dbn.error.jira.JiraParserIssueReportSubmitter;
import com.dbn.language.common.DBLanguageFileType;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.psi.PsiUtil;
import com.dbn.language.common.psi.scrambler.DBLLanguageFileScrambler;
import com.dbn.language.psql.PSQLFileType;
import com.dbn.language.sql.SQLFileType;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.fileTypes.ExtensionFileNameMatcher;
import com.intellij.openapi.fileTypes.FileNameMatcher;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.file.util.VirtualFiles.findFiles;
import static com.dbn.common.notification.NotificationCategory.DEVELOPER;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.thread.Progress.progressOf;
import static com.dbn.common.util.Dialogs.whenOk;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

@Slf4j
@Getter
@State(
    name = ParserDiagnosticsManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class ParserDiagnosticsManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.ParserDiagnosticsManager";

    private final List<ParserDiagnosticsResult> resultHistory = new ArrayList<>();
    private final StateContainer states = new StateContainer();
    private ParserDiagnosticsFilter resultFilter = ParserDiagnosticsFilter.EMPTY;
    private boolean running;

    private ParserDiagnosticsManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
    }

    public static ParserDiagnosticsManager get(@NotNull Project project) {
        return projectService(project, ParserDiagnosticsManager.class);
    }

    @SneakyThrows
    public void submitParserIssueReport(DBLanguagePsiFile psiFile) {
        File attachmentFile = File.createTempFile("dbn-parser-issue-", "." + psiFile.getVirtualFile().getExtension());
        attachmentFile.deleteOnExit();

        Charset charset = psiFile.getVirtualFile().getCharset();
        byte[] scrambled = ParserDiagnosticsManager.scrambleFile(psiFile, charset);
        FileUtil.writeToFile(attachmentFile, scrambled);

        Attachment attachment = new Attachment(attachmentFile.getPath(), attachmentFile, attachmentFile.getName());
        FileType fileType = psiFile.getFileType();
        String scrambledCode = new String(scrambled, charset);

        Dialogs.show(() -> new ParserIssueReportDialog(getProject(), scrambledCode, fileType,
                psiFile.getLanguageDialect()), whenOk(d -> submitParserIssueReport(attachment)));
    }

    private void submitParserIssueReport(Attachment attachment) {
        IdeaLoggingEvent event = new IdeaLoggingEvent(
                "Parser issue",
                new IllegalArgumentException("Parser error"),
                List.of(attachment),
                (IdeaPluginDescriptor) null,
                null);

        new JiraParserIssueReportSubmitter().submit(
                getProject(),
                new IdeaLoggingEvent[]{event},
                "Parser issue reported from the SQL/PLSQL editor");
    }

    @NotNull
    public ParserDiagnosticsResult runParserDiagnostics(ProgressIndicator progress) {
        try {
            running = true;
            Project project = getProject();
            String[] extensions = getFileExtensions();
            FileSearchRequest searchRequest = FileSearchRequest.forExtensions(extensions);
            VirtualFile[] files = findFiles(project, searchRequest);
            ParserDiagnosticsResult result = new ParserDiagnosticsResult(project);

            progress.setIndeterminate(false);
            progress.setText(txt("prc.diagnostics.text.RunningParserDiagnosticsProgress", 0, files.length));

            for (int i = 0, filesLength = files.length; i < filesLength; i++) {
                VirtualFile file = files[i];
                String filePath = file.getPath();

                progress.checkCanceled();
                progress.setText(txt("prc.diagnostics.text.RunningParserDiagnosticsProgress", i, files.length));
                String relativeFilePath = Files.convertToRelativePath(project, filePath);
                progress.setText2(Strings.truncateWithMiddleEllipsis(relativeFilePath, 80));
                progress.setFraction(progressOf(i, files.length));

                DBLanguagePsiFile psiFile = ensureFileParsed(file);
                progress.checkCanceled();
                if (psiFile == null) {
                    result.addEntry(filePath, 1, 0);
                } else {
                    int errors = Read.call(psiFile, f -> ParserDiagnosticsUtil.countErrors(f));
                    int warnings = Read.call(psiFile, f -> ParserDiagnosticsUtil.countWarnings(f));
                    if (errors > 0 || warnings > 0) {
                        result.addEntry(filePath, errors, warnings);
                    }
                }
            }
            resultHistory.add(0, result);
            indexResults();
            return result;
        } finally {
            running = false;
        }
    }

    public void scrambleProjectFiles(ProgressIndicator progress, File rootDir) {
        String[] extensions = getFileExtensions();
        FileSearchRequest searchRequest = FileSearchRequest.forExtensions(extensions);
        VirtualFile[] files = findFiles(getProject(), searchRequest);

        DBLLanguageFileScrambler scrambler = new DBLLanguageFileScrambler();
        progress.setIndeterminate(true);

        for (int i = 0, filesLength = files.length; i < filesLength; i++) {
            VirtualFile file = files[i];
            progress.checkCanceled();
            String filePath = file.getPath();
            progress.setText(filePath);
            progress.setFraction(progressOf(i, files.length));

            DBLanguagePsiFile psiFile = ensureFileParsed(file);
            progress.checkCanceled();
            if (psiFile != null) {

                String scrambled = scrambleFile(psiFile, scrambler);
                String newFileName = scrambler.scrambleName(file);
                File scrambledFile = new File(rootDir, newFileName);
                try {
                    Charset charset = file.getCharset();
                    byte[] bytes = scrambled.getBytes(charset);
                    FileUtil.writeToFile(scrambledFile, bytes);
                } catch (IOException e) {
                    conditionallyLog(e);
                    NotificationSupport.sendWarningNotification(getProject(), DEVELOPER,
                            txt("ntf.diagnostics.warning.FailedToWriteFile", scrambledFile.getPath(), e));
                }
            }
        }
    }

    public static byte[] scrambleFile(@NotNull DBLanguagePsiFile psiFile, @NotNull Charset charset) {
        String scrambled = scrambleFile(psiFile, new DBLLanguageFileScrambler());
        return scrambled.getBytes(charset);
    }

    private static String scrambleFile(DBLanguagePsiFile psiFile, DBLLanguageFileScrambler scrambler) {
        return scrambler.scramble(psiFile);
    }

    public String[] getFileExtensions() {
        List<String> extensions = new ArrayList<>();
        collectFileExtensions(extensions, SQLFileType.INSTANCE);
        collectFileExtensions(extensions, PSQLFileType.INSTANCE);
        return extensions.toArray(new String[0]);
    }

    private void collectFileExtensions(List<String> bucket, DBLanguageFileType fileType) {
        FileTypeManager fileTypeManager = FileTypeManager.getInstance();
        List<FileNameMatcher> associations = fileTypeManager.getAssociations(fileType);
        for (FileNameMatcher association : associations) {
            if (association instanceof ExtensionFileNameMatcher matcher) {
                bucket.add(matcher.getExtension());
            }
        }
    }

    public void openParserDiagnostics(@Nullable ParserDiagnosticsResult result) {
        Project project = getProject();
        DiagnosticsManager diagnosticsManager = DiagnosticsManager.getInstance(project);
        ParserDiagnosticsForm form = diagnosticsManager.showDiagnosticsConsole(
                DiagnosticCategory.PARSER,
                () -> new ParserDiagnosticsForm(project));

        ParserDiagnosticsResult selectedResult = form.getSelectedResult();
        form.selectResult(Commons.nvln(result, selectedResult));
    }


    @Nullable
    public ParserDiagnosticsResult getLatestResult() {
        if (resultHistory.isEmpty()) {
            return null;
        }
        return resultHistory.get(0);
    }

    @Nullable
    public ParserDiagnosticsResult getPreviousResult(ParserDiagnosticsResult result) {
        int index = resultHistory.indexOf(result);
        if (index == -1) {
            return getLatestResult();
        }
        if (index + 1 >= resultHistory.size()) {
            return null;
        }

        return resultHistory.get(index + 1);
    }

    public boolean hasDraftResults() {
        return Lists.anyMatch(resultHistory, result -> result.isDraft());
    }

    public void saveResult(@NotNull ParserDiagnosticsResult result) {
        result.markSaved();
    }

    public void deleteResult(@NotNull ParserDiagnosticsResult selectedResult) {
        resultHistory.remove(selectedResult);
        indexResults();
    }

    private void indexResults() {
        int size = resultHistory.size();
        for (int i = 0; i < size; i++) {
            ParserDiagnosticsResult result = resultHistory.get(i);
            result.setIndex(size - i);
        }
    }

    private DBLanguagePsiFile ensureFileParsed(VirtualFile file) {
        PsiFile psiFile = PsiUtil.getPsiFile(getProject(), file);
        return psiFile instanceof DBLanguagePsiFile ? (DBLanguagePsiFile) psiFile : null;
    }

    /*********************************************
     *            PersistentStateComponent       *
     *********************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        Element element = newElement("state");
        states.writeState(element);
        Element historyElement = newElement(element, "diagnostics-history");
        for (ParserDiagnosticsResult capturedResult : resultHistory) {
            if (!capturedResult.isDraft()) {
                Element resultElement = newElement(historyElement, "result");
                capturedResult.writeState(resultElement);
            }
        }
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        states.readState(element);
        Element historyElement = element.getChild("diagnostics-history");
        resultHistory.clear();
        if (historyElement != null) {
            List<Element> resultElements = historyElement.getChildren("result");
            for (Element resultElement : resultElements) {
                ParserDiagnosticsResult result = new ParserDiagnosticsResult(getProject(), resultElement);
                resultHistory.add(result);
            }
        }
        indexResults();
    }
}

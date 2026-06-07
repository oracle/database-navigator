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

package com.dbn.execution.script;

import com.dbn.DatabaseNavigator;
import com.dbn.common.approval.UserApprovalManager;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.routine.Consumer;
import com.dbn.common.thread.CancellableDatabaseCall;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.FileChoosers;
import com.dbn.common.util.Messages;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.SchemaId;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.dbn.database.CmdLineExecutionInput;
import com.dbn.database.interfaces.DatabaseExecutionInterface;
import com.dbn.execution.ExecutionManager;
import com.dbn.execution.ExecutionStatus;
import com.dbn.execution.common.options.ExecutionEngineSettings;
import com.dbn.execution.logging.LogOutput;
import com.dbn.execution.logging.LogOutputContext;
import com.dbn.execution.script.options.ScriptExecutionSettings;
import com.dbn.execution.script.ui.CmdLineInterfaceInputDialog;
import com.dbn.execution.script.ui.ScriptExecutionInputDialog;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;
import org.jdesktop.swingx.util.OS;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.common.options.setting.Settings.setBooleanAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Conditional.when;
import static com.dbn.common.util.FilePermissions.ownDirectoryPermissions;
import static com.dbn.common.util.FilePermissions.ownFilePermissions;
import static com.dbn.common.util.FilePermissions.restrictToOwner;
import static com.dbn.common.util.Messages.showErrorDialog;
import static com.dbn.common.util.Messages.showInfoDialog;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.execution.logging.LogOutput.createSysOutput;
import static com.dbn.execution.script.ScriptExecutionProcessHandler.startProcess;
import static com.dbn.nls.NlsResources.txt;
import static com.intellij.openapi.ui.DialogWrapper.OK_EXIT_CODE;
import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.createTempFile;
import static java.util.concurrent.TimeUnit.SECONDS;

@Getter
@Setter
@State(
    name = ScriptExecutionManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class ScriptExecutionManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.ScriptExecutionManager";

    private final ExecutionManager executionManager;
    private final Map<VirtualFile, Process> activeProcesses = new ConcurrentHashMap<>();
    private final Map<DatabaseType, String> recentlyUsedInterfaces = new EnumMap<>(DatabaseType.class);
    private boolean clearOutputOption = true;

    private ScriptExecutionManager(Project project) {
        super(project, COMPONENT_NAME);
        executionManager = ExecutionManager.getInstance(project);
    }

    public static ScriptExecutionManager getInstance(@NotNull Project project) {
        return projectService(project, ScriptExecutionManager.class);
    }

    public List<CmdLineInterface> getAvailableInterfaces(DatabaseType databaseType) {
        ExecutionEngineSettings executionEngineSettings = ExecutionEngineSettings.getInstance(getProject());
        CmdLineInterfaceBundle commandLineInterfaces = executionEngineSettings.getScriptExecutionSettings().getCommandLineInterfaces();
        List<CmdLineInterface> interfaces = commandLineInterfaces.getInterfaces(databaseType);
        CmdLineInterface defaultInterface = CmdLineInterface.getDefault(databaseType);
        if (defaultInterface != null) {
            interfaces.add(0, defaultInterface);
        }
        return interfaces;
    }


    public void executeScript(VirtualFile virtualFile) {
        Project project = getProject();
        if (activeProcesses.containsKey(virtualFile)) {
            showInfoDialog(project, txt("msg.shared.title.Info"), txt("msg.execution.info.ScriptAlreadyRunning", virtualFile.getPath()));
        } else {
            FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);

            ConnectionHandler activeConnection = contextManager.getConnection(virtualFile);
            SchemaId currentSchema = contextManager.getDatabaseSchema(virtualFile);

            ScriptExecutionInput executionInput = new ScriptExecutionInput(getProject(), virtualFile, activeConnection, currentSchema, clearOutputOption);
            ScriptExecutionSettings scriptExecutionSettings = ExecutionEngineSettings.getInstance(project).getScriptExecutionSettings();
            int timeout = scriptExecutionSettings.getExecutionTimeout();
            executionInput.setExecutionTimeout(timeout);

            ScriptExecutionInputDialog inputDialog = new ScriptExecutionInputDialog(project,executionInput);

            inputDialog.show();
            if (inputDialog.getExitCode() != OK_EXIT_CODE) return;

            ConnectionHandler connection = executionInput.getConnection();
            SchemaId schemaId = executionInput.getSchemaId();
            CmdLineInterface cmdLineExecutable = executionInput.getCmdLineInterface();
            contextManager.setConnection(virtualFile, connection);
            contextManager.setDatabaseSchema(virtualFile, schemaId);
            if (connection != null) {
                recentlyUsedInterfaces.put(connection.getDatabaseType(), cmdLineExecutable.getId());
            }
            clearOutputOption = executionInput.isClearOutput();

            Progress.background(project, connection, true,
                    txt("prc.execution.title.ExecutingScript"),
                    txt("prc.execution.text.ExecutingScript",virtualFile.getName()),
                    progress -> {
                        try {
                            doExecuteScript(executionInput);
                        } catch (ProcessCanceledException e) {
                            conditionallyLog(e);
                        } catch (Exception e) {
                            conditionallyLog(e);
                            showErrorDialog(getProject(),
                                    txt("msg.execution.error.ErrorExecutingScript", virtualFile.getPath(), e.getMessage()));
                        }
                    });
        }
    }

    private void doExecuteScript(ScriptExecutionInput input) throws Exception {
        CmdLineInterface cmdLineInterface = input.getCmdLineInterface();
        UserApprovalManager approvalManager = UserApprovalManager.getInstance();
        approvalManager.ensureApproved(cmdLineInterface);

        ScriptExecutionContext context = input.getExecutionContext();
        context.set(ExecutionStatus.EXECUTING, true);
        ConnectionHandler connection = nd(input.getConnection());
        VirtualFile sourceFile = input.getSourceFile();
        activeProcesses.remove(sourceFile, null);

        Project project = getProject();
        AtomicReference<File> tempScriptFile = new AtomicReference<>();
        LogOutputContext outputContext = new LogOutputContext(connection, sourceFile, null);
        int timeout = input.getExecutionTimeout();
        executionManager.writeLogOutput(outputContext, createSysOutput(outputContext, txt("log.execution.info.InitializingScriptExecution"), input.isClearOutput()));

        try {
            new CancellableDatabaseCall<>(connection, null, timeout, SECONDS) {
                @Override
                public Object execute() throws Exception {
                    SchemaId schemaId = input.getSchemaId();

                    String content = new String(sourceFile.contentsToByteArray());
                    File temporaryScriptFile = createTempScriptFile();

                    executionManager.writeLogOutput(outputContext, createSysOutput(txt("log.execution.info.CreatingTemporaryScriptFile", temporaryScriptFile)));
                    tempScriptFile.set(temporaryScriptFile);

                    DatabaseExecutionInterface executionInterface = connection.getInterfaces().getExecutionInterface();
                    CmdLineExecutionInput executionInput = executionInterface.createScriptExecutionInput(cmdLineInterface,
                            temporaryScriptFile.getPath(),
                            content,
                            schemaId,
                            connection.getDatabaseInfo(),
                            connection.getAuthenticationInfo());

                    FileUtil.writeToFile(temporaryScriptFile, executionInput.getTextContent());
                    if (!temporaryScriptFile.isFile() || !temporaryScriptFile.exists()) {
                        executionManager.writeLogOutput(outputContext, LogOutput.createErrOutput(txt("log.execution.error.TemporaryScriptFileCreationFailed", temporaryScriptFile)));
                        throw new IllegalStateException(txt("msg.execution.error.TemporaryScriptFileCreationFailed", temporaryScriptFile));
                    }

                    String commandLine = executionInput.getCommandLine();
                    executionManager.writeLogOutput(outputContext, createSysOutput(txt("log.execution.info.ExecutingCommand", commandLine)));
                    executionManager.writeLogOutput(outputContext, createSysOutput(""));

                    ScriptExecutionProcessHandler processHandler = startProcess(executionInput);
                    processHandler.whenOutputted(e -> consumeProcessOutput(e.getText(), outputContext));

                    // start the process
                    Process process = processHandler.getProcess();

                    outputContext.setProcess(process);
                    activeProcesses.put(sourceFile, process);

                    outputContext.setHideEmptyLines(false);
                    outputContext.start();
                    executionManager.writeLogOutput(outputContext, createSysOutput(outputContext, txt("log.execution.info.ScriptExecutionStarted"), false));

                    // start monitoring the process and wait for completion
                    processHandler.startNotify();
                    processHandler.waitFor();

                    LogOutput logOutput = createSysOutput(outputContext,
                            outputContext.isStopped() ?
                                    txt("log.execution.info.ScriptExecutionInterrupted") :
                                    txt("log.execution.info.ScriptExecutionFinished"), false);
                    executionManager.writeLogOutput(outputContext, logOutput);
                    ProjectEvents.notify(project,
                            ScriptExecutionListener.TOPIC,
                            (listener) -> listener.scriptExecuted(project, sourceFile));
                    return null;
                }

                @Override
                public void cancel() {
                    outputContext.stop();
                }

                @Override
                public void handleTimeout() {
                    showErrorDialog(project,
                            txt("msg.execution.title.ScriptExecutionTimeout"),
                            txt("msg.execution.error.ScriptExecutionTimeout"),
                            Messages.OPTIONS_RETRY_CANCEL, 0,
                            option -> when(option == 0, () -> executeScript(sourceFile)));

                }

                @Override
                public void handleException(Throwable e) {
                    showErrorDialog(project,
                            txt("msg.execution.title.ScriptExecutionError"),
                            txt("msg.execution.error.ScriptExecutionError", sourceFile.getPath(), e.getMessage()),
                            Messages.OPTIONS_RETRY_CANCEL, 0,
                            option -> when(option == 0, () -> executeScript(sourceFile)));
                }
            }.start();
        } catch (ProcessCanceledException e) {
            conditionallyLog(e);
            //executionManager.writeLogOutput(outputContext, LogOutput.createSysOutput(outputContext, " - Script execution cancelled by user", false));
        } catch (Exception e) {
            conditionallyLog(e);
            executionManager.writeLogOutput(outputContext, LogOutput.createErrOutput(e.getMessage()));
            executionManager.writeLogOutput(outputContext, createSysOutput(outputContext, txt("log.execution.info.ScriptExecutionFinishedWithErrors"), false));
            throw e;
        } finally {
            context.set(ExecutionStatus.EXECUTING, false);
            outputContext.finish();
            activeProcesses.remove(sourceFile);
            File temporaryScriptFile = tempScriptFile.get();
            if (temporaryScriptFile != null && temporaryScriptFile.exists()) {
                executionManager.writeLogOutput(outputContext, createSysOutput(txt("log.execution.info.DeletingTemporaryScriptFile", temporaryScriptFile)));
                FileUtil.delete(temporaryScriptFile);
            }
            File temporaryScriptDirectory = temporaryScriptFile == null ? null : temporaryScriptFile.getParentFile();
            if (temporaryScriptDirectory != null && temporaryScriptDirectory.exists()) {
                executionManager.writeLogOutput(outputContext, createSysOutput(txt("log.execution.info.DeletingTemporaryScriptDirectory", temporaryScriptDirectory)));
                FileUtil.delete(temporaryScriptDirectory);
            }
        }
    }

    private void consumeProcessOutput(String line, LogOutputContext outputContext) {
        line = line.replace("\n", "").replace("\r", "");
        LogOutput stdOutput = LogOutput.createStdOutput(line);
        executionManager.writeLogOutput(outputContext, stdOutput);
    }

    public void createCmdLineInterface(
            @NotNull DatabaseType databaseType,
            @Nullable Set<String> bannedNames,
            @NotNull Consumer<CmdLineInterface> consumer) {

        boolean updateSettings = false;
        VirtualFile virtualFile = selectCmdLineExecutable(databaseType, null);
        if (virtualFile == null) return;

        Project project = getProject();
        ExecutionEngineSettings executionEngineSettings = ExecutionEngineSettings.getInstance(project);
        if (bannedNames == null) {
            bannedNames = executionEngineSettings.getScriptExecutionSettings().getCommandLineInterfaces().getInterfaceNames();
            updateSettings = true;
        }

        CmdLineInterface cmdLineInterface = new CmdLineInterface(databaseType, virtualFile.getPath(), CmdLineInterface.getDefault(databaseType).getName(), null);
        CmdLineInterfaceInputDialog dialog = new CmdLineInterfaceInputDialog(project, cmdLineInterface, bannedNames);
        dialog.show();
        if (dialog.getExitCode() != OK_EXIT_CODE) return;

        cmdLineInterface.setAcknowledged(true);
        consumer.accept(cmdLineInterface);
        if (updateSettings) {
            CmdLineInterfaceBundle commandLineInterfaces = executionEngineSettings.getScriptExecutionSettings().getCommandLineInterfaces();
            commandLineInterfaces.add(cmdLineInterface);

            UserApprovalManager approvalManager = UserApprovalManager.getInstance();
            approvalManager.approve(cmdLineInterface);
        }
    }

    @Nullable
    public VirtualFile selectCmdLineExecutable(@NotNull DatabaseType databaseType, @Nullable String selectedExecutable) {
        CmdLineInterface defaultCli = CmdLineInterface.getDefault(databaseType);
        String extension = OS.isWindows() ? ".exe" : "";
        FileChooserDescriptor fileChooserDescriptor = FileChoosers.singleFile().
                withTitle(txt("cfg.execution.title.SelectCommandLineClient")).
                withDescription(txt("cfg.execution.text.SelectCommandLineClient", defaultCli.getExecutablePath() + extension)).
                withShowHiddenFiles(true);
        VirtualFile selectedFile = Strings.isEmpty(selectedExecutable) ? null : LocalFileSystem.getInstance().findFileByPath(selectedExecutable);
        VirtualFile[] virtualFiles = FileChooser.chooseFiles(fileChooserDescriptor, getProject(), selectedFile);
        return virtualFiles.length == 1 ? virtualFiles[0] : null;
    }

    @Nullable
    public CmdLineInterface getRecentInterface(DatabaseType databaseType) {
        String id = recentlyUsedInterfaces.get(databaseType);
        if (id != null) {
            if (Objects.equals(id, CmdLineInterface.DEFAULT_ID)) {
                return CmdLineInterface.getDefault(databaseType);
            }

            ExecutionEngineSettings executionEngineSettings = ExecutionEngineSettings.getInstance(getProject());
            CmdLineInterfaceBundle commandLineInterfaces = executionEngineSettings.getScriptExecutionSettings().getCommandLineInterfaces();
            return commandLineInterfaces.getInterface(id);

        }
        return null;
    }

    private File createTempScriptFile() throws IOException {
        Path tempDirectory = createTempScriptDirectory();
        try {
            return createTempScriptFile(tempDirectory).toFile();
        } catch (IOException e) {
            FileUtil.delete(tempDirectory.toFile());
            throw e;
        }
    }

    private static Path createTempScriptDirectory() throws IOException {
        try {
            return createTempScriptDirectory(null);
        } catch (IOException e) {
            Path systemTempDirectory = Path.of(PathManager.getSystemPath(), "tmp");
            Files.createDirectories(systemTempDirectory);
            return createTempScriptDirectory(systemTempDirectory);
        }
    }

    private static Path createTempScriptDirectory(@Nullable Path parentDirectory) throws IOException {
        try {
            var permissions = ownDirectoryPermissions();
            return parentDirectory == null ?
                    createTempDirectory("DBN-", permissions) :
                    createTempDirectory(parentDirectory, "DBN-", permissions);

        } catch (UnsupportedOperationException e) {
            Path tempDirectory = parentDirectory == null ?
                    createTempDirectory("DBN-") :
                    createTempDirectory(parentDirectory, "DBN-");

            restrictToOwner(tempDirectory.toFile());
            return tempDirectory;
        }
    }

    private static Path createTempScriptFile(Path tempDirectory) throws IOException {
        try {
            var permissions = ownFilePermissions();
            return createTempFile(tempDirectory, "DBN-", ".sql", permissions);
        } catch (UnsupportedOperationException e) {
            Path tempFile = createTempFile(tempDirectory, "DBN-", ".sql");
            restrictToOwner(tempFile.toFile());
            return tempFile;
        }
    }

    /****************************************
     *       PersistentStateComponent       *
     *****************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        Element element = newStateElement();
        setBooleanAttribute(element, "clear-outputs", clearOutputOption);
        Element interfacesElement = newElement(element, "recently-used-interfaces");
        for (var entry : recentlyUsedInterfaces.entrySet()) {
            DatabaseType databaseType = entry.getKey();
            String interfaceId = entry.getValue();
            Element interfaceElement = newElement(interfacesElement, "mapping");
            interfaceElement.setAttribute("database-type", databaseType.name());
            interfaceElement.setAttribute("interface-id", interfaceId);
        }
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        recentlyUsedInterfaces.clear();
        clearOutputOption = booleanAttribute(element, "clear-outputs", clearOutputOption);
        Element interfacesElement = element.getChild("recently-used-interfaces");
        if (interfacesElement != null) {
            for (Element child : interfacesElement.getChildren()) {
                DatabaseType databaseType = enumAttribute(child, "database-type", DatabaseType.class);
                String interfaceId = stringAttribute(child, "interface-id");
                recentlyUsedInterfaces.put(databaseType, interfaceId);
            }

        }
    }
}

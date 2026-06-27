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

package com.dbn.execution.script;

import com.dbn.common.approval.UserApprovalAction;
import com.dbn.common.approval.UserApprovalAdapter;
import com.dbn.common.approval.UserApprovalOption;
import com.dbn.common.checksum.Checksum;

import java.io.File;

import static com.dbn.common.approval.UserApprovalAction.COMMAND_LINE_EXECUTION;
import static com.dbn.common.checksum.Checksum.fromFileAttributes;
import static com.dbn.common.checksum.ChecksumType.SHA_256;
import static com.dbn.common.util.Executables.resolveExecutableFile;
import static com.dbn.nls.NlsResources.txt;

/**
 * Prepares user approval information for launching external command-line clients selected
 * for database script execution.
 */
public class CmdLineExecutionApprovalAdapter implements UserApprovalAdapter<CmdLineInterface> {
    private static final UserApprovalOption[] APPROVAL_OPTIONS = {
            UserApprovalOption.one(txt("msg.execution.button.TrustAndExecute")),
            UserApprovalOption.noneWithoutCooldown(txt("msg.shared.button.Cancel"))};

    @Override
    public Class<CmdLineInterface> getApprovalClass() {
        return CmdLineInterface.class;
    }

    @Override
    public UserApprovalAction getApprovalAction() {
        return COMMAND_LINE_EXECUTION;
    }

    @Override
    public String getApprovalTitle(CmdLineInterface cmdLineInterface) {
        return txt("msg.execution.title.TrustCommandLineInterface");
    }

    @Override
    public String getApprovalMessage(CmdLineInterface cmdLineInterface) {
        String executablePath = getExecutablePath(cmdLineInterface);
        File executableFile = resolveExecutableFile(executablePath);

        return txt("msg.execution.question.TrustCommandLineInterface",
                cmdLineInterface.getName(),
                executableFile == null ? executablePath : executableFile.getAbsolutePath());
    }

    @Override
    public String getApprovalKey(CmdLineInterface cmdLineInterface) {
        return "cmd-line-interface:" + cmdLineInterface.getId() + ":" + getExecutableFingerprint(cmdLineInterface);
    }

    @Override
    public UserApprovalOption[] getApprovalOptions(CmdLineInterface cmdLineInterface) {
        return APPROVAL_OPTIONS;
    }

    private String getExecutableFingerprint(CmdLineInterface cmdLineInterface) {
        String executablePath = getExecutablePath(cmdLineInterface);
        File executableFile = resolveExecutableFile(executablePath);

        String executableSignature = executableFile == null ?
                "path:" + executablePath :
                "file:" + executableFile.getPath() + ":" + fromFileAttributes(executableFile, SHA_256);

        return Checksum.fromStringContent(getDatabaseType(cmdLineInterface) + ":" + executableSignature, SHA_256);
    }

    private static String getDatabaseType(CmdLineInterface cmdLineInterface) {
        return cmdLineInterface.getDatabaseType() == null ? "UNKNOWN" : cmdLineInterface.getDatabaseType().name();
    }

    private static String getExecutablePath(CmdLineInterface cmdLineInterface) {
        String executablePath = cmdLineInterface.getExecutablePath();
        return executablePath == null ? "" : executablePath.trim();
    }
}

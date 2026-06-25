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
import com.dbn.common.checksum.Checksum;
import com.dbn.common.util.Messages;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.time.Duration;

import static com.dbn.common.approval.UserApprovalAction.PASSWORD_ENVIRONMENT_VARIABLE;
import static com.dbn.common.checksum.Checksum.fromFileAttributes;
import static com.dbn.common.checksum.ChecksumType.SHA_256;
import static com.dbn.common.util.Executables.resolveExecutableFile;
import static com.dbn.nls.NlsResources.txt;

/**
 * Prepares user approval information for the legacy script authentication path that sends
 * database passwords to external clients through child-process environment variables.
 */
public class CmdLineEnvPasswordApprovalAdapter implements UserApprovalAdapter<CmdLineInterface> {
    private static final String[] APPROVAL_OPTIONS = Messages.options(
            txt("msg.execution.button.AllowAndExecute"),
            txt("msg.shared.button.Cancel"));

    @Override
    public Class<CmdLineInterface> getApprovalClass() {
        return CmdLineInterface.class;
    }

    @Override
    public UserApprovalAction getApprovalAction() {
        return PASSWORD_ENVIRONMENT_VARIABLE;
    }

    @Override
    public String getApprovalTitle(CmdLineInterface cmdLineInterface) {
        return txt("msg.execution.title.AllowPasswordEnvironmentVariable");
    }

    @Override
    public String getApprovalMessage(CmdLineInterface cmdLineInterface) {
        String executablePath = getExecutablePath(cmdLineInterface);
        File executableFile = resolveExecutableFile(executablePath);

        return txt("msg.execution.question.AllowPasswordEnvironmentVariable",
                cmdLineInterface.getName(),
                executableFile == null ? executablePath : executableFile.getAbsolutePath());
    }

    @Override
    public String getApprovalKey(CmdLineInterface cmdLineInterface) {
        return "cmd-line-interface:" + cmdLineInterface.getId() + ":" + getExecutableFingerprint(cmdLineInterface);
    }

    @Override
    public String[] getApprovalOptions(CmdLineInterface cmdLineInterface) {
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

    @Override
    @Nullable
    public Duration getRejectionCooldown(CmdLineInterface approvable, int option) {
        return null; // do not remember rejections
    }
}

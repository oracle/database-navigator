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

import com.dbn.common.checksum.file.FileFingerprint;
import com.intellij.openapi.progress.ProcessCanceledException;
import lombok.experimental.UtilityClass;

import java.io.File;

import static com.dbn.common.checksum.Checksum.fromStringContent;
import static com.dbn.common.checksum.ChecksumType.SHA_256;
import static com.dbn.common.checksum.file.FileContentSignature.capture;
import static com.dbn.common.checksum.file.FileContentSignature.getIdentity;
import static com.dbn.common.util.Executables.resolveExecutableFile;
import static com.dbn.nls.NlsResources.txt;

/**
 * Produces stable approval identities and content-based signatures for command-line executables.
 */
@UtilityClass
public final class CmdLineExecutableSignature {
    public static String getApprovalKey(CmdLineInterface cmdLineInterface) {
        String databaseType = cmdLineInterface.getDatabaseType() == null ? "UNKNOWN" : cmdLineInterface.getDatabaseType().name();
        return "cmd-line-interface:" + databaseType + ":" + cmdLineInterface.getId();
    }

    public static String getApprovalSignature(CmdLineInterface cmdLineInterface) {
        return createApprovalSignature(cmdLineInterface, getFingerprint(cmdLineInterface));
    }

    public static FileFingerprint getFingerprint(CmdLineInterface cmdLineInterface) {
        String executablePath = cmdLineInterface.getExecutablePath();
        File executableFile = resolveExecutableFile(executablePath == null ? "" : executablePath.trim());
        if (executableFile == null) {
            throw new IllegalStateException(txt("msg.execution.error.CommandLineExecutableFingerprintFailed", executablePath));
        }

        try {
            return capture(executableFile);
        } catch (ProcessCanceledException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(txt("msg.execution.error.CommandLineExecutableFingerprintFailed", executablePath), e);
        }
    }

    public static String getFingerprintCacheIdentity(CmdLineInterface cmdLineInterface) {
        String executablePath = cmdLineInterface.getExecutablePath();
        if (executablePath == null || executablePath.isBlank()) return "";

        File executable = new File(executablePath.trim());
        return executable.isAbsolute() ? getIdentity(executable) : "command-line-executable:" + executable.getPath();
    }

    public static String createApprovalSignature(CmdLineInterface cmdLineInterface, FileFingerprint fingerprint) {
        String databaseType = cmdLineInterface.getDatabaseType() == null ? "UNKNOWN" : cmdLineInterface.getDatabaseType().name();
        return fromStringContent(databaseType + ":" + fingerprint.canonicalPath() + ":" + fingerprint.checksum(), SHA_256);
    }
}

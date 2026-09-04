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

import com.dbn.connection.DatabaseType;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;

public class CmdLineEnvPasswordApprovalAdapterTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void approvalKeyChangesWhenExecutableContentChangesWithStableAttributes() throws Exception {
        File executable = temporaryFolder.newFile("client");
        long lastModified = 1_700_000_000_000L;
        Files.writeString(executable.toPath(), "trusted-client");
        Assert.assertTrue(executable.setLastModified(lastModified));

        CmdLineInterface cmdLineInterface = new CmdLineInterface(
                "client-id", DatabaseType.POSTGRES, executable.getPath(), "Client", "client");
        CmdLineEnvPasswordApprovalAdapter adapter = new CmdLineEnvPasswordApprovalAdapter();
        String approvedKey = adapter.getApprovalKey(cmdLineInterface);

        Files.writeString(executable.toPath(), "changed-client");
        Assert.assertEquals("test executable size must remain unchanged", 14, executable.length());
        Assert.assertTrue(executable.setLastModified(lastModified));

        Assert.assertNotEquals(approvedKey, adapter.getApprovalKey(cmdLineInterface));
    }
}

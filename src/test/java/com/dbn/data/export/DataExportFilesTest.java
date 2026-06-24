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

package com.dbn.data.export;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class DataExportFilesTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void getFileKeepsSimpleNameInsideExportDirectory() throws Exception {
        File exportDirectory = temporaryFolder.newFolder("export");

        File file = DataExportFiles.getFile(exportDirectory.getPath(), "safe.csv");

        assertEquals(new File(exportDirectory, "safe.csv").getCanonicalFile(), file);
        assertTrue(file.toPath().startsWith(exportDirectory.getCanonicalFile().toPath()));
    }

    @Test
    public void getFileRejectsParentTraversal() throws Exception {
        File exportDirectory = temporaryFolder.newFolder("export");
        assertThrows(DataExportException.class, () -> DataExportFiles.getFile(exportDirectory.getPath(), "../pwn.csv"));
    }

    @Test
    public void getFileRejectsAbsolutePath() throws Exception {
        File exportDirectory = temporaryFolder.newFolder("export");
        assertThrows(DataExportException.class, () -> DataExportFiles.getFile(exportDirectory.getPath(), temporaryFolder.newFile("pwn.csv").getAbsolutePath()));
    }

    @Test
    public void getFileRejectsBackslashSeparatedPath() throws Exception {
        File exportDirectory = temporaryFolder.newFolder("export");
        assertThrows(DataExportException.class, () -> DataExportFiles.getFile(exportDirectory.getPath(), "..\\pwn.csv"));
    }

    @Test
    public void getFileRejectsMixedTraversalPath() throws Exception {
        File exportDirectory = temporaryFolder.newFolder("export");
        assertThrows(DataExportException.class, () -> DataExportFiles.getFile(exportDirectory.getPath(), "safe/..\\pwn.csv"));
    }

    @Test
    public void getFileRejectsWindowsAbsolutePath() throws Exception {
        File exportDirectory = temporaryFolder.newFolder("export");
        assertThrows(DataExportException.class, () -> DataExportFiles.getFile(exportDirectory.getPath(), "C:\\tmp\\pwn.csv"));
    }

    @Test
    public void sanitizeFileNameStripsPathSegmentsFromDefaultName() {
        assertEquals("pwn.csv", DataExportFiles.sanitizeFileName("../pwn.csv"));
        assertEquals("pwn.csv", DataExportFiles.sanitizeFileName("/tmp/pwn.csv"));
        assertEquals("pwn.csv", DataExportFiles.sanitizeFileName("C:\\tmp\\pwn.csv"));
        assertEquals("safe.csv", DataExportFiles.sanitizeFileName("safe.csv"));
    }
}

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

package com.dbn.common.util;

import com.dbn.common.checksum.Checksum;
import com.dbn.common.checksum.ChecksumType;
import com.dbn.test.util.FileUtil;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;

import static com.dbn.common.util.Commons.nvl;
import static org.junit.Assume.assumeFalse;

public class ChecksumTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void fromFileContentTest() throws Exception {
        File file = FileUtil.getFileFromClasspath(getClass(), "checksum/T0001.txt");
        String checksum = Checksum.fromFileContent(file, ChecksumType.SHA_256);

        Assert.assertEquals("5eaaa3637c055ff9b4a33bb25ad868d0486cf206f8077f5e30bf29a5f81bf103", checksum);
    }


    @Test
    public void fromFileAttributesTest() throws Exception {
        // JDBC-4166 -- for some reason these currently fail on Linux
        assumeFalse(isLinux());
        File file = createChecksumDirectory();
        String checksum = Checksum.fromFileAttributes(file, ChecksumType.SHA_256);

        Assert.assertEquals("f3ea5f50af189887c33f280b57c935eda532cb0869ea333557e2a8bb2a1547af", checksum);
    }

    @Test
    public void fromFileContentsTest() throws Exception {
        // JDBC-4166 -- for some reason these currently fail on Linux
        assumeFalse(isLinux());
        File file = createChecksumDirectory();
        String checksum = Checksum.fromFileContents(file, ChecksumType.SHA_256);

        Assert.assertEquals("8187ee8ab8085d3eb9f94e2a739bc50332f17c534516fd44a340402d0ce9ccd2", checksum);
    }

    @Test
    public void verifyChecksumTest() {
        String checksum = "5eaaa3637c055ff9b4a33bb25ad868d0486cf206f8077f5e30bf29a5f81bf103";

        Assert.assertTrue(Checksum.verifyChecksum(checksum, checksum, ChecksumType.SHA_256));
        Assert.assertTrue(Checksum.verifyChecksum(checksum.toUpperCase(Locale.ROOT), checksum, ChecksumType.SHA_256));
        Assert.assertTrue(Checksum.verifyChecksum(" " + checksum.toUpperCase(Locale.ROOT) + " ", checksum, ChecksumType.SHA_256));
        Assert.assertFalse(Checksum.verifyChecksum(checksum.replace('e', 'g'), checksum, ChecksumType.SHA_256));
        Assert.assertFalse(Checksum.verifyChecksum(checksum.substring(0, 40), checksum.substring(0, 40), ChecksumType.SHA_256));
        Assert.assertFalse(Checksum.verifyChecksum(null, checksum, ChecksumType.SHA_256));
        Assert.assertFalse(Checksum.verifyChecksum(checksum, null, ChecksumType.SHA_256));
    }


    private static boolean isLinux() {
        String osName = nvl(System.getProperty("os.name"), "unknown");
        return Strings.containsIgnoreCase(osName, "linux");
    }

    private File createChecksumDirectory() throws IOException {
        File directory = temporaryFolder.newFolder("checksum");
        File subdirectory = new File(directory, "sub");
        Assert.assertTrue(subdirectory.mkdir());

        File file = new File(subdirectory, "T0001.txt");
        Files.writeString(file.toPath(), "Test resource for ChecksumTest\n");

        Assert.assertTrue(file.setLastModified(1_700_000_000_000L));
        Assert.assertTrue(subdirectory.setLastModified(1_700_000_001_000L));
        Assert.assertTrue(directory.setLastModified(1_700_000_002_000L));
        return directory;
    }
}

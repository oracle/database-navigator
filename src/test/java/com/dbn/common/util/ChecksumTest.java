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
import org.junit.Test;

import java.io.File;
import java.util.Locale;

import static com.dbn.common.util.Commons.nvl;
import static org.junit.Assume.assumeFalse;

public class ChecksumTest {
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
        File file = FileUtil.getFileFromClasspath(getClass(), "checksum");
        String checksum = Checksum.fromFileAttributes(file, ChecksumType.SHA_256);

        Assert.assertEquals("f6b5bc7bde6d05995135bbcb1b2a20294f5f5f2e1688e2dff9c4db2eb08245bd", checksum);
    }

    @Test
    public void fromFileContentsTest() throws Exception {
        // JDBC-4166 -- for some reason these currently fail on Linux
        assumeFalse(isLinux());
        File file = FileUtil.getFileFromClasspath(getClass(), "checksum");
        String checksum = Checksum.fromFileContents(file, ChecksumType.SHA_256);

        Assert.assertEquals("abcd15289b0c71eb444940e8b38c93b31fe2b80dc4c6839bf3ce460e3b4edbc6", checksum);
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
}
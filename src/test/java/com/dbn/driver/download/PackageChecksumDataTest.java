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

package com.dbn.driver.download;

import com.dbn.common.checksum.Checksum;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static com.dbn.common.checksum.ChecksumType.SHA_1;
import static com.dbn.common.checksum.ChecksumType.SHA_256;

public class PackageChecksumDataTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void verifyChecksumsAcceptsStrongChecksum() throws Exception {
        File packageDir = temporaryFolder.newFolder("package");
        File artifact = new File(packageDir, "driver-1.0.jar");
        java.nio.file.Files.writeString(artifact.toPath(), "artifact-content");

        PackageChecksumData checksumData = new PackageChecksumData("test-package", temporaryFolder.newFile("test-package.txt"));
        checksumData.addChecksum("driver-1.0", SHA_256, Checksum.fromFileContent(artifact, SHA_256));

        Assert.assertTrue(checksumData.verifyChecksums(packageDir));
    }

    @Test
    public void verifyChecksumsAcceptsSha1RepositoryChecksum() throws Exception {
        File packageDir = temporaryFolder.newFolder("package");
        File artifact = new File(packageDir, "driver-1.0.jar");
        java.nio.file.Files.writeString(artifact.toPath(), "artifact-content");

        PackageChecksumData checksumData = new PackageChecksumData("test-package", temporaryFolder.newFile("test-package.txt"));
        checksumData.addChecksum("driver-1.0", SHA_1, Checksum.fromFileContent(artifact, SHA_1));

        Assert.assertTrue(checksumData.verifyChecksums(packageDir));
    }

    @Test
    public void verifyChecksumsRejectsMismatchedSha1Checksum() throws Exception {
        File packageDir = temporaryFolder.newFolder("package");
        File artifact = new File(packageDir, "driver-1.0.jar");
        java.nio.file.Files.writeString(artifact.toPath(), "artifact-content");

        PackageChecksumData checksumData = new PackageChecksumData("test-package", temporaryFolder.newFile("test-package.txt"));
        checksumData.addChecksum("driver-1.0", SHA_1, "0000000000000000000000000000000000000000");

        Assert.assertFalse(checksumData.verifyChecksums(packageDir));
        Assert.assertTrue(checksumData.getInvalidChecksums().contains(artifact));
    }

    @Test
    public void readChecksumsAcceptsLegacyTwoColumnSha1Data() throws Exception {
        File packageDir = temporaryFolder.newFolder("package");
        File artifact = new File(packageDir, "driver-1.0.jar");
        java.nio.file.Files.writeString(artifact.toPath(), "artifact-content");

        File checksumFile = temporaryFolder.newFile("test-package.txt");
        java.nio.file.Files.writeString(checksumFile.toPath(), "driver-1.0 " + Checksum.fromFileContent(artifact, SHA_1));
        PackageChecksumData checksumData = new PackageChecksumData("test-package", checksumFile);
        checksumData.readChecksums();

        Assert.assertTrue(checksumData.verifyChecksums(packageDir));
    }

    @Test
    public void readChecksumsAcceptsEnumAlgorithmData() throws Exception {
        File packageDir = temporaryFolder.newFolder("package");
        File artifact = new File(packageDir, "driver-1.0.jar");
        java.nio.file.Files.writeString(artifact.toPath(), "artifact-content");

        File checksumFile = temporaryFolder.newFile("test-package.txt");
        PackageChecksumData checksumData = new PackageChecksumData("test-package", checksumFile);
        checksumData.addChecksum("driver-1.0", SHA_256, Checksum.fromFileContent(artifact, SHA_256));
        checksumData.writeChecksums();

        PackageChecksumData reloadedChecksumData = new PackageChecksumData("test-package", checksumFile);
        reloadedChecksumData.readChecksums();

        Assert.assertTrue(reloadedChecksumData.verifyChecksums(packageDir));
    }

    @Test
    public void readChecksumsRejectsNonEnumAlgorithmData() throws Exception {
        File packageDir = temporaryFolder.newFolder("package");
        File artifact = new File(packageDir, "driver-1.0.jar");
        java.nio.file.Files.writeString(artifact.toPath(), "artifact-content");

        File checksumFile = temporaryFolder.newFile("test-package.txt");
        java.nio.file.Files.writeString(checksumFile.toPath(), "driver-1.0 SHA-256 " + Checksum.fromFileContent(artifact, SHA_256));
        PackageChecksumData checksumData = new PackageChecksumData("test-package", checksumFile);
        checksumData.readChecksums();

        Assert.assertFalse(checksumData.verifyChecksums(packageDir));
        Assert.assertTrue(checksumData.getInvalidChecksums().contains(artifact));
    }

    @Test
    public void verifyChecksumsRejectsMissingChecksumData() throws Exception {
        File packageDir = temporaryFolder.newFolder("package");
        PackageChecksumData checksumData = new PackageChecksumData("test-package", temporaryFolder.newFile("test-package.txt"));

        Assert.assertFalse(checksumData.verifyChecksums(packageDir));
    }

    @Test
    public void verifyStrongChecksumsAcceptsEveryJarWithStrongChecksum() throws Exception {
        File packageDir = temporaryFolder.newFolder("package");
        File artifact = new File(packageDir, "driver-1.0.jar");
        java.nio.file.Files.writeString(artifact.toPath(), "artifact-content");

        PackageChecksumData checksumData = new PackageChecksumData("test-package", temporaryFolder.newFile("test-package.txt"));
        checksumData.addChecksum("driver-1.0", SHA_256, Checksum.fromFileContent(artifact, SHA_256));

        Assert.assertTrue(checksumData.verifyStrongChecksums(packageDir));
    }

    @Test
    public void verifyStrongChecksumsRejectsWeakChecksum() throws Exception {
        File packageDir = temporaryFolder.newFolder("package");
        File artifact = new File(packageDir, "driver-1.0.jar");
        java.nio.file.Files.writeString(artifact.toPath(), "artifact-content");

        PackageChecksumData checksumData = new PackageChecksumData("test-package", temporaryFolder.newFile("test-package.txt"));
        checksumData.addChecksum("driver-1.0", SHA_1, Checksum.fromFileContent(artifact, SHA_1));

        Assert.assertFalse(checksumData.verifyStrongChecksums(packageDir));
        Assert.assertTrue(checksumData.getInvalidChecksums().contains(artifact));
    }

    @Test
    public void verifyStrongChecksumsRejectsJarWithoutChecksum() throws Exception {
        File packageDir = temporaryFolder.newFolder("package");
        File artifact = new File(packageDir, "driver-1.0.jar");
        File extraArtifact = new File(packageDir, "extra-1.0.jar");
        java.nio.file.Files.writeString(artifact.toPath(), "artifact-content");
        java.nio.file.Files.writeString(extraArtifact.toPath(), "extra-artifact-content");

        PackageChecksumData checksumData = new PackageChecksumData("test-package", temporaryFolder.newFile("test-package.txt"));
        checksumData.addChecksum("driver-1.0", SHA_256, Checksum.fromFileContent(artifact, SHA_256));

        Assert.assertFalse(checksumData.verifyStrongChecksums(packageDir));
        Assert.assertTrue(checksumData.getInvalidChecksums().contains(extraArtifact));
    }
}

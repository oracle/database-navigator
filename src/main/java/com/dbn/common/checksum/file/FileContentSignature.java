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

package com.dbn.common.checksum.file;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.util.Collection;

import static com.dbn.common.checksum.Checksum.fromFileContent;
import static com.dbn.common.checksum.Checksum.fromStringContent;
import static com.dbn.common.checksum.ChecksumType.SHA_256;

/**
 * Produces content-based signatures for files used in external approval decisions.
 * Callers bind the result to their own approval key or include target identity and
 * context when constructing the adapter's approval signature.
 */
@UtilityClass
public class FileContentSignature {

    /**
     * Produces a SHA-256 content digest for one file.
     */
    public static String fromFile(File file) {
        return fromFileContent(file, SHA_256);
    }

    /**
     * Captures the canonical path and content digest for one file.
     */
    @SneakyThrows
    public static FileFingerprint capture(File file) {
        File canonicalFile = file.getCanonicalFile();
        return new FileFingerprint(canonicalFile.getPath(), fromFile(canonicalFile));
    }

    /**
     * Returns a stable, normalized key for sharing fingerprints of the same file path.
     */
    public static String getIdentity(File file) {
        return file.getAbsoluteFile().toPath().normalize().toString();
    }

    /**
     * Produces a SHA-256 digest for a set of files, including each file name and
     * content digest in iteration order.
     */
    public static String fromFiles(Collection<File> files) {
        StringBuilder signature = new StringBuilder();
        for (File file : files) {
            signature.append(file.getName())
                    .append(':')
                    .append(fromFile(file))
                    .append('\n');
        }
        return fromStringContent(signature.toString(), SHA_256);
    }
}

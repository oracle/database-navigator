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
import com.dbn.common.checksum.ChecksumType;
import com.dbn.common.download.Downloads;
import com.dbn.driver.download.metadata.Library;
import com.dbn.driver.download.metadata.LibraryChecksum;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dbn.common.checksum.ChecksumType.MD_5;
import static com.dbn.common.checksum.ChecksumType.SHA_1;
import static com.dbn.common.checksum.ChecksumType.SHA_256;
import static com.dbn.common.checksum.ChecksumType.SHA_512;
import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;

@Slf4j
final class MavenArtifactIntegrityVerifier {
    private static final List<ChecksumType> CHECKSUM_TYPES = List.of(SHA_512, SHA_256, SHA_1, MD_5);

    private MavenArtifactIntegrityVerifier() {}

    static LibraryChecksum verify(
            ProgressIndicator indicator,
            String artifactUrl,
            File artifactFile,
            Library library) throws IOException {
        LibraryChecksum pinnedChecksum = selectPinnedChecksum(library);
        if (pinnedChecksum != null) {
            return verifyChecksum(artifactFile, pinnedChecksum.getType(), pinnedChecksum.getValue(), "pinned metadata");
        }

        for (LibraryChecksum checksum : getRemoteChecksumSources(artifactUrl, library)) {
            try {
                String checksumUrl = resolveChecksumUrl(artifactUrl, library, checksum);
                String expectedChecksum = downloadExpectedChecksum(indicator, checksumUrl, checksum.getType());
                return verifyChecksum(artifactFile, checksum.getType(), expectedChecksum, checksumUrl);
            } catch (ChecksumUnavailableException e) {
                log.info("Strong checksum source unavailable for '{}'. Cause: {}", library.getLibraryId(), e.getMessage());
            }
        }

        throw new IOException("Checksum verification is required for " + library.getLibraryId() +
                " but no Maven checksum metadata was available");
    }

    static String extractChecksum(String content, ChecksumType type) throws IOException {
        int hexLength = type.getHexLength();
        Matcher matcher = Pattern.compile("(?i)(?<![0-9a-f])([0-9a-f]{" + hexLength + "})(?![0-9a-f])").matcher(content);
        if (matcher.find()) {
            return matcher.group(1).toLowerCase(Locale.ROOT);
        }
        throw new IOException("Could not extract " + type.getName() + " checksum from checksum metadata");
    }

    static String resolveChecksumUrl(String artifactUrl, Library library, LibraryChecksum checksum) {
        String checksumUrl = checksum.getUrl();
        if (!isNotEmptyOrSpaces(checksumUrl)) return artifactUrl + "." + checksum.getType().getExtension();

        String artifactPath = library.getArtefactPath();
        String repositoryUrl = resolveRepositoryUrl(artifactUrl, artifactPath);
        String fileName = library.getFileName();

        @NonNls
        String resolvedUrl = checksumUrl
                .replace("{artifactUrl}", artifactUrl)
                .replace("{repositoryUrl}", repositoryUrl)
                .replace("{artifactPath}", artifactPath)
                .replace("{groupPath}", library.getGroupPath())
                .replace("{groupId}", library.getGroupId())
                .replace("{artifactId}", library.getArtifactId())
                .replace("{version}", library.getVersion())
                .replace("{fileName}", fileName)
                .replace("{checksumExtension}", checksum.getType().getExtension())
                .replace("{checksumAlgorithm}", checksum.getType().getName());

        if (resolvedUrl.startsWith("https://") || resolvedUrl.startsWith("http://")) return resolvedUrl;
        if (resolvedUrl.startsWith("/")) return repositoryUrl + resolvedUrl;
        return repositoryUrl + "/" + resolvedUrl;
    }

    private static LibraryChecksum selectPinnedChecksum(Library library) {
        return library.getChecksums().stream()
                .filter(c -> c.getType() != null && c.hasValue())
                .min(Comparator.comparingInt(c -> CHECKSUM_TYPES.indexOf(c.getType())))
                .orElse(null);
    }

    private static List<LibraryChecksum> getRemoteChecksumSources(String artifactUrl, Library library) {
        Map<String, LibraryChecksum> checksums = new LinkedHashMap<>();
        library.getChecksums().stream()
                .filter(c -> c.getType() != null && c.hasUrl())
                .forEach(c -> checksums.put(resolveChecksumUrl(artifactUrl, library, c), c));

        for (ChecksumType type : CHECKSUM_TYPES) {
            LibraryChecksum checksum = new LibraryChecksum(type, null);
            checksums.put(resolveChecksumUrl(artifactUrl, library, checksum), checksum);
        }
        return new ArrayList<>(checksums.values());
    }

    private static LibraryChecksum verifyChecksum(
            File artifactFile,
            ChecksumType type,
            String expectedChecksum,
            String source) throws IOException {
        String actualChecksum = Checksum.fromFileContent(artifactFile, type);
        if (Checksum.verifyChecksum(expectedChecksum, actualChecksum, type)) {
            return new LibraryChecksum(type, actualChecksum);
        }

        throw new IOException("Checksum verification failed for " + artifactFile.getName() +
                " using " + type.getName() + " from " + source);
    }

    private static String downloadExpectedChecksum(
            ProgressIndicator indicator,
            String checksumUrl,
            ChecksumType type) throws IOException, ChecksumUnavailableException {
        String content;
        try {
            content = downloadChecksumContent(indicator, checksumUrl);
        } catch (IOException e) {
            throw new ChecksumUnavailableException("Could not download " + checksumUrl, e);
        }
        return extractChecksum(content, type);
    }

    private static String downloadChecksumContent(ProgressIndicator indicator, String checksumUrl) throws IOException {
        File tempFile = FileUtil.createTempFile(UUID.randomUUID().toString(), ".tmp", true);
        try {
            Downloads.downloadAtomically(indicator, checksumUrl, tempFile);
            return String.join("\n", FileUtil.loadLines(tempFile));
        } finally {
            FileUtil.delete(tempFile);
        }
    }

    private static String resolveRepositoryUrl(String artifactUrl, String artifactPath) {
        String suffix = "/" + artifactPath;
        if (artifactUrl.endsWith(suffix)) {
            return artifactUrl.substring(0, artifactUrl.length() - suffix.length());
        }
        return artifactUrl;
    }

    private static class ChecksumUnavailableException extends Exception {
        ChecksumUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

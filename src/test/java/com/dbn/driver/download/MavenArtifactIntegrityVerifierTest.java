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
import com.dbn.driver.download.metadata.Library;
import com.dbn.driver.download.metadata.LibraryChecksum;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static com.dbn.common.checksum.ChecksumType.SHA_1;
import static com.dbn.common.checksum.ChecksumType.SHA_256;

public class MavenArtifactIntegrityVerifierTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void extractChecksumAcceptsCommonFormats() throws Exception {
        String checksum = "5eaaa3637c055ff9b4a33bb25ad868d0486cf206f8077f5e30bf29a5f81bf103";

        Assert.assertEquals(checksum, MavenArtifactIntegrityVerifier.extractChecksum(checksum + " driver.jar", SHA_256));
        Assert.assertEquals(checksum, MavenArtifactIntegrityVerifier.extractChecksum("SHA-256 (driver.jar) = " + checksum.toUpperCase(), SHA_256));
    }

    @Test
    public void verifyUsesPinnedSha256WithoutRemoteChecksum() throws Exception {
        File artifact = newArtifactFile();
        Library library = new Library("com.example", "driver", "1.0");
        String checksum = Checksum.fromFileContent(artifact, SHA_256);
        library.getChecksums().add(new LibraryChecksum(SHA_256, checksum));

        LibraryChecksum verifiedChecksum = MavenArtifactIntegrityVerifier.verify(null, artifactUrl(), artifact, library);

        Assert.assertEquals(SHA_256, verifiedChecksum.getType());
        Assert.assertEquals(checksum, verifiedChecksum.getValue());
    }

    @Test
    public void verifyAcceptsPinnedSha1Metadata() throws Exception {
        File artifact = newArtifactFile();
        Library library = new Library("com.example", "driver", "1.0");
        String checksum = Checksum.fromFileContent(artifact, SHA_1);
        library.getChecksums().add(new LibraryChecksum(SHA_1, checksum));

        LibraryChecksum verifiedChecksum = MavenArtifactIntegrityVerifier.verify(null, artifactUrl(), artifact, library);

        Assert.assertEquals(SHA_1, verifiedChecksum.getType());
        Assert.assertEquals(checksum, verifiedChecksum.getValue());
    }

    @Test
    public void verifyFallsBackToSha1SidecarWhenStrongSidecarsAreUnavailable() throws Exception {
        File artifact = newArtifactFile();
        Library library = new Library("com.example", "driver", "1.0");
        String checksum = Checksum.fromFileContent(artifact, SHA_1);

        try (ChecksumServer server = new ChecksumServer()) {
            server.put(artifactPath() + ".sha1", checksum + " driver-1.0.jar");
            LibraryChecksum verifiedChecksum = MavenArtifactIntegrityVerifier.verify(null, server.url(artifactPath()), artifact, library);

            Assert.assertEquals(SHA_1, verifiedChecksum.getType());
            Assert.assertEquals(checksum, verifiedChecksum.getValue());
        }
    }

    @Test
    public void verifySupportsConfiguredChecksumUrlTemplate() throws Exception {
        File artifact = newArtifactFile();
        Library library = new Library("com.example", "driver", "1.0");
        String checksum = Checksum.fromFileContent(artifact, SHA_256);
        library.getChecksums().add(new LibraryChecksum(SHA_256, null, "checksums/{fileName}.{checksumExtension}"));

        try (ChecksumServer server = new ChecksumServer()) {
            server.put("/maven2/checksums/driver-1.0.jar.sha256", checksum + " driver-1.0.jar");

            MavenArtifactIntegrityVerifier.verify(null, server.url(artifactPath()), artifact, library);

            Assert.assertEquals("/maven2/checksums/driver-1.0.jar.sha256", server.getRequestedPath());
        }
    }

    @Test
    public void verifyRejectsMismatchedStrongChecksum() throws Exception {
        File artifact = newArtifactFile();
        Library library = new Library("com.example", "driver", "1.0");
        library.getChecksums().add(new LibraryChecksum(SHA_256, "0000000000000000000000000000000000000000000000000000000000000000"));

        Assert.assertThrows(IOException.class, () -> MavenArtifactIntegrityVerifier.verify(null, artifactUrl(), artifact, library));
    }

    @Test
    public void verifyRejectsMismatchedSha1Checksum() throws Exception {
        File artifact = newArtifactFile();
        Library library = new Library("com.example", "driver", "1.0");
        library.getChecksums().add(new LibraryChecksum(SHA_1, "0000000000000000000000000000000000000000"));

        Assert.assertThrows(IOException.class, () -> MavenArtifactIntegrityVerifier.verify(null, artifactUrl(), artifact, library));
    }

    private File newArtifactFile() throws IOException {
        File artifact = temporaryFolder.newFile("driver-1.0.jar");
        java.nio.file.Files.writeString(artifact.toPath(), "artifact-content");
        return artifact;
    }

    private static String artifactUrl() {
        return "https://repo.example/maven2/com/example/driver/1.0/driver-1.0.jar";
    }

    private static String artifactPath() {
        return "/maven2/com/example/driver/1.0/driver-1.0.jar";
    }

    private static class ChecksumServer implements AutoCloseable {
        private final HttpServer server;
        private final Map<String, String> responses = new ConcurrentHashMap<>();
        private final AtomicReference<String> requestedPath = new AtomicReference<>();

        private ChecksumServer() throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", this::handle);
            server.start();
        }

        private void put(String path, String content) {
            responses.put(path, content);
        }

        private String url(String path) {
            return "http://127.0.0.1:" + server.getAddress().getPort() + path;
        }

        private String getRequestedPath() {
            return requestedPath.get();
        }

        private void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            requestedPath.set(path);
            String content = responses.get(path);
            if (content == null) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }

            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream responseBody = exchange.getResponseBody()) {
                responseBody.write(bytes);
            }
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}

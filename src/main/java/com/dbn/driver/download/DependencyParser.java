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
package com.dbn.driver.download;

import com.dbn.common.Pair;
import com.dbn.common.checksum.Checksum;
import com.dbn.common.checksum.ChecksumType;
import com.dbn.common.download.Downloads;
import com.dbn.common.util.FilePermissions;
import com.dbn.common.util.Measured;
import com.dbn.driver.download.metadata.Library;
import com.dbn.driver.download.metadata.LibraryDeveloper;
import com.dbn.driver.download.metadata.LibraryLicense;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.io.FileUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.internal.impl.DefaultArtifactResolver;
import org.eclipse.aether.internal.impl.EnhancedLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalArtifactRequest;
import org.eclipse.aether.repository.LocalArtifactResult;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.GetTask;
import org.eclipse.aether.spi.connector.transport.PeekTask;
import org.eclipse.aether.spi.connector.transport.PutTask;
import org.eclipse.aether.spi.connector.transport.TransportListener;
import org.eclipse.aether.spi.connector.transport.Transporter;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory;
import org.eclipse.aether.transfer.NoTransporterException;
import org.xml.sax.InputSource;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static com.dbn.common.util.Lists.convert;
import static java.util.Collections.emptyList;

@Slf4j
public class DependencyParser {
    private static final int TRANSFER_BUFFER_SIZE = 8192;
    private static final DateTimeFormatter STDOUT_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final Map<String, Pair<List<LibraryDeveloper>, List<LibraryLicense>>> metadataMap = new ConcurrentHashMap<>();
    private static final AtomicInteger ACTIVE_DEPENDENCY_RESOLUTIONS = new AtomicInteger();
    private static final AtomicInteger ACTIVE_MAVEN_TRANSFERS = new AtomicInteger();
    private static final Map<String, ReentrantLock> POM_CACHE_LOCKS = new ConcurrentHashMap<>();

    @SneakyThrows
    public static List<Library> resolveDependencies(Library library, String type, DownloadSession downloadSession) {
        List<Library> libraries = new ArrayList<>();
        long startedAt = System.nanoTime();
        int activeResolutions = ACTIVE_DEPENDENCY_RESOLUTIONS.incrementAndGet();
        String threadName = Thread.currentThread().getName();
        logInfoAndPrint("[DBN] Maven dependency resolution started: library=" + library.getLibraryId() +
                ", type=" + type + ", active=" + activeResolutions + ", thread=" + threadName);

        boolean resolved = false;
        try {
            RepositorySystem repositorySystem = newRepositorySystem();
            RepositorySystemSession session = newRepositorySystemSession(repositorySystem);

            DefaultArtifact artifact = new DefaultArtifact(library.getGroupId() + ":" + library.getArtifactId() + ":" + library.getVersion());

            RemoteRepository central = initCentralRepository();

            CollectRequest collectRequest = new CollectRequest();
            collectRequest.addDependency(new Dependency(artifact, "compile"));
            collectRequest.addRepository(central);

            downloadSession.updateProgress(library.getLibraryId());

            CollectResult collectResult = downloadSession.surround(() ->
                    Measured.call("collecting dependencies for library " + library,
                    () -> repositorySystem.collectDependencies(session, collectRequest)));

            DependencyNode root = collectResult.getRoot();
            if (type.equals("pom")) traverse(root.getChildren().get(0), libraries, library);
            else traverse(root.getChildren().get(0), libraries);

            resolved = true;
            return libraries;
        } finally {
            int activeAfterResolution = ACTIVE_DEPENDENCY_RESOLUTIONS.decrementAndGet();
            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
            logInfoAndPrint("[DBN] Maven dependency resolution " + (resolved ? "completed" : "failed") +
                    ": library=" + library.getLibraryId() +
                    ", type=" + type +
                    ", dependencies=" + libraries.size() +
                    ", elapsed=" + elapsedMillis + "ms" +
                    ", active=" + activeAfterResolution +
                    ", thread=" + threadName);
        }

    }

    private static RemoteRepository initCentralRepository() {
        RepositoryPolicy repositoryPolicy = new RepositoryPolicy(true,
                RepositoryPolicy.UPDATE_POLICY_DAILY,
                RepositoryPolicy.CHECKSUM_POLICY_FAIL);

        return new RemoteRepository.Builder("central", "default", MavenRepositories.CENTRAL_URL)
                .setPolicy(repositoryPolicy)
                .build();
    }

    // TEMP: mirror these diagnostics to stdout while comparing the IDE log and run consoles.
    private static void logInfoAndPrint(String message) {
        log.info(message);
        System.out.println(LocalDateTime.now().format(STDOUT_TIMESTAMP_FORMAT) + " " + message);
    }

    private static void traverse(DependencyNode node, List<Library> libraries) {
        if (node.getArtifact() != null) {
            doTraverse(node, libraries);
        }
        for (DependencyNode child : node.getChildren()) {
            traverse(child, libraries);
        }
    }

    private static void traverse(DependencyNode node, List<Library> libraries, Library rootLibrary) {
        if (node.getArtifact() != null && !rootLibrary.is(node.getArtifact())) {
            doTraverse(node, libraries);
        }
        for (DependencyNode child : node.getChildren()) {
            traverse(child, libraries);
        }
    }

    private static void doTraverse(DependencyNode node, List<Library> libraries) {
        String artifactKey = node.getArtifact().getGroupId() + ":" + node.getArtifact().getArtifactId() + ":" + node.getArtifact().getVersion();

        if (libraries.stream().noneMatch(lib -> lib.getArtifactId().equals(node.getArtifact().getArtifactId()))) {
            // Retrieve metadata from the map
            Pair<List<LibraryDeveloper>, List<LibraryLicense>> metadata = metadataMap.getOrDefault(artifactKey, Pair.of(emptyList(), emptyList()));
            libraries.add(new Library(node, metadata.first(), metadata.second()));
        }
    }

    static class CustomTransporter implements Transporter {
        @Override
        public void close() {
        }

        @Override
        public int classify(Throwable throwable) {
            return isNotFound(throwable) ? ERROR_NOT_FOUND : ERROR_OTHER;
        }

        @Override
        public void peek(PeekTask peekTask) throws Exception {
            File tempFile = FileUtil.createTempFile("dbn-maven-peek", ".tmp", true);
            try {
                String downloadUrl = buildFullUrl(peekTask.getLocation().toString());
                downloadMavenFile(downloadUrl, tempFile);
                verifyDownloadedFile(tempFile, downloadUrl);
            } finally {
                FileUtil.delete(tempFile);
            }
        }

        @Override
        public void get(GetTask task) throws Exception {
            String relativePath = task.getLocation().toString();

            int separatorIndex = relativePath.lastIndexOf("/");// Artifact path relative to the repository
            String fileName = separatorIndex == -1 ? relativePath : relativePath.substring(separatorIndex + 1);
            String fullUrl = buildFullUrl(relativePath);

            DownloadSession downloadSession = DownloadSession.current();
            downloadSession.updateProgress(fileName);
            if (downloadSession.isCanceled()) return;

            File dataFile = task.getDataFile();
            if (dataFile == null) {
                File tempFile = FileUtil.createTempFile("dbn-maven-get", ".tmp", true);
                try {
                    downloadMavenFile(fullUrl, tempFile);
                    verifyDownloadedFile(tempFile, fullUrl);
                    copyToTask(task, tempFile);
                } finally {
                    FileUtil.delete(tempFile);
                }
            } else {
                downloadMavenFile(fullUrl, dataFile);
                verifyDownloadedFile(dataFile, fullUrl);
                notifyTransferListener(task, dataFile);
            }
        }

        private static void copyToTask(GetTask task, File file) throws Exception {
            notifyTransferListener(task, file);
            try (FileInputStream fileInputStream = new FileInputStream(file);
                 BufferedInputStream inputStream = new BufferedInputStream(fileInputStream);
                 OutputStream outputStream = task.newOutputStream()) {
                inputStream.transferTo(outputStream);
            }
        }

        private static void notifyTransferListener(GetTask task, File file) throws Exception {
            TransportListener listener = task.getListener();
            listener.transportStarted(0, file.length());

            try (FileInputStream fileInputStream = new FileInputStream(file);
                 BufferedInputStream inputStream = new BufferedInputStream(fileInputStream)) {
                byte[] buffer = new byte[TRANSFER_BUFFER_SIZE];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    listener.transportProgressed(ByteBuffer.wrap(buffer, 0, length));
                }
            }
        }

        private static void verifyDownloadedFile(File file, String downloadUrl) throws IOException {
            if (file.exists() && file.length() > 0) return;

            FileUtil.delete(file);
            throw new IOException("Downloaded empty Maven artifact: " + downloadUrl);
        }

        private static String buildFullUrl(String relativePath) {
            return MavenRepositories.CENTRAL_URL + "/" + relativePath;
        }

        private static void downloadMavenFile(String downloadUrl, File outputFile) throws IOException {
            long startedAt = System.nanoTime();
            int activeTransfers = ACTIVE_MAVEN_TRANSFERS.incrementAndGet();
            String threadName = Thread.currentThread().getName();
            log.debug("[DBN] Maven transfer started: active={}, thread={}, url={}", activeTransfers, threadName, downloadUrl);

            boolean downloaded = false;
            try {
                Downloads.downloadAtomically(null, downloadUrl, outputFile);
                downloaded = true;
            } finally {
                long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
                int activeAfterTransfer = ACTIVE_MAVEN_TRANSFERS.decrementAndGet();
                if (elapsedMillis >= 2_000) {
                    logInfoAndPrint("[DBN] Maven transfer " + (downloaded ? "completed" : "failed") +
                            ": elapsed=" + elapsedMillis + "ms" +
                            ", active=" + activeAfterTransfer +
                            ", thread=" + threadName +
                            ", url=" + downloadUrl);
                }
                log.debug("[DBN] Maven transfer {}: elapsed={}ms, active={}, thread={}, url={}",
                        downloaded ? "completed" : "failed", elapsedMillis, activeAfterTransfer, threadName, downloadUrl);
            }
        }

        private static boolean isNotFound(Throwable throwable) {
            for (Throwable current = throwable; current != null; current = current.getCause()) {
                String message = current.getMessage();
                if (message == null) continue;
                if (message.contains("404") || message.contains("Not Found") || message.contains("not found")) return true;
            }
            return false;
        }

        @Override
        public void put(PutTask task) throws Exception {
            throw new UnsupportedOperationException("Uploads are not supported.");
        }
    }

    // Factory for the custom transporter
    static class CustomTransporterFactory implements TransporterFactory {

        @Override
        public Transporter newInstance(RepositorySystemSession repositorySystemSession, RemoteRepository remoteRepository) throws NoTransporterException {
            return new CustomTransporter();
        }

        @Override
        public float getPriority() {
            return 100; // Higher priority
        }
    }

    private static RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();

        // Add required services
        locator.addService(TransporterFactory.class, CustomTransporterFactory.class);
        locator.setService(LocalRepositoryManagerFactory.class, EnhancedLocalRepositoryManagerFactory.class);
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.setService(ArtifactResolver.class, CustomArtifactResolver.class);

        return locator.getService(RepositorySystem.class);
    }

    private static RepositorySystemSession newRepositorySystemSession(RepositorySystem system) throws IOException {
        Path localRepositoryPath = getMavenCachePath();

        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        session.setConfigProperty("aether.syncContext.named.factory", "file-lock");
        session.setConfigProperty("aether.syncContext.named.nameMapper", "file-gaecv");

        LocalRepository localRepo = new LocalRepository(localRepositoryPath.toFile());
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
        session.setSystemProperty("java.version", System.getProperty("java.version"));

        return session;
    }

    private static Path getMavenCachePath() throws IOException {
        Path cachePath = Path.of(PathManager.getSystemPath(), "dbn-driver-maven-cache-v1");
        Files.createDirectories(cachePath);
        FilePermissions.restrictToOwner(cachePath);
        return cachePath;
    }

    static class CustomArtifactResolver extends DefaultArtifactResolver {
        @Override
        public ArtifactResult resolveArtifact(RepositorySystemSession session, ArtifactRequest request) throws ArtifactResolutionException {
            if (!"pom".equals(request.getArtifact().getExtension())) {
                return super.resolveArtifact(session, request);
            }

            String lockKey = request.getArtifact().toString();
            ReentrantLock processLock = POM_CACHE_LOCKS.computeIfAbsent(lockKey, key -> new ReentrantLock());
            processLock.lock();
            try {
                return resolvePomArtifact(session, request);
            } catch (IOException e) {
                throw artifactResolutionException(request, "Could not lock or verify the DBN Maven cache", e);
            } finally {
                processLock.unlock();
            }
        }

        private ArtifactResult resolvePomArtifact(RepositorySystemSession session, ArtifactRequest request)
                throws IOException, ArtifactResolutionException {
            Path lockDirectory = getMavenCachePath().resolve(".dbn-integrity-locks");
            Files.createDirectories(lockDirectory);
            FilePermissions.restrictToOwner(lockDirectory);
            Path lockPath = lockDirectory.resolve(sha256(request.getArtifact().toString()) + ".lock");
            if (Files.isSymbolicLink(lockPath)) {
                throw new IOException("Refusing to use a symbolic link as a Maven cache lock: " + lockPath);
            }

            try (FileChannel lockChannel = FileChannel.open(
                    lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
                 FileLock ignored = lockChannel.lock()) {
                return resolvePomArtifactLocked(session, request);
            }
        }

        private ArtifactResult resolvePomArtifactLocked(RepositorySystemSession session, ArtifactRequest request)
                throws IOException, ArtifactResolutionException {
            String cachedDigest = validateCachedPom(session, request);
            ArtifactResult result = super.resolveArtifact(session, request);

            Artifact artifact = result.getArtifact();
            File pomFile = artifact.getFile();
            if (pomFile == null) {
                throw artifactResolutionException(request, "Resolved POM has no local file", null);
            }

            String resolvedDigest = Checksum.fromFileContent(pomFile, ChecksumType.SHA_256);
            if (cachedDigest != null && !cachedDigest.equalsIgnoreCase(resolvedDigest) &&
                    result.getRepository() instanceof LocalRepository) {
                // The local cache changed after validation. Discard it and force a checksum-verified remote fetch.
                deleteCachedPom(pomFile);
                result = super.resolveArtifact(session, request);
                artifact = result.getArtifact();
                pomFile = artifact == null ? null : artifact.getFile();
                if (pomFile == null || !(result.getRepository() instanceof RemoteRepository)) {
                    throw artifactResolutionException(request, "Could not re-resolve a changed cached POM", null);
                }
            }

            writeCachedPomDigest(pomFile);
            parsePomForMetadata(pomFile);

            return result;
        }

        private static String validateCachedPom(RepositorySystemSession session, ArtifactRequest request)
                throws ArtifactResolutionException {
            LocalArtifactRequest localRequest = new LocalArtifactRequest(
                    request.getArtifact(), request.getRepositories(), request.getRequestContext());
            LocalArtifactResult localResult = session.getLocalRepositoryManager().find(session, localRequest);
            File pomFile = localResult.getFile();
            if (pomFile == null) return null;

            Path pomPath = pomFile.toPath();
            Path digestPath = cachedPomDigestPath(pomFile);
            try {
                if (!Files.exists(pomPath, LinkOption.NOFOLLOW_LINKS) &&
                        !Files.exists(digestPath, LinkOption.NOFOLLOW_LINKS)) return null;
                if (!localResult.isAvailable() || Files.isSymbolicLink(pomPath) || Files.isSymbolicLink(digestPath) ||
                        !Files.isRegularFile(pomPath) || !Files.isRegularFile(digestPath)) {
                    deleteCachedPom(pomFile);
                    return null;
                }

                String expectedDigest = Files.readString(digestPath, StandardCharsets.US_ASCII).trim();
                String actualDigest = Checksum.fromFileContent(pomFile, ChecksumType.SHA_256);
                if (!expectedDigest.equalsIgnoreCase(actualDigest)) {
                    deleteCachedPom(pomFile);
                    return null;
                }
                return actualDigest;
            } catch (IOException e) {
                throw artifactResolutionException(request, "Could not verify cached POM integrity", e);
            }
        }

        private static void writeCachedPomDigest(File pomFile) throws IOException {
            Path digestPath = cachedPomDigestPath(pomFile);
            Path temporaryFile = FilePermissions.createOwnerOnlyTempFile(
                    digestPath.getParent(), "dbn-pom-integrity-", ".tmp");
            try {
                String digest = Checksum.fromFileContent(pomFile, ChecksumType.SHA_256);
                Files.writeString(temporaryFile, digest, StandardCharsets.US_ASCII);
                try {
                    Files.move(temporaryFile, digestPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temporaryFile, digestPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporaryFile);
            }
        }

        private static void deleteCachedPom(File pomFile) throws IOException {
            Files.deleteIfExists(pomFile.toPath());
            Files.deleteIfExists(cachedPomDigestPath(pomFile));
        }

        private static Path cachedPomDigestPath(File pomFile) {
            return pomFile.toPath().resolveSibling(pomFile.getName() + ".dbn-sha256");
        }

        private static String sha256(String value) throws IOException {
            try {
                byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
                return HexFormat.of().formatHex(digest);
            } catch (NoSuchAlgorithmException e) {
                throw new IOException("SHA-256 is unavailable", e);
            }
        }

        private static ArtifactResolutionException artifactResolutionException(
                ArtifactRequest request, String message, Throwable cause) {
            return new ArtifactResolutionException(List.of(new ArtifactResult(request)), message, cause);
        }

        private static void parsePomForMetadata(File pomFile) {
            try (FileInputStream fis = new FileInputStream(pomFile)) {
                MavenXpp3Reader reader = new MavenXpp3Reader();
                InputSource inputSource = new InputSource(fis);
                inputSource.setSystemId(pomFile.getAbsolutePath());
                Model model = reader.read(inputSource.getByteStream());

                // Extract artifact coordinates
                String artifactKey = model.getGroupId() + ":" + model.getArtifactId() + ":" + model.getVersion();

                // Extract developers & licenses
                List<LibraryDeveloper> devs = convertDevelopers(model.getDevelopers());
                List<LibraryLicense> lic = convertLicenses(model.getLicenses());

                // Store metadata in map
                metadataMap.put(artifactKey, Pair.of(devs, lic));

            } catch (Exception e) {
                log.warn("Failed to parse pom file '{}'", pomFile, e);
            }
        }

        private static List<LibraryDeveloper> convertDevelopers(List<org.apache.maven.model.Developer> mavenDevelopers) {
            return convert(mavenDevelopers, d -> new LibraryDeveloper(d.getName(), d.getUrl()));
        }

        private static List<LibraryLicense> convertLicenses(List<org.apache.maven.model.License> mavenLicenses) {
            return convert(mavenLicenses, l -> new LibraryLicense(l.getName(), l.getUrl()));
        }
    }
}

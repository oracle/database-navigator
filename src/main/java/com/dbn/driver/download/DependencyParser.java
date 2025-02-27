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
import com.dbn.driver.download.metadata.Developer;
import com.dbn.driver.download.metadata.Library;
import com.dbn.driver.download.metadata.License;
import com.intellij.platform.templates.github.DownloadUtil;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.internal.impl.DefaultArtifactResolver;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.*;
import org.eclipse.aether.spi.connector.transport.PeekTask;
import org.eclipse.aether.spi.connector.transport.Transporter;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.spi.connector.transport.GetTask;
import org.eclipse.aether.spi.connector.transport.PutTask;
import org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory;
import org.eclipse.aether.transfer.NoTransporterException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.stream.Collectors;

public class DependencyParser {
    private static String central_url;
    private static final Map<String, Pair<List<Developer>, List<License>>> metadataMap = new HashMap<>();

    public static List<Library> resolveDependencies(Library library, String type) throws Exception {
        List<Library> libraries = new ArrayList<>();
        // Run the network operation in a background thread

        RepositorySystem repositorySystem = newRepositorySystem();

        RepositorySystemSession session = newRepositorySystemSession(repositorySystem);

        DefaultArtifact artifact2 = new DefaultArtifact(library.getGroupId() + ":" + library.getArtifactId() + ":" + library.getVersion());

        RemoteRepository central = new RemoteRepository.Builder("central", "default", "https://repo1.maven.org/maven2/")
                .setPolicy(new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_ALWAYS, RepositoryPolicy.CHECKSUM_POLICY_IGNORE))
                .build();
        central_url = central.getUrl();

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.addDependency(new Dependency(artifact2, "compile"));
        collectRequest.addRepository(central);

        CollectResult collectResult = repositorySystem.collectDependencies(session, collectRequest);

        DependencyNode root = collectResult.getRoot();
        if (type.equals("pom")) traverse(root.getChildren().get(0), libraries, library);
        else traverse(root.getChildren().get(0), libraries);

        return libraries;

    }

    private static void traverse(DependencyNode node, List<Library> libraries) {
        if (node.getArtifact() != null) {
            String artifactKey = node.getArtifact().getGroupId() + ":" + node.getArtifact().getArtifactId() + ":" + node.getArtifact().getVersion();

            if (libraries.stream().noneMatch(lib -> lib.getArtifactId().equals(node.getArtifact().getArtifactId()))) {
                // Retrieve metadata from the map
                Pair<List<Developer>, List<License>> metadata = metadataMap.getOrDefault(artifactKey, Pair.of(Collections.emptyList(), Collections.emptyList()));
                libraries.add(new Library(node, metadata.first(), metadata.second()));
            }
        }
        for (DependencyNode child : node.getChildren()) {
            traverse(child, libraries);
        }
    }

    private static void traverse(DependencyNode node, List<Library> libraries, Library rootLibrary) {
        if (node.getArtifact() != null && !rootLibrary.is(node.getArtifact())) {
            String artifactKey = node.getArtifact().getGroupId() + ":" + node.getArtifact().getArtifactId() + ":" + node.getArtifact().getVersion();

            if (libraries.stream().noneMatch(lib -> lib.getArtifactId().equals(node.getArtifact().getArtifactId()))) {
                // Retrieve metadata from the map
                Pair<List<Developer>, List<License>> metadata = metadataMap.getOrDefault(artifactKey, Pair.of(Collections.emptyList(), Collections.emptyList()));
                libraries.add(new Library(node, metadata.first(), metadata.second()));
            }
        }
        for (DependencyNode child : node.getChildren()) {
            traverse(child, libraries);
        }
    }


    private static RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();

        // Add required services
        locator.addService(TransporterFactory.class, CustomTransporterFactory.class);
        locator.addService(LocalRepositoryManagerFactory.class, org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory.class);
        locator.addService(org.eclipse.aether.spi.connector.RepositoryConnectorFactory.class,
                org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory.class);
        locator.setService(ArtifactResolver.class, CustomArtifactResolver.class);
        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
                exception.printStackTrace();
            }
        });

        return locator.getService(RepositorySystem.class);
    }


    private static RepositorySystemSession newRepositorySystemSession(RepositorySystem system) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo = new LocalRepository("target/local-repo");
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
        session.setSystemProperty("java.version", System.getProperty("java.version"));

        return session;
    }

    // Custom Transporter
    static class CustomTransporter implements Transporter {
        @Override
        public void close() {
        }

        @Override
        public int classify(Throwable throwable) {
            return 0;
        }

        @Override
        public void peek(PeekTask peekTask) {

        }

        @Override
        public void get(GetTask task) throws Exception {
            try {
                File targetFile = new File(task.getDataFile().getAbsolutePath());

                String relativePath = task.getLocation().toString(); // Artifact path relative to the repository

                String fullUrl = central_url.endsWith("/") ? central_url + relativePath : central_url + "/" + relativePath;

                System.out.println("Downloading from: " + fullUrl); // Debug: Log the full URL

                // Use DownloadUtil to download the artifact and ensure ideal
                // handling of download ( taking proxy into consideration )
                DownloadUtil.downloadAtomically(null, fullUrl, targetFile);

            } catch (Exception e) {
                throw new Exception("Error during download", e);
            }
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


    static class CustomArtifactResolver extends DefaultArtifactResolver {


        @Override
        public ArtifactResult resolveArtifact(RepositorySystemSession session, ArtifactRequest request) throws ArtifactResolutionException {
            ArtifactResult result = super.resolveArtifact(session, request);

            // Check if the resolved artifact is a POM file
            Artifact artifact = result.getArtifact();
            if ("pom".equals(artifact.getExtension())) {
                File pomFile = artifact.getFile();
                if (pomFile != null) {
                    parsePomForMetadata(pomFile);
                }
            }

            return result;
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
                List<Developer> devs = convertDevelopers(model.getDevelopers());
                List<License> lic = convertLicenses(model.getLicenses());

                // Store metadata in map
                metadataMap.put(artifactKey, Pair.of(devs, lic));

            } catch (Exception e) {
                System.err.println("Failed to parse POM: " + e.getMessage());
            }
        }

        private static List<Developer> convertDevelopers(List<org.apache.maven.model.Developer> mavenDevelopers) {
            if (mavenDevelopers == null) {
                return Collections.emptyList();
            }
            return mavenDevelopers.stream()
                    .map(dev -> new Developer(dev.getName(), dev.getUrl()))
                    .collect(Collectors.toList());
        }

        private static List<License> convertLicenses(List<org.apache.maven.model.License> mavenLicenses) {
            if (mavenLicenses == null) {
                return Collections.emptyList();
            }
            return mavenLicenses.stream()
                    .map(lic -> new License(lic.getName(), lic.getUrl()))
                    .collect(Collectors.toList());
        }
    }
}
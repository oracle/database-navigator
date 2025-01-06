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
package com.dbn.driver.packages.parser;

import com.dbn.driver.packages.Library;
import com.intellij.platform.templates.github.DownloadUtil;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.*;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.spi.connector.transport.PeekTask;
import org.eclipse.aether.spi.connector.transport.Transporter;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.spi.connector.transport.GetTask;
import org.eclipse.aether.spi.connector.transport.PutTask;
import org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory;
import org.eclipse.aether.transfer.NoTransporterException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.impl.DefaultServiceLocator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DependencyParser {
    private static String central_url;

    public static List<Library> resolveDependencies(Library library, String type) {
        List<Library> libraries = new ArrayList<>();
        // Run the network operation in a background thread
        try {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return libraries;

    }

    private static void traverse(DependencyNode node, List<Library> libraries) {
        if (node.getArtifact() != null) {
            if (libraries.stream().noneMatch(lib -> lib.getArtifactId().equals(node.getArtifact().getArtifactId()))) {
                libraries.add(new Library(node));
            }
        }
        for (DependencyNode child : node.getChildren()) {
            traverse(child, libraries);
        }
    }

    private static void traverse(DependencyNode node, List<Library> libraries, Library rootLibrary) {
        if (node.getArtifact() != null && !rootLibrary.is(node.getArtifact())) {
            if (libraries.stream().noneMatch(lib -> lib.getArtifactId().equals(node.getArtifact().getArtifactId()))) {
                libraries.add(new Library(node));
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
        locator.addService(org.eclipse.aether.spi.connector.RepositoryConnectorFactory.class, org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory.class);

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
        public void peek(PeekTask peekTask) throws Exception {

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
}

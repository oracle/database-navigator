package com.dbn.driver.packages.parser;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.util.net.HttpConfigurable;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.*;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.repository.DefaultProxySelector;

import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class DependencyParser {

    public static void execute() throws Exception {
        String groupId = "com.oracle.database.jdbc";
        String artifactId = "ojdbc17-production";
        String version = "23.6.0.24.10";

        Artifact artifact = new DefaultArtifact(groupId, artifactId, "pom", version);

        RepositorySystem system = newRepositorySystem();
        RepositorySystemSession session = newRepositorySystemSession(system);

        RemoteRepository central = new RemoteRepository.Builder("central", "default", "https://repo1.maven.org/maven2/")
                .build();

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, ""));
        collectRequest.addRepository(central);

        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, null);
        DependencyResult dependencyResult = system.resolveDependencies(session, dependencyRequest);

        for (ArtifactResult artifactResult : dependencyResult.getArtifactResults()) {
            Artifact resolvedArtifact = artifactResult.getArtifact();
            System.out.println("Downloaded: " + resolvedArtifact.getFile());
        }
    }

    private static RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        return locator.getService(RepositorySystem.class);
    }

    private static RepositorySystemSession newRepositorySystemSession(RepositorySystem system) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo = new LocalRepository(new File("target/local-repo"));
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

        return session;
    }

}
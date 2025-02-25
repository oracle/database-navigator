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

package com.dbn.driver.packages;

import com.intellij.platform.templates.github.DownloadUtil;
import lombok.Getter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;

import java.io.FileInputStream;
import java.util.List;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.xml.sax.InputSource;

import java.io.File;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Library holds the metadata for a Maven dependency required by a driver package.
 * The information includes groupId, artifactId, and version.
 * <p>
 * Example:
 * <pre>
 * {@code
 * <library group-id="javax.resource" artifact-id="connector-api" version="1.5"/>
 * }
 * </pre>
 *
 * @author Ayoub Aarrasse
 */
@Getter
public class Library {
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final List<Developer> developers;
    private final List<License> licenses;

    public Library(String groupId, String artifactId, String version, List<Developer> developers, List<License> licenses) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.developers = developers;
        this.licenses = licenses;
    }


    // Constructor using DependencyNode
    public Library(DependencyNode node) {
        this(node.getArtifact().getGroupId(),
                node.getArtifact().getArtifactId(),
                node.getArtifact().getVersion());
    }

    // New Constructor using groupId, artifactId, and version
    public Library(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;

        File pomFile = downloadPomFile(groupId, artifactId, version);
        if (pomFile != null) {
            Model model = parsePom(pomFile);
            if (model != null) {
                this.developers = convertDevelopers(model.getDevelopers());
                this.licenses = convertLicenses(model.getLicenses());
            } else {
                this.developers = Collections.emptyList();
                this.licenses = Collections.emptyList();
            }
        } else {
            this.developers = Collections.emptyList();
            this.licenses = Collections.emptyList();
        }
    }

    private File downloadPomFile(String groupId, String artifactId, String version) {
        try {
            String pomUrl = constructPomUrl(groupId, artifactId, version);
            File tempFile = File.createTempFile("artifact-pom", ".xml");
            DownloadUtil.downloadAtomically(null, pomUrl, tempFile);
            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Model parsePom(File pomFile) {
        try (FileInputStream fis = new FileInputStream(pomFile)) {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            InputSource inputSource = new InputSource(fis);
            inputSource.setSystemId(pomFile.getAbsolutePath());
            return reader.read(inputSource.getByteStream());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String constructPomUrl(String groupId, String artifactId, String version) {
        String groupPath = groupId.replace(".", "/");
        return String.format("https://repo.maven.apache.org/maven2/%s/%s/%s/%s-%s.pom",
                groupPath, artifactId, version, artifactId, version);
    }

    private List<Developer> convertDevelopers(List<org.apache.maven.model.Developer> mavenDevelopers) {
        if (mavenDevelopers == null) {
            return Collections.emptyList();
        }
        return mavenDevelopers.stream()
                .map(dev -> new Developer(dev.getName(), dev.getUrl()))
                .collect(Collectors.toList());
    }

    private List<License> convertLicenses(List<org.apache.maven.model.License> mavenLicenses) {
        if (mavenLicenses == null) {
            return Collections.emptyList();
        }
        return mavenLicenses.stream()
                .map(lic -> new License(lic.getName(), lic.getUrl()))
                .collect(Collectors.toList());
    }

    public boolean is(Artifact artifact) {
        if (artifact == null) return false;
        return artifact.getArtifactId().equals(this.artifactId)
                && artifact.getGroupId().equals(this.groupId)
                && artifact.getVersion().equals(this.version);
    }

    @Override
    public String toString() {
        return String.format("Library [groupId=%s, artifactId=%s, version=%s, developers=%s, licenses=%s]",
                groupId, artifactId, version,
                developers.stream().map(Developer::getName).collect(Collectors.joining(", ")),
                licenses.stream().map(License::getName).collect(Collectors.joining(", ")));
    }
}
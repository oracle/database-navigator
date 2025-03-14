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

package com.dbn.driver.download.metadata;

import com.dbn.common.state.PersistentStateElement;
import lombok.Getter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

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
public class Library implements PersistentStateElement {
    private final String groupId;
    private final String artifactId;
    private final String version;
    private List<Developer> developers = new ArrayList<>();
    private List<License> licenses = new ArrayList<>();

    public Library(String groupId, String artifactId, String version, List<Developer> developers, List<License> licenses) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.developers = developers;
        this.licenses = licenses;
    }

    // Constructor using DependencyNode
    public Library(DependencyNode node, List<Developer> developers, List<License> licenses) {
        this(node.getArtifact().getGroupId(),
                node.getArtifact().getArtifactId(),
                node.getArtifact().getVersion(), developers, licenses);
    }

    // New Constructor using groupId, artifactId, and version
    public Library(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public boolean is(Artifact artifact) {
        if (artifact == null) return false;
        return artifact.getArtifactId().equals(this.artifactId)
                && artifact.getGroupId().equals(this.groupId)
                && artifact.getVersion().equals(this.version);
    }

    public String getArtefactPath() {
        return groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar";
    }

    public String getLibraryId() {
        return artifactId + "-" + version;
    }

    @Override
    public String toString() {
        return String.format("Library [groupId=%s, artifactId=%s, version=%s, developers=%s, licenses=%s]",
                groupId, artifactId, version,
                developers.stream().map(Developer::getName).collect(Collectors.joining(", ")),
                licenses.stream().map(License::getName).collect(Collectors.joining(", ")));
    }

    @Override
    public void readState(Element element) {
        // Read developers
        this.developers = element.getChildren("developer").stream()
                .map(e -> new Developer(stringAttribute(e, "name"), stringAttribute(e, "url")))
                .collect(Collectors.toList());

        // Read licenses
        this.licenses = element.getChildren("license").stream()
                .map(e -> new License(stringAttribute(e, "name"), stringAttribute(e, "url")))
                .collect(Collectors.toList());
    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "group-id", groupId);
        setStringAttribute(element, "artifact-id", artifactId);
        setStringAttribute(element, "version", version);

        for (Developer dev : this.developers) {
            Element devElement = newElement(element, "developer");
            setStringAttribute(devElement, "name", dev.getName());
            setStringAttribute(devElement, "url", dev.getUrl());
        }

        for (License lic : this.licenses) {
            Element licElement = newElement(element, "license");
            setStringAttribute(licElement, "name", lic.getName());
            setStringAttribute(licElement, "url", lic.getUrl());
        }
    }

}
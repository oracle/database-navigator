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
import lombok.NoArgsConstructor;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.dbn.common.options.setting.Settings.childrenOf;
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
@NoArgsConstructor
public class Library implements PersistentStateElement {
    private String groupId;
    private String artifactId;
    private String version;
    private List<LibraryDeveloper> developers = new ArrayList<>();
    private List<LibraryLicense> licenses = new ArrayList<>();
    private List<LibraryChecksum> checksums = new ArrayList<>();

    public Library(String groupId, String artifactId, String version, List<LibraryDeveloper> developers, List<LibraryLicense> licenses) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.developers = developers;
        this.licenses = licenses;
    }

    // Constructor using DependencyNode
    public Library(DependencyNode node, List<LibraryDeveloper> developers, List<LibraryLicense> licenses) {
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
        return getGroupPath() + "/" + artifactId + "/" + version + "/" + getFileName();
    }

    public String getLibraryId() {
        return artifactId + "-" + version;
    }

    public String getGroupPath() {
        return groupId.replace(".", "/");
    }

    public String getFileName() {
        return artifactId + "-" + version + ".jar";
    }

    @Override
    public String toString() {
        return String.format("Library [groupId=%s, artifactId=%s, version=%s, developers=%s, licenses=%s]",
                groupId, artifactId, version,
                developers.stream().map(LibraryDeveloper::getName).collect(Collectors.joining(", ")),
                licenses.stream().map(LibraryLicense::getName).collect(Collectors.joining(", ")));
    }

    @Override
    public void readState(Element element) {
        if (element == null) return;

        this.groupId = stringAttribute(element, "group-id");
        this.artifactId = stringAttribute(element, "artifact-id");
        this.version = stringAttribute(element, "version");

        this.developers = new ArrayList<>();
        for (Element developerElement : childrenOf(element, "developer")) {
            LibraryDeveloper developer = new LibraryDeveloper();
            developer.readState(developerElement);
            developers.add(developer);
        }

        this.licenses = new ArrayList<>();
        for (Element licenseElement : childrenOf(element, "license")) {
            LibraryLicense license = new LibraryLicense();
            license.readState(licenseElement);
            licenses.add(license);
        }

        this.checksums = new ArrayList<>();
        for (Element checksumElement : childrenOf(element, "checksum")) {
            LibraryChecksum checksum = new LibraryChecksum();
            checksum.readState(checksumElement);
            checksums.add(checksum);
        }
    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "group-id", groupId);
        setStringAttribute(element, "artifact-id", artifactId);
        setStringAttribute(element, "version", version);

        for (LibraryDeveloper developer : this.developers) {
            Element developerElement = newElement(element, "developer");
            developer.writeState(developerElement);
        }

        for (LibraryLicense license : this.licenses) {
            Element licenseElement = newElement(element, "license");
            license.writeState(licenseElement);
        }

        for (LibraryChecksum checksum : this.checksums) {
            Element checksumElement = newElement(element, "checksum");
            checksum.writeState(checksumElement);
        }
    }

}

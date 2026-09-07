/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.common.cloud;

import com.dbn.common.state.PersistentStateElement;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

/**
 * Configuration for cloud object storage data source.
 * Supports OCI, AWS S3, Azure Blob, and GCP via DBMS_CLOUD.
 */
@Getter
@Setter
public class CloudSourceConfig implements PersistentStateElement {
    private String fileUri;
    private boolean noCredential = false;
    private String credentialSchemaName;
    private String credentialName;
    private String delimiter = ",";
    private boolean hasHeader = true;
    private List<String> discoveredColumns = new ArrayList<>();
    private Set<String> numericColumns = new HashSet<>();

    @Override
    public void readState(Element element) {
        if (element == null) return;

        fileUri = stringAttribute(element, "file-uri");
        noCredential = Boolean.parseBoolean(stringAttribute(element, "no-credential", "false"));
        credentialSchemaName = stringAttribute(element, "credential-schema");
        credentialName = stringAttribute(element, "credential");
        delimiter = stringAttribute(element, "delimiter", delimiter);
        hasHeader = Boolean.parseBoolean(stringAttribute(element, "has-header", "true"));
    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "file-uri", fileUri);
        setStringAttribute(element, "no-credential", String.valueOf(noCredential));
        setStringAttribute(element, "credential-schema", credentialSchemaName);
        setStringAttribute(element, "credential", credentialName);
        setStringAttribute(element, "delimiter", delimiter);
        setStringAttribute(element, "has-header", String.valueOf(hasHeader));
    }
}

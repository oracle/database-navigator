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

package com.dbn.mcp.registry;

import com.dbn.common.state.PersistentStateElement;
import lombok.Data;
import org.jdom.Element;

import static com.dbn.common.options.setting.Settings.longAttribute;
import static com.dbn.common.options.setting.Settings.setLongAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

@Data
public class McpDeploymentInfo implements PersistentStateElement {
    private String endpoint;
    private String applicationName;
    private String imageOcid;
    private long deployTimestamp;

    // registry coordinates, kept so a later step or a retry does not ask for them again
    private String regionKey;
    private String namespace;
    private String repository;
    private String tag;

    // when each deployment step last succeeded; a step is complete when its stamp is set
    private long imageBuiltAt;
    private long imagePushedAt;

    @Override
    public void readState(Element element) {
        endpoint = stringAttribute(element, "endpoint");
        applicationName = stringAttribute(element, "application-name");
        imageOcid = stringAttribute(element, "image-ocid");
        deployTimestamp = longAttribute(element, "deploy-timestamp", 0);
        regionKey = stringAttribute(element, "region-key");
        namespace = stringAttribute(element, "namespace");
        repository = stringAttribute(element, "repository");
        tag = stringAttribute(element, "tag");
        imageBuiltAt = longAttribute(element, "image-built-at", 0);
        imagePushedAt = longAttribute(element, "image-pushed-at", 0);
    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "endpoint", endpoint);
        setStringAttribute(element, "application-name", applicationName);
        setStringAttribute(element, "image-ocid", imageOcid);
        setLongAttribute(element, "deploy-timestamp", deployTimestamp);
        setStringAttribute(element, "region-key", regionKey);
        setStringAttribute(element, "namespace", namespace);
        setStringAttribute(element, "repository", repository);
        setStringAttribute(element, "tag", tag);
        setLongAttribute(element, "image-built-at", imageBuiltAt);
        setLongAttribute(element, "image-pushed-at", imagePushedAt);
    }
}

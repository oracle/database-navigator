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

package com.dbn.vector.model.request;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Json;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

@Getter
@Setter
public class EmbeddingModelThirdPartySpec implements PersistentStateElement {
  private String provider;
  private String credentialSchemaName;
  private String credentialName;
  private String endpointUrl;
  private String modelName;

  public String getConfigJson() {
    Map params = getConfigMap();
    return Json.writeAsString(params);
  }

  @NotNull
  public Map<String, ?> getConfigMap() {
    return Map.of(
            "provider", provider,
            "credential_name", credentialSchemaName + "." + credentialName,
            "url", endpointUrl,
            "model", modelName);
  }

  @Override
  public void readState(Element element) {
    if (element == null) return;

    provider = stringAttribute(element, "provider");
    credentialSchemaName = stringAttribute(element, "credential-schema");
    credentialName = stringAttribute(element, "credential");
    endpointUrl = stringAttribute(element, "url");
    modelName = stringAttribute(element, "model");
  }

  @Override
  public void writeState(Element element) {
    setStringAttribute(element, "provider", provider);
    setStringAttribute(element, "credential-schema", credentialSchemaName);
    setStringAttribute(element, "credential", credentialName);
    setStringAttribute(element, "url", endpointUrl);
    setStringAttribute(element, "model", modelName);
  }
}

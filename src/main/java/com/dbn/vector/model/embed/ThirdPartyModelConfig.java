package com.dbn.vector.model.embed;

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
public class ThirdPartyModelConfig implements PersistentStateElement {
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

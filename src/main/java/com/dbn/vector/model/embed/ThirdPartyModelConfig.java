package com.dbn.vector.model.embed;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Json;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jdom.Element;

import java.util.Map;

import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

@Getter
@Setter
@NoArgsConstructor
public class ThirdPartyModelConfig implements PersistentStateElement {
  private String provider;
  private String credentialName;
  private String url;
  private String model;

  public ThirdPartyModelConfig(String providerName, String credentialName, String url, String modelName) {
    this.provider = providerName;
    this.credentialName = credentialName;
    this.url = url;
    this.model = modelName;
  }

  public String getConfigJson() {
    Map params = Map.of(
            "provider", provider,
            "CredentialName", credentialName,
            "url", url,
            "model", model
    );
    return Json.writeAsString(params);
  }

  @Override
  public void readState(Element element) {
    if (element == null) return;

    provider = stringAttribute(element, "provider");
    credentialName = stringAttribute(element, "credential-name");
    url = stringAttribute(element, "url");
    model = stringAttribute(element, "model");
  }

  @Override
  public void writeState(Element element) {
    setStringAttribute(element, "provider", provider);
    setStringAttribute(element, "credential-name", credentialName);
    setStringAttribute(element, "url", url);
    setStringAttribute(element, "model", model);
  }
}

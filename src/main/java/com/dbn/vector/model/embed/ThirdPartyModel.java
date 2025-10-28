package com.dbn.vector.model.embed;

import com.dbn.common.util.Json;
import lombok.Getter;

import java.util.Map;

@Getter
public class ThirdPartyModel extends EmbedConfig{
  private final String provider ;
  private final String credentialName;
  private final String url;
  private final String model;
  public ThirdPartyModel(String provider, String CredentialName, String url, String Model) {
    this.provider = provider;
    this.credentialName = CredentialName;
    this.url = url;
    this.model = Model;
  }

  @Override
  public String getConfigJson() {
    Map params = Map.of(
            "provider", provider,
            "CredentialName", credentialName,
            "url", url,
            "model", model
    );
    return Json.writeAsString(params);
  }
}

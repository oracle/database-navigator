package com.dbn.vector.model.embed;

import com.dbn.common.util.Json;
import lombok.Getter;

@Getter
public class ThirdPartyModel extends EmbedConfig{
  private final String provider ;
  private final String CredentialName ;
  private final String url;
  private final String Model;
  public ThirdPartyModel(String provider, String CredentialName, String url, String Model) {
    this.provider = provider;
    this.CredentialName = CredentialName;
    this.url = url;
    this.Model = Model;
  }

  @Override
  public String getConfigJson() {
    String embedConfig = "{"
            + "\"provider\":\"" + provider + "\","
            + "\"CredentialName\":\"" + CredentialName + "\","
            + "\"url\":\"" + url + "\","
            + "\"Model\":\"" + Model + "\""
            +"}";
    return "" ;
  }
}

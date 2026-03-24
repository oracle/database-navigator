package com.dbn.connection.config.configprovider;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SecretRef {

    SecretProviderType type;
    String value;
    String fieldName;
    SecretAuthentication authentication;

}

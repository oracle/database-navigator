package com.dbn.connection.config.export;

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

package com.dbn.connection.config.configprovider;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class SecretAuthentication {

    String method;
    Map<String,Object> parameters;
}

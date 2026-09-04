package com.dbn.connection.config.export;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class ConfigProviderPayload {

    String connectDescriptor;
    String user;
    SecretRef password;
    SecretRef walletLocation;

    Map<String,Object> jdbc;
}

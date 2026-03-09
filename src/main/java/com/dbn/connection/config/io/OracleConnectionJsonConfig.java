package com.dbn.connection.config.io;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OracleConnectionJsonConfig {
    @JsonProperty("connect_descriptor")
    private String connectDescriptor;

    private String user;
    private SecretRef password;

    @JsonProperty("wallet_location")
    private SecretRef walletLocation;

    private Map<String,Object> jdbc;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SecretRef{
        private String type;
        private String value;

        @JsonProperty("field_name")
        private String fieldName;

        private Authentication authentication;

    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Authentication{

        private String method;
        private Map<String,Object> parameters;


    }
}

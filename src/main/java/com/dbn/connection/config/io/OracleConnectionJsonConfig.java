package com.dbn.connection.config.io;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OracleConnectionJsonConfig {
    @JsonProperty("connect_descriptor")
     String connectDescriptor;

     String user;
     SecretRef password;

    @JsonProperty("wallet_location")
     SecretRef walletLocation;

     Map<String,Object> jdbc;

    @Value
    @Builder
    @Jacksonized
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SecretRef{
         String type;
         String value;

        @JsonProperty("field_name")
         String fieldName;

         Authentication authentication;

    }

    @Value
    @Builder
    @Jacksonized
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Authentication{

         String method;
         Map<String,Object> parameters;


    }
}

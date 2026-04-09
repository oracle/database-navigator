package com.dbn.connection.config.configprovider;


import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;

public enum SecretProviderType {
    OCIVAULT("ocivault"),
    AZUREVAULT("azurevault"),
    BASE64("base64"),
    AWSSECRETSMANAGER("awssecretsmanager"),
    AWSPARAMETERSTORE("awsparameterstore"),
    HCPVAULTDEDICATED("hcpvaultdedicated"),
    GCPSECRETMANAGER("gcpsecretmanager");

    private final String id;

    SecretProviderType(String id){
        this.id = id;
    }
    public String id(){
        return id;
    }

    public static @Nullable SecretProviderType fromId(@Nullable String id ){
        if(id == null) return null;
        String normalized =id.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(v->v.id.equals(normalized))
                .findFirst()
                .orElse(null);
    }
}

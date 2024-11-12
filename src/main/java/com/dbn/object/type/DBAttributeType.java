package com.dbn.object.type;

import com.dbn.common.constant.Constant;
import lombok.Getter;

@Getter
public enum DBAttributeType implements Constant<DBAttributeType> {
    // Credential attributes
    USER_NAME("username"),
    PASSWORD("password"),
    USER_OCID("user_ocid"),
    USER_TENANCY_OCID("user_tenancy_oci"),
    PRIVATE_KEY("private_key"),
    FINGERPRINT("fingerprint"),

    // Profile attributes
    MODEL("model"),
    PROVIDER("provider"),
    TEMPERATURE("temperature"),
    OBJECT_LIST("object_list"),
    CREDENTIAL_NAME("credential_name"),

    ;

    private final String id;

    DBAttributeType(String id) {
        this.id = id;
    }
}

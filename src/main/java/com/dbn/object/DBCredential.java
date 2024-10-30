package com.dbn.object;

import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.type.DBCredentialType;

import java.util.Map;

public interface DBCredential extends DBSchemaObject {
    DBCredentialType getType();

    String getUserName();

    String getComments();

    Map<String, String> getAttributes();

    void setAttribute(String key, String value);

    String getAttribute(String key);

    interface Attribute {
        String USER_NAME = "username";
        String PASSWORD = "password";
        String USER_OCID = "user_ocid";
        String USER_TENANCY_OCID = "user_tenancy_ocid";
        String PRIVATE_KEY = "private_key";
        String FINGERPRINT = "fingerprint";
    }
}

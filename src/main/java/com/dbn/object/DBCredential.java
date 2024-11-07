package com.dbn.object;

import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.type.DBAttributeType;
import com.dbn.object.type.DBCredentialType;

import java.util.Map;

public interface DBCredential extends DBSchemaObject {
    DBCredentialType getType();

    String getUserName();

    String getComments();

    Map<DBAttributeType, String> getAttributes();

    void setAttribute(DBAttributeType attr, String value);

    String getAttribute(DBAttributeType attr);
}

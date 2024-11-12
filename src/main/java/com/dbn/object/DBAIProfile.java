package com.dbn.object;

import com.dbn.assistant.provider.AIModel;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObject;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface DBAIProfile extends DBSchemaObject {

    String getDescription();

    @Nullable
    DBCredential getCredential();

    AIProvider getProvider();

    AIModel getModel();

    double getTemperature();

    List<DBObject> getObjects();

    String getAttributesJson();

    String getCredentialName();
}

/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

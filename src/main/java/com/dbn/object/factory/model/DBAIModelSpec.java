/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.object.factory.model;

import com.dbn.object.DBCredential;
import com.dbn.object.DBSchema;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.dbn.vector.common.ModelSourceType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DBAIModelSpec extends DBSchemaObjectSpec {
  private ModelSourceType sourceType;
  private String sourceLocation;
  private DBObjectRef<DBCredential> credential;

  public DBAIModelSpec(DBSchema schema) {
       super(schema, DBObjectType.AI_MODEL);
  }

  public String getCredentialName() {
    return DBObjectRef.getQualifiedObjectName(credential);
  }

}

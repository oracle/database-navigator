package com.dbn.object.factory;

import com.dbn.object.DBCredential;
import com.dbn.object.DBSchema;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.dbn.vector.common.ModelSourceType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
public class ModelFactoryInput extends SchemaObjectFactoryInput {
  private ModelSourceType sourceType;
  private String sourceLocation;
  private DBObjectRef<DBCredential> credential;

  public ModelFactoryInput(DBSchema schema) {
       super(schema, DBObjectType.AI_MODEL);
  }

  public String getCredentialName() {
    return DBObjectRef.getObjectName(credential);
  }

  @Override
  public void validate(List<String> errors) {

  }
}

package com.dbn.object.factory;

import com.dbn.object.DBCredential;
import com.dbn.object.DBSchema;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.dbn.vector.common.ModelSourceType;
import lombok.Getter;

import java.util.List;
@Getter
public class ModelFactoryInput extends SchemaObjectFactoryInput {
  private final String modelName;
  private final ModelSourceType sourceType;
  private final String sourceLocation;
  private final DBObjectRef<DBCredential> credential;

  public ModelFactoryInput(DBSchema schema, String modelName, ModelSourceType sourceType, String sourceLocation, DBCredential credential) {
    super(schema, modelName, DBObjectType.AI_MODEL);
    this.modelName = modelName;
    this.sourceType = sourceType;
    this.sourceLocation = sourceLocation;
    this.credential = DBObjectRef.of(credential);
  }

  public String getCredentialName() {
    return DBObjectRef.getObjectName(credential);
  }

  @Override
  public void validate(List<String> errors) {

  }
}

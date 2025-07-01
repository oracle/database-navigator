package com.dbn.object.factory;

import com.dbn.object.DBSchema;
import com.dbn.object.type.DBObjectType;
import com.dbn.vector.common.ModelPathType;
import lombok.Getter;

import java.util.List;
@Getter
public class ModelFactoryInput extends SchemaObjectFactoryInput {
  private final String modelName;
  private final ModelPathType modelPathType;
  private final String location;
  private final String credential;

  public ModelFactoryInput(DBSchema schema, String modelName, ModelPathType modelPathType, String location, String credential) {
    super(schema ,modelName, DBObjectType.AI_MODEL);
    this.modelName = modelName;
    this.modelPathType = modelPathType;
    this.location = location;
    this.credential = credential;
  }

  @Override
  public void validate(List<String> errors) {

  }
}

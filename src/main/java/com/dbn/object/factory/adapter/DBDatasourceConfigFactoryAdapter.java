/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.dbn.object.factory.adapter;

import com.dbn.common.ui.component.DBNComponent;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.object.DBSchema;
import com.dbn.object.event.ObjectChangeEvent;
import com.dbn.object.factory.ObjectFactoryAdapter;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.factory.ui.DBDatasourceConfigFactoryInputForm;
import com.dbn.object.type.DBObjectType;
import java.sql.SQLException;
import java.util.List;

import static com.dbn.common.Priority.HIGH;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.event.ObjectChangeAction.CREATE;
import static com.dbn.object.factory.model.DBObjectAttributeType.OBJECT_DETAIL;
import static com.dbn.object.type.DBObjectType.DATASOURCE_CONFIG;

public class DBDatasourceConfigFactoryAdapter implements ObjectFactoryAdapter {
    private static final String DEFAULT_JSON_TEMPLATE = """
            {
              "connect_descriptor": "",
              "jdbc": {},
              "oci": {},
              "pyo": {},
              "njs": {}
            }
            """;

    @Override
    public DBObjectType getObjectType() {
        return DATASOURCE_CONFIG;
    }

    @Override
    public DBObjectSpec createInput(DBSchema schema) {
        DBObjectSpec input = new DBObjectSpec(schema, DATASOURCE_CONFIG);
        input.setObjectName("new_configuration");
        input.setAttributeValue(OBJECT_DETAIL, DEFAULT_JSON_TEMPLATE);
        return input;
    }

    @Override
    public DBDatasourceConfigFactoryInputForm createInputForm(DBNComponent parent, DBObjectSpec input) {
        return new DBDatasourceConfigFactoryInputForm(parent, input);
    }

    @Override
    public void validateInput(DBObjectSpec input, List<String> errors) {
        String configName = input.getIdentifierCase().format(input.getObjectName());
        String value = OBJECT_DETAIL.of(input);
        if (value == null || value.isBlank()) {
            errors.add(txt("cfg.datasourceConfig.error.JsonRequired"));
        }
    }

    @Override
    public void createObject(DBObjectSpec input) throws SQLException {
        DBSchema schema = input.getSchema();
        ConnectionId connectionId = schema.getConnectionId();
        SchemaId schemaId = schema.getSchemaId();
        String value = OBJECT_DETAIL.of(input);
        String configName = input.getIdentifierCase().format(input.getObjectName());

        DatabaseInterfaceInvoker.execute(
                HIGH,
                txt("prc.object.title.CreatingObject", input.getObjectType().getTitleCasedDisplayName()),
                txt("prc.object.text.CreatingObjectDescription", input.getObjectDescription()),
                input.getProject(),
                connectionId,
                schemaId,
                conn -> schema.getConnection().getDatasourceConfigInterface().createDatasourceConfig(
                        schema.getName(), configName, value, conn));

        ObjectChangeEvent.notify(CREATE, DATASOURCE_CONFIG, connectionId, schemaId);
    }
}

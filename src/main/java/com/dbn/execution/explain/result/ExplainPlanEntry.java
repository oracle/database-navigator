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

package com.dbn.execution.explain.result;

import com.dbn.common.dispose.Disposed;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ExplainPlanEntry extends StatefulDisposableBase {
    private DBObjectRef<?> objectRef;
    private Integer parentId;
    private final String operation;
    private final String operationOptions;
    private final String optimizer;
    private final Integer id;
    private final BigDecimal depth;
    private final BigDecimal position;
    private final BigDecimal cost;
    private final BigDecimal cardinality;
    private final BigDecimal bytes;
    private final BigDecimal cpuCost;
    private final BigDecimal ioCost;
    private final String accessPredicates;
    private final String filterPredicates;
    private final String projection;

    private ExplainPlanEntry parent;
    private List<ExplainPlanEntry> children;

    public ExplainPlanEntry(ConnectionHandler connection, ResultSet resultSet, List<String> columnNames) throws SQLException {
        operation = resultSet.getString("OPERATION");
        operationOptions = resultSet.getString("OPTIONS");
        optimizer = resultSet.getString("OPTIMIZER");
        id = resultSet.getInt("ID");
        parentId = resultSet.getInt("PARENT_ID");
        if (resultSet.wasNull()) {
            parentId = null;
        }

        depth = columnNames.contains("DEPTH") ? resultSet.getBigDecimal("DEPTH") : null;
        position = resultSet.getBigDecimal("POSITION");
        cost = resultSet.getBigDecimal("COST");
        cpuCost = resultSet.getBigDecimal("CPU_COST");
        ioCost = resultSet.getBigDecimal("IO_COST");
        cardinality = resultSet.getBigDecimal("CARDINALITY");
        bytes = resultSet.getBigDecimal("BYTES");

        accessPredicates = resultSet.getString("ACCESS_PREDICATES");
        filterPredicates = resultSet.getString("FILTER_PREDICATES");
        projection = resultSet.getString("PROJECTION");

        String objectOwner = resultSet.getString("OBJECT_OWNER");
        String objectName = resultSet.getString("OBJECT_NAME");
        String objectTypeName = resultSet.getString("OBJECT_TYPE");
        if (Strings.isNotEmpty(objectOwner) && Strings.isNotEmpty(objectName) && Strings.isNotEmpty(objectTypeName)) {
            DBObjectType objectType = DBObjectType.ANY;
            if (objectTypeName.startsWith("TABLE")) {
                objectType = DBObjectType.TABLE;
            } else if (objectTypeName.startsWith("MAT_VIEW")) {
                objectType = DBObjectType.MATERIALIZED_VIEW;
            } else if (objectTypeName.startsWith("VIEW")) {
                objectType = DBObjectType.VIEW;
            } else if (objectTypeName.startsWith("INDEX")) {
                objectType = DBObjectType.INDEX;
            }


            DBObjectRef<?> schemaRef = new DBObjectRef<>(connection.getConnectionId(), DBObjectType.SCHEMA, objectOwner);
            objectRef = new DBObjectRef<>(schemaRef, objectType, objectName);
        }
    }

    public void addChild(ExplainPlanEntry child) {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(child);
    }

    @Override
    public void disposeInner() {
        children = Disposer.replace(children, Disposed.list());
        nullify();
    }
}

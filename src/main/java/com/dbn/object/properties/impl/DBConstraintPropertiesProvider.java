/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.object.properties.impl;

import com.dbn.object.DBConstraint;
import com.dbn.object.properties.DBObjectPresentableProperty;
import com.dbn.object.properties.DBObjectProperty;
import com.dbn.object.properties.SimplePresentableProperty;
import com.dbn.object.type.DBObjectType;

import java.util.List;

public class DBConstraintPropertiesProvider extends DBGenericObjectPropertiesProvider<DBConstraint> {
    public DBConstraintPropertiesProvider() {
        super(DBObjectType.CONSTRAINT);
    }

    @Override
    public List<DBObjectProperty> getProperties(DBConstraint constraint) {
        List<DBObjectProperty> properties = super.getProperties(constraint);

        switch (constraint.getConstraintType()) {
            case CHECK:
                properties.add(0, new SimplePresentableProperty("Check condition", constraint.getCheckCondition()));
                properties.add(0, new SimplePresentableProperty("Constraint type", "Check"));
                break;
            case PRIMARY_KEY: properties.add(0, new SimplePresentableProperty("Constraint type", "Primary Key")); break;
            case FOREIGN_KEY:
                DBConstraint foreignKeyConstraint = constraint.getForeignKeyConstraint();
                if (foreignKeyConstraint != null) {
                    properties.add(0, new DBObjectPresentableProperty(foreignKeyConstraint));
                    properties.add(0, new SimplePresentableProperty("Constraint type", "Foreign Key"));
                }
                break;
            case UNIQUE_KEY: properties.add(0, new SimplePresentableProperty("Constraint type", "Unique")); break;
        }
        return properties;
    }
}

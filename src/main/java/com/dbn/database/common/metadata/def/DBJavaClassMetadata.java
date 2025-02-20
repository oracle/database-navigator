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

package com.dbn.database.common.metadata.def;

import com.dbn.database.common.metadata.DBObjectMetadata;
import com.dbn.database.common.security.ObjectIdentifier;

import java.sql.SQLException;

/**
 * Interface for showing Java object node in Schema tree
 * @author rishabh (Oracle)
 */
public interface DBJavaClassMetadata extends DBObjectMetadata {
    @ObjectIdentifier
    String getObjectName() throws SQLException;

    @ObjectIdentifier
    String getOuterClassName() throws SQLException;

	String getObjectKind()throws SQLException;

    String getAccessibility()throws SQLException;

    boolean isPrimitive() throws SQLException;

    boolean isFinal() throws SQLException;

    boolean isAbstract()throws SQLException;

    boolean isStatic()throws SQLException;

    boolean isInner()throws SQLException;

    boolean isValid() throws SQLException;

    boolean isDebug() throws SQLException;
}

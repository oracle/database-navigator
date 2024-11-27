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

import java.sql.SQLException;

public interface DBDataTypeMetadata {

    String getDataTypeName() throws SQLException;

    String getDeclaredTypeName() throws SQLException;

    String getDeclaredTypeOwner() throws SQLException;

    String getDeclaredTypeProgram() throws SQLException;

    long getDataLength() throws SQLException;

    int getDataPrecision() throws SQLException;

    int getDataScale() throws SQLException;

    boolean isSet() throws SQLException;

    DBDataTypeMetadata collection();
}

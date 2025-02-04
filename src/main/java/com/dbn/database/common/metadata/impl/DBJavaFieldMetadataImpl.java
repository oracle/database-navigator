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

package com.dbn.database.common.metadata.impl;

import com.dbn.database.common.metadata.DBObjectMetadataBase;
import com.dbn.database.common.metadata.def.DBJavaFieldMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DBJavaFieldMetadataImpl extends DBObjectMetadataBase implements DBJavaFieldMetadata {

	public DBJavaFieldMetadataImpl(ResultSet resultSet) {
		super(resultSet);
	}

	@Override
	public String getOwnerClassName() throws SQLException {
		return getString("OWNER_CLASS_NAME");
	}

	@Override
	public short getFieldIndex() throws SQLException {
		return resultSet.getShort("FIELD_INDEX");
	}

	@Override
	public String getFieldName() throws SQLException {
		return getString("FIELD_NAME");
	}

	@Override
	public String getAccessibility() throws SQLException {
		return getString("ACCESSIBILITY");
	}

	@Override
	public boolean isFinal() throws SQLException {
		return isYesFlag("IS_FINAL");
	}

	@Override
	public boolean isStatic() throws SQLException {
		return isYesFlag("IS_STATIC");
	}

	@Override
	public short getArrayDepth() throws SQLException {
		return resultSet.getShort("ARRAY_DEPTH");
	}

	@Override
	public String getFieldClassName() throws SQLException {
		return getString("FIELD_CLASS_NAME");
	}
}

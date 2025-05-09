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

package com.dbn.database.common.statement;

import com.dbn.common.exception.Exceptions;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Getter
public class ClobText implements CallableStatementOutput {
	private final List<String> value = new ArrayList<>();

	@Override
	public void registerParameters(CallableStatement statement) throws SQLException {
		statement.registerOutParameter(1, Types.CLOB);
	}

	@Override
	public void read(CallableStatement statement) throws SQLException {
		Clob clob = statement.getClob(1);
		Reader reader = clob.getCharacterStream();
		BufferedReader br = new BufferedReader(reader);

		try {
			String line;
			while ((line = br.readLine()) != null) {
				value.add(line);
			}
			br.close();
		} catch (IOException e) {
			throw Exceptions.toSqlException(e);
		}
	}
}

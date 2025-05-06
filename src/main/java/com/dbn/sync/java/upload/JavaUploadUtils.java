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

package com.dbn.sync.java.upload;

import com.dbn.common.Priority;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.Resources;
import com.dbn.connection.jdbc.DBNCallableStatement;
import com.dbn.connection.jdbc.DBNPreparedStatement;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.intellij.openapi.project.Project;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

public class JavaUploadUtils {

	public static final String LOB_TABLE = "CREATE$JAVA$LOB$TABLE";

	public static byte[] readAllBytes(InputStream in) throws IOException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		byte[] tmp = new byte[4 * 1024];
		int read;
		while ((read = in.read(tmp)) != -1) {
			buf.write(tmp, 0, read);
		}
		return buf.toByteArray();
	}

	public static void insertLob(Project project, ConnectionId connectionId, String key, byte[] data) throws SQLException {
		String sql = "INSERT INTO \"" + LOB_TABLE + "\" (name, lob, loadtime) VALUES (?, ?, SYSDATE)";

		DatabaseInterfaceInvoker.execute(
				Priority.HIGH,
				"Insert LOB",
				"Uploading LOB row for key " + key,
				project,
				connectionId,
				c -> {
					DBNPreparedStatement preparedStatement = null;
					try {
						preparedStatement = c.prepareStatement(sql);
						preparedStatement.setString(1, key);
						preparedStatement.setBytes(2, data);
						preparedStatement.execute();
					} finally {
						Resources.close(preparedStatement);
					}
				});
	}

	public static void executeQuery(Project project, ConnectionId connectionId, String query) throws SQLException {
		DatabaseInterfaceInvoker.execute(
				Priority.HIGH,
				"Executing DDL",
				query,
				project,
				connectionId,
				c -> {
					DBNCallableStatement stmt = null;
					try {
						stmt = c.prepareCall(query);
						stmt.execute();
					} finally {
						Resources.close(stmt);
					}
				});
	}
}

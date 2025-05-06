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

package com.dbn.sync.java.upload.impl;

import com.dbn.common.Priority;
import com.dbn.connection.Resources;
import com.dbn.connection.jdbc.DBNCallableStatement;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.jdbc.DBNPreparedStatement;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.sync.java.upload.JavaUploadContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

public class JavaUploaderBase {

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


	public static void executeStatement(DBNConnection c, String sql) throws SQLException {
		DBNCallableStatement statement = null;
		try {
			statement = c.prepareCall(sql);
			statement.execute();
		} finally {
			Resources.close(statement);
		}
	}

	protected static void ensureLobTable(JavaUploadContext context) throws SQLException {
		DatabaseInterfaceInvoker.execute(
				Priority.HIGH,
				"Creating Table",
				"Creating table \"" + LOB_TABLE + "\"",
				context.getProject(),
				context.getConnectionId(),
				c -> createLobTable(c));
	}

	private static void createLobTable(DBNConnection connection) throws SQLException {
		DBNCallableStatement statement = null;
		try {
			// Try a simple count to see if it exists
			executeStatement(connection, "SELECT 1 FROM \"" + LOB_TABLE + "\" WHERE 1 = 2");
		} catch (SQLException e) {
			// Table missing: create it
			executeStatement(connection, "CREATE TABLE \"" + LOB_TABLE + "\" ("
					+ " name VARCHAR2(700) PRIMARY KEY, "
					+ " lob  BLOB, "
					+ " loadtime DATE )");
		} finally {
			Resources.close(statement);
		}
	}

	public static void insertLobData(DBNConnection connection, String key, byte[] data) throws SQLException {
		DBNPreparedStatement statement = null;
		try {
			String sql = "INSERT INTO \"" + LOB_TABLE + "\" (name, lob, loadtime) VALUES (?, ?, SYSDATE)";
			statement = connection.prepareStatement(sql);
			statement.setString(1, key);
			statement.setBytes(2, data);
			statement.execute();
		} finally {
			Resources.close(statement);
		}
	}

	public static void deleteLobData(DBNConnection connection, String key) throws SQLException {
		DBNPreparedStatement statement = null;
		try {
			String sql = "DELETE FROM \"" + LOB_TABLE + "\" WHERE name = ?";
			statement = connection.prepareStatement(sql);
			statement.setString(1, key);
			statement.execute();
		} finally {
			Resources.close(statement);
		}
	}


}

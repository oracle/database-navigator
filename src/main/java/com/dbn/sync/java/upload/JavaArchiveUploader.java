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
import com.dbn.connection.jdbc.DBNResultSet;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.intellij.openapi.project.Project;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.dbn.common.load.ProgressMonitor.isProgressCancelled;
import static com.dbn.sync.java.upload.JavaUploadUtils.LOB_TABLE;
import static com.dbn.sync.java.upload.JavaUploadUtils.executeQuery;
import static com.dbn.sync.java.upload.JavaUploadUtils.insertLob;
import static com.dbn.sync.java.upload.JavaUploadUtils.readAllBytes;

public class JavaArchiveUploader {

	@SneakyThrows
	public static void loadJar(JavaUploadContext context, String jarPath) {
		List<String> classesToCompile = new ArrayList<>();

		ensureLobTable(context);

		try (InputStream fis = Files.newInputStream(Paths.get(jarPath))) {
			processArchive(fis, context, classesToCompile);
		}

		compileClasses(context, classesToCompile);
	}

	private static void processArchive(InputStream in, JavaUploadContext context, List<String> classesToCompile) throws IOException, SQLException {
		try (ZipInputStream zis = new ZipInputStream(in)) {
			ZipEntry entry;

			while ((entry = zis.getNextEntry()) != null) {
				if (isProgressCancelled()) return;
				String name = entry.getName();

				if (entry.isDirectory()) {
					zis.closeEntry();
					continue;
				}

				if (name.endsWith(".jar") || name.endsWith(".zip")) {
					// Nested archive: recurse
					byte[] nested = readAllBytes(zis);
					try (InputStream bin = new ByteArrayInputStream(nested)) {
						processArchive(bin, context, classesToCompile);
					}

				} else if (name.endsWith(".class")) {
					// Class file: load and record for compile
					byte[] classBytes = readAllBytes(zis);
					String className = name
							.substring(0, name.length() - 6);  // strip ".class"
					loadClass(context, className, classBytes, classesToCompile);

				} else {
					// Other resource: load as Java resource
					byte[] resBytes = readAllBytes(zis);
					ConnectionId connectionId = context.getInput().getTargetConnectionId();
					Project project = context.getProject();
					JavaResourceUploader.loadResource(project, connectionId, name, resBytes);
				}

				zis.closeEntry();
			}
		}
	}

	private static void loadClass(JavaUploadContext context, String className, byte[] classBytes, List<String> classesToCompile) throws SQLException {
		ConnectionId connectionId = context.getInput().getTargetConnectionId();
		Project project = context.getProject();

		String key = String.valueOf(System.nanoTime());
		insertLob(project, connectionId, key, classBytes);

		String ddlCreate = "CREATE OR REPLACE JAVA CLASS USING '" + key + "'";
		executeQuery(project, connectionId, ddlCreate);

		classesToCompile.add(className);

		String ddlDelete = "DELETE FROM \"" + LOB_TABLE + "\" WHERE name = '" + key + "'";
		executeQuery(project, connectionId, ddlDelete);
	}

	private static void compileClasses(JavaUploadContext context, List<String> classesToCompile) throws SQLException {
		ConnectionId connectionId = context.getInput().getTargetConnectionId();
		Project project = context.getProject();

		for (String className : classesToCompile) {
			String ddlCompile = "ALTER JAVA CLASS \"" + className + "\" COMPILE";
			executeQuery(project, connectionId, ddlCompile);
		}

		String classList = classesToCompile.stream()
				.map(s -> "'" + s + "'")
				.collect(Collectors.joining(","));

		String errorQuery = "SELECT NVL(j.longname, e.name) AS error_name,\n" +
				"       e.text\n" +
				"  FROM user_errors e\n" +
				"  LEFT JOIN javasnm j\n" +
				"    ON j.short = e.name\n" +
				" WHERE NVL(j.longname, e.name) in ("+ classList + ")";

		DatabaseInterfaceInvoker.execute(
				Priority.HIGH,
				"Executing DDL",
				"Checking for error",
				project,
				connectionId,
				c -> {
					DBNCallableStatement stmt = null;
					try {
						stmt = c.prepareCall(errorQuery);
						boolean result = stmt.execute();
						if(result) {
							DBNResultSet resultSet = stmt.getResultSet();
							while(resultSet.next()) {
								String title = resultSet.getString(1);
								String message = resultSet.getString(2);
								context.addErrorMessage(title, message);
							}
						}
					} finally {
						Resources.close(stmt);
					}
				}
		);
	}

	private static void ensureLobTable(JavaUploadContext context) throws SQLException {
		ConnectionId connectionId = context.getInput().getTargetConnectionId();
		Project project = context.getProject();

		DatabaseInterfaceInvoker.execute(
				Priority.HIGH,
				"Creating Table",
				"Creating table \"" + LOB_TABLE + "\"",
				project,
				connectionId,
				c -> {
					DBNCallableStatement stmt = null;
					try {
						// Try a simple count to see if it exists
						stmt = c.prepareCall("SELECT COUNT(1) FROM " + LOB_TABLE);
						stmt.execute();
					} catch (SQLException e) {
						// Table missing: create it
						String ddl = "CREATE TABLE \"" + LOB_TABLE + "\" ("
								+ " name VARCHAR2(700) PRIMARY KEY, "
								+ " lob  BLOB, "
								+ " loadtime DATE )";
						executeQuery(project, connectionId, ddl);
					} finally {
						Resources.close(stmt);
					}
				});
	}
}
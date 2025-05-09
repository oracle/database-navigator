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
import com.dbn.connection.jdbc.DBNResultSet;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.sync.java.upload.JavaUploadBatch;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.dbn.common.load.ProgressMonitor.isProgressCancelled;
import static com.dbn.common.load.ProgressMonitor.setProgressDetail;
import static com.dbn.sync.java.upload.impl.JavaClassUploader.uploadJavaClass;
import static com.dbn.sync.java.upload.impl.JavaResourceUploader.uploadJavaResource;

public class JavaArchiveUploader extends JavaUploaderBase {

	@SneakyThrows
	public static void uploadJavaArchive(JavaUploadBatch batch, String jarPath) {
		ensureLobTable(batch);

		try (InputStream fis = Files.newInputStream(Paths.get(jarPath))) {
			processArchive(fis, batch);
		}

		compileClasses(batch);
		loadCompilationErrors(batch);
	}

	private static void processArchive(InputStream in, JavaUploadBatch batch) throws IOException, SQLException {
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
					byte[] content = readAllBytes(zis);
					try (InputStream bin = new ByteArrayInputStream(content)) {
						processArchive(bin, batch);
					}

				} else if (name.endsWith(".class")) {
					// Class file: load and record for compile
					byte[] content = readAllBytes(zis);
					String className = name.substring(0, name.length() - 6);  // strip ".class"
					uploadJavaClass(batch, className, content);

				} else {
					// Other resource: load as Java resource
					byte[] content = readAllBytes(zis);
					uploadJavaResource(batch, name, content);
				}

				zis.closeEntry();
			}
		}
	}

	private static void compileClasses(JavaUploadBatch batch) throws SQLException {
		DatabaseInterfaceInvoker.execute(
				Priority.HIGH,
				"Compiling Java Classes",
				"Compiling uploaded java classes",
				batch.getProject(),
				batch.getConnectionId(),
				c -> {
					for (String className : batch.getClassesToCompile()) {
                        executeStatement(c, "ALTER JAVA CLASS \"" + className + "\" COMPILE");
						setProgressDetail(className);
                    }
				});


		loadCompilationErrors(batch);
	}

	private static void loadCompilationErrors(JavaUploadBatch batch) throws SQLException {
		String classList = batch.getClassesToCompile().stream()
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
				"Load Compiler Result",
				"Loading java class compilation errors",
				batch.getProject(),
				batch.getConnectionId(),
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
								batch.getMessages().addErrorMessage(title, message);
							}
						}
					} finally {
						Resources.close(stmt);
					}
				}
		);
	}
}
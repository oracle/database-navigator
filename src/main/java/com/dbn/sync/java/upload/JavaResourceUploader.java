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

import com.dbn.connection.ConnectionId;
import com.intellij.openapi.project.Project;

import java.sql.SQLException;

import static com.dbn.sync.java.upload.JavaUploadUtils.LOB_TABLE;
import static com.dbn.sync.java.upload.JavaUploadUtils.executeQuery;
import static com.dbn.sync.java.upload.JavaUploadUtils.insertLob;

public class JavaResourceUploader {

	public static void loadResource(Project project, ConnectionId connectionId, String resourceName, byte[] resourceBytes) throws SQLException {
		String key = String.valueOf(System.nanoTime());
		insertLob(project, connectionId, key, resourceBytes);

		String ddl = "CREATE OR REPLACE JAVA RESOURCE NAMED \"" + resourceName +
				"\" USING blob LOB FROM " + LOB_TABLE +
				" WHERE name = '" + key + "'";
		executeQuery(project, connectionId, ddl);

		String del = "DELETE FROM \"" + LOB_TABLE + "\" WHERE name = '" + key + "'";
		executeQuery(project, connectionId, del);
	}
}

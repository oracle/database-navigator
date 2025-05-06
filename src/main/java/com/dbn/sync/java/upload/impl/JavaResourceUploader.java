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
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.sync.java.upload.JavaUploadContext;

import java.sql.SQLException;

public class JavaResourceUploader extends JavaUploaderBase {

	public static void uploadJavaResource(JavaUploadContext context, String resourceName, byte[] resourceBytes) throws SQLException {
		String key = String.valueOf(System.nanoTime());
		DatabaseInterfaceInvoker.execute(
				Priority.HIGH,
				"Uploading Java Resource",
				"Uploading java resource \"" + resourceName + "\"",
				context.getProject(),
				context.getConnectionId(),
				c -> {
					try {
						insertLobData(c, key, resourceBytes);
						executeStatement(c, "CREATE OR REPLACE JAVA RESOURCE NAMED \"" + resourceName +
								"\" USING blob LOB FROM " + LOB_TABLE +
								" WHERE name = '" + key + "'");
					} finally {
						deleteLobData(c, key);
					}
				});
	}
}

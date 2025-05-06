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
import com.dbn.connection.ConnectionId;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.sync.java.upload.JavaUploadContext;
import com.dbn.sync.java.upload.JavaUploadInput;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

public class JavaClassUploader extends JavaUploaderBase {
	public static void uploadJavaClass(JavaUploadContext context, String className, byte[] classContent) throws SQLException {
		JavaUploadInput input = context.getInput();

		String objectName = className.replace('.', '/');
		String schemaName = input.getTargetSchemaName();
		ConnectionId connectionId = input.getTargetConnectionId();

		// TODO move to oracle-ddl-interface
		String creationStatement = "BEGIN\n" +
				"   BEGIN\n" +
				"      EXECUTE IMMEDIATE 'DROP JAVA SOURCE \"" + schemaName + "\".\"" + objectName + "\"';\n" +
				"   EXCEPTION\n" +
				"      WHEN OTHERS THEN\n" +
				"         IF SQLCODE <> -4043 THEN\n" +
				"            RAISE;\n" +
				"         END IF;\n" +
				"   END;\n" +
				"\n" +
				"   EXECUTE IMMEDIATE q'[\n" +
				"CREATE OR REPLACE AND COMPILE JAVA SOURCE NAMED \"" + schemaName + "\".\"" + objectName + "\" AS \n" +
				new String(classContent, StandardCharsets.UTF_8) +
				"]';\n" +
				"END;";

		DatabaseInterfaceInvoker.execute(Priority.HIGH,
				"Uploading Java Class",
				"Uploading java class \"" + className + "\"",
				context.getProject(),
				connectionId, c -> executeStatement(c, creationStatement));
	}
}

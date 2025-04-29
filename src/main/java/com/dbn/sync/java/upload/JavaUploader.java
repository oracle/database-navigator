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
import com.dbn.connection.jdbc.DBNPreparedStatement;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import lombok.SneakyThrows;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;

import static com.dbn.common.load.ProgressMonitor.setProgressDetail;

public final class JavaUploader extends JavaUploaderBase {
	public static final JavaUploader INSTANCE = new JavaUploader();

	private JavaUploader() {}

	public void uploadJavaClasses(JavaUploadContext context) {
		if (context.hasErrors()) return;

		JavaUploadInput input = context.getInput();
		List<JavaUploadElement> uploadElements = input.getSelectedElements();
		for (JavaUploadElement uploadElement : uploadElements) {
			context.handled(() -> uploadJavaClass(context, uploadElement));
		}
	}

	@SneakyThrows
	private void uploadJavaClass(JavaUploadContext context, JavaUploadElement element) {
		String elementName = element.getName();
		setProgressDetail("Uploading sources of \"" + elementName + "\"");

		// create upload task
		JavaUploadTask uploadTask = context.createUploadTask(element);

		if (element.isArchive()) {
			JavaArchiveUploader.loadJar(context, element.getFile().getPath());
		} else {
			byte[] content = VfsUtilCore.loadBytes(element.getFile());
			uploadTask.setContent(new String(content, StandardCharsets.UTF_8));

			if (element.isJavaClass()) {
				uploadJavaClass(context, uploadTask, element.getJavaClassName());
			} else {
				// TODO upload resources
				context.addError(List.of(elementName, "Uploading resources is not supported yet"));
			}
		}
	}

	private static void uploadJavaClass(JavaUploadContext context, JavaUploadTask task, String className) throws SQLException {
		Project project = context.getProject();

		String databaseObjectName = className.replace('.', '/');
		String creationStatement = "BEGIN\n" +
				"   BEGIN\n" +
				"      EXECUTE IMMEDIATE 'DROP JAVA SOURCE \"" + databaseObjectName + "\"';\n" +
				"   EXCEPTION\n" +
				"      WHEN OTHERS THEN\n" +
				"         IF SQLCODE <> -4043 THEN\n" +
				"            RAISE;\n" +
				"         END IF;\n" +
				"   END;\n" +
				"\n" +
				"   EXECUTE IMMEDIATE q'[\n" +
				"CREATE OR REPLACE AND COMPILE JAVA SOURCE NAMED \"" + databaseObjectName + "\" AS \n" +
				task.getContent() +
				"]';\n" +
				"END;";

		ConnectionId connectionId = context.getInput().getConnection().getConnectionId();
		DatabaseInterfaceInvoker.execute(Priority.HIGH,
				"Uploading Java Class",
				"Uploading java class \"" + className + "\"",
				project,
				connectionId, c -> {
					DBNPreparedStatement statement = c.prepareStatement(creationStatement);
					statement.execute();
				});
	}
}

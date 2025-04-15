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
import com.intellij.openapi.vfs.VirtualFile;
import lombok.SneakyThrows;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.dbn.common.load.ProgressMonitor.setProgressDetail;

public final class JavaUploader extends JavaUploaderBase {
	public static final JavaUploader INSTANCE = new JavaUploader();

	private JavaUploader() {}

	public void uploadJavaClasses(JavaUploadContext context) {
		if (context.hasErrors()) return;

		JavaUploadInput input = context.getInput();
		List<JavaUploadElement> uploadElements = input.getSelectedUploadElements();
		for (JavaUploadElement uploadElement : uploadElements) {
			context.handled(() -> uploadJavaClass(context, uploadElement));
		}
	}

	@SneakyThrows
	private void uploadJavaClass(JavaUploadContext context, JavaUploadElement uploadElement) {
		String className = uploadElement.getJavaClassName();
		setProgressDetail("Uploading sources of \"" + className + "\"");

		Project project = context.getProject();
		VirtualFile javaClass = uploadElement.getJavaClass();
		String jarPath = uploadElement.getJarPath();

		if(jarPath == null) {
			byte[] content = VfsUtilCore.loadBytes(javaClass);
			String fileContent =  new String(content, StandardCharsets.UTF_8);
			String creationStatement = "BEGIN\n" +
					"   BEGIN\n" +
					"      EXECUTE IMMEDIATE 'DROP JAVA SOURCE \"" + uploadElement.getQualifiedName() + "\"';\n" +
					"   EXCEPTION\n" +
					"      WHEN OTHERS THEN\n" +
					"         IF SQLCODE <> -4043 THEN\n" +
					"            RAISE;\n" +
					"         END IF;\n" +
					"   END;\n" +
					"\n" +
					"   EXECUTE IMMEDIATE q'[\n" +
					"CREATE OR REPLACE AND COMPILE JAVA SOURCE NAMED \"" + uploadElement.getQualifiedName() + "\" AS \n" +
					fileContent +
					"]';\n" +
					"END;";

			ConnectionId connectionId = context.getInput().getConnection().getConnectionId();
			DatabaseInterfaceInvoker.execute(Priority.HIGH,
					"Uploading Java File",
					"Uploading java file \"" + javaClass.getPresentableName() + "\"",
					project,
					connectionId, c -> {
						DBNPreparedStatement statement = c.prepareStatement(creationStatement);
						statement.execute();
					});
		} else {
			// TODO upload jar file
		}
	}
}

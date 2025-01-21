/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.common.template;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.openapi.project.Project;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Properties;

/**
 * Utility class for generating code content using file templates within a project.
 * This class provides helper methods to simplify interactions with file templates.
 *
 * @author Rishabh Agrawal (Oracle)
 */
@UtilityClass
public class TemplateUtilities {

	/**
	 * Generates code content using a specified file template and a set of properties.
	 * The method retrieves the specified template from the file template manager,
	 * applies the provided properties, and returns the generated code as a string.
	 *
	 * @param project the project context where the file template is retrieved
	 * @param templateName the name of the template to be used for generating the code
	 * @param properties the properties to be applied to the template for code generation
	 * @return the generated code content as a string
	 */
	@SneakyThrows
	public static String generateCode(@NotNull Project project, @NonNls String templateName, Properties properties){
		FileTemplateManager templateManager = FileTemplateManager.getInstance(project);
		FileTemplate template = templateManager.getCodeTemplate(templateName);
		return template.getText(properties);
	}
}

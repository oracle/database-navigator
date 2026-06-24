/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.assistant.mcp;

import com.dbn.assistant.tool.approval.AssistantToolApprovalException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.List;
import java.util.Map;

public class AssistantMcpServerToolVerifierTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void validateIdeMcpArgumentsAcceptsCurrentProjectPath() {
        String projectPath = projectPath();

        validate(Map.of("projectPath", projectPath), projectPath);
    }

    @Test
    public void validateIdeMcpArgumentsRejectsDifferentProjectPath() {
        String projectPath = projectPath();

        AssistantToolApprovalException exception = Assert.assertThrows(
                AssistantToolApprovalException.class,
                () -> validate(Map.of("projectPath", outsidePath()), projectPath));

        Assert.assertTrue(exception.getMessage().contains("projectPath does not match"));
    }

    @Test
    public void validateIdeMcpArgumentsRejectsEscapingFilePath() {
        String projectPath = projectPath();

        AssistantToolApprovalException exception = Assert.assertThrows(
                AssistantToolApprovalException.class,
                () -> validate(Map.of("filePath", "../outside.txt"), projectPath));

        Assert.assertTrue(exception.getMessage().contains("filePath"));
    }

    @Test
    public void validateIdeMcpArgumentsRejectsAbsolutePathOutsideProject() {
        String projectPath = projectPath();

        AssistantToolApprovalException exception = Assert.assertThrows(
                AssistantToolApprovalException.class,
                () -> validate(Map.of("path", outsidePath()), projectPath));

        Assert.assertTrue(exception.getMessage().contains("path"));
    }

    @Test
    public void validateIdeMcpArgumentsRejectsFileUriOutsideProject() {
        String projectPath = projectPath();

        AssistantToolApprovalException exception = Assert.assertThrows(
                AssistantToolApprovalException.class,
                () -> validate(Map.of("uri", outsideFile().toURI().toString()), projectPath));

        Assert.assertTrue(exception.getMessage().contains("uri"));
    }

    @Test
    public void validateIdeMcpArgumentsRejectsNestedFileArgument() {
        String projectPath = projectPath();

        AssistantToolApprovalException exception = Assert.assertThrows(
                AssistantToolApprovalException.class,
                () -> validate(Map.of("target", Map.of("file", "../outside.txt")), projectPath));

        Assert.assertTrue(exception.getMessage().contains("file"));
    }

    @Test
    public void validateIdeMcpArgumentsRejectsPathArgumentInList() {
        String projectPath = projectPath();

        AssistantToolApprovalException exception = Assert.assertThrows(
                AssistantToolApprovalException.class,
                () -> validate(Map.of("files", List.of("src/Main.java", "../outside.txt")), projectPath));

        Assert.assertTrue(exception.getMessage().contains("files"));
    }

    @Test
    public void validateIdeMcpArgumentsRejectsCurrentIdeMcpPathArguments() {
        String projectPath = projectPath();

        Assert.assertThrows(
                AssistantToolApprovalException.class,
                () -> validate(Map.of("pathInProject", "../outside.txt"), projectPath));
        Assert.assertThrows(
                AssistantToolApprovalException.class,
                () -> validate(Map.of("repositoryPathRelativeToProject", "../outside"), projectPath));
        Assert.assertThrows(
                AssistantToolApprovalException.class,
                () -> validate(Map.of("filesToRebuild", List.of("../outside.txt")), projectPath));
    }

    @Test
    public void validateIdeMcpArgumentsAcceptsProjectRelativePaths() {
        String projectPath = projectPath();

        validate(Map.of(
                "path", "src/Main.java",
                "target", Map.of("file", "README.md"),
                "files", List.of("src/Main.java", "docs/index.md")), projectPath);
    }

    @Test
    public void validateIdeMcpArgumentsAcceptsProjectAbsolutePaths() {
        String projectPath = projectPath();

        validate(Map.of("directory", new File(projectPath, "src").getAbsolutePath()), projectPath);
    }

    @Test
    public void validateIdeMcpArgumentsIgnoresNonFilesystemArguments() {
        String projectPath = projectPath();

        validate(Map.of(
                "query", "select * from users",
                "target", "editor",
                "url", "https://example.com/path",
                "limit", 10), projectPath);
    }

    private static void validate(Map<String, Object> arguments, String projectPath) {
        AssistantMcpServerToolVerifier.validateIdeMcpArguments(arguments, projectPath);
    }

    private String projectPath() {
        try {
            return temporaryFolder.newFolder("project").getCanonicalPath();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String outsidePath() {
        try {
            return outsideFile().getCanonicalPath();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private File outsideFile() {
        return new File(temporaryFolder.getRoot(), "outside.txt");
    }
}

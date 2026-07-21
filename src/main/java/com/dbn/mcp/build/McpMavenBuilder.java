package com.dbn.mcp.build;

import com.dbn.common.template.TemplateUtilities;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.dbn.nls.NlsResources.txt;

public final class McpMavenBuilder {

    private McpMavenBuilder() {}

    public static Path build(
            Project project,
            Path workDir,
            Path artifactDir,
            McpServerGenerator generator,
            Path sourceProjectDir,
            @Nullable String graalVmSdkName,
            ProgressIndicator indicator,
            Consumer<String> outputHandler)
            throws IOException {

        Path projDir = uniqueDir(workDir, generator.getServerName());
        try {
            writeSourceFiles(projDir, generator.getSourceFiles());
            writePom(project, projDir, generator);
            runMaven(project, projDir, generator.getMavenGoals(), graalVmSdkName, indicator, outputHandler);
            exportSourceProject(projDir, sourceProjectDir);
            return copyArtifact(generator, projDir, artifactDir);
        } finally {
            deleteDir(projDir);
        }
    }

    private static void exportSourceProject(Path projDir, Path sourceProjectDir) throws IOException {
        if (sourceProjectDir == null) return;

        if (Files.exists(sourceProjectDir)) {
            deleteDir(sourceProjectDir);
        }
        Files.createDirectories(sourceProjectDir);

        copyFileIfExists(projDir.resolve("pom.xml"), sourceProjectDir.resolve("pom.xml"));
        // rendered as "Dockerfile.ci" so micronaut-maven-plugin does not pick it up
        // as a docker-native packaging override during the build
        copyFileIfExists(projDir.resolve("Dockerfile.ci"), sourceProjectDir.resolve("Dockerfile"));
        copyDirIfExists(projDir.resolve("src"), sourceProjectDir.resolve("src"));
    }

    private static void copyFileIfExists(Path source, Path target) throws IOException {
        if (!Files.exists(source) || !Files.isRegularFile(source)) return;
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void copyDirIfExists(Path source, Path target) throws IOException {
        if (!Files.exists(source) || !Files.isDirectory(source)) return;
        copyDirectory(source, target);
    }

    private static void copyDirectory(Path sourceDir, Path targetDir) throws IOException {
        try (Stream<Path> stream = Files.walk(sourceDir)) {
            for (Path source : stream.toList()) {
                Path relative = sourceDir.relativize(source);
                Path target = targetDir.resolve(relative);
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Path parent = target.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void deleteDir(Path dir) {
        try (var stream = Files.walk(dir)) {
            stream.sorted(java.util.Comparator.reverseOrder())
                  .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
        } catch (IOException ignored) {}
    }

    private static Path uniqueDir(Path base, String name) throws IOException {
        Path dir = base.resolve("mcp-mvn-" + name);
        int i = 1;
        while (Files.exists(dir)) dir = base.resolve("mcp-mvn-" + name + "-" + i++);
        return dir;
    }

    private static void writeSourceFiles(Path projDir, Map<String, String> sourceFiles) throws IOException {
        for (Map.Entry<String, String> entry : sourceFiles.entrySet()) {
            Path file = projDir.resolve(entry.getKey());
            Files.createDirectories(file.getParent());
            Files.writeString(file, entry.getValue());
        }
    }

    private static void writePom(Project project, Path dir, McpServerGenerator generator) throws IOException {
        String pom = TemplateUtilities.generateCode(project, generator.getPomTemplateName(), generator.getPomProperties());
        Files.writeString(dir.resolve("pom.xml"), pom);
    }

    private static void runMaven(Project project, Path dir, List<String> goals, @Nullable String graalVmSdkName, ProgressIndicator indicator, Consumer<String> outputHandler) throws IOException {
        McpMavenBuildManager mavenManager = McpMavenBuildManager.getInstance(project);
        if (mavenManager == null) {
            throw new IOException(txt("msg.mcp.exception.MavenServiceUnavailable"));
        }
        mavenManager.runBuild(dir, goals, graalVmSdkName, indicator, outputHandler);
    }

    private static Path copyArtifact(McpServerGenerator generator, Path proj, Path out) throws IOException {
        Path artifact = generator.locateArtifact(proj.resolve("target"));
        if (artifact == null) return null; // no file artifact (e.g. container image build)

        Files.createDirectories(out);
        Path dest = out.resolve(artifact.getFileName());
        // COPY_ATTRIBUTES preserves the executable bit of native binaries
        Files.copy(artifact, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        return dest;
    }
}

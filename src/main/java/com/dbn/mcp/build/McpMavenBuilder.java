package com.dbn.mcp.build;

import com.dbn.common.template.TemplateUtilities;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.dbn.common.util.LinkRejectingFiles.copyFileNoFollow;
import static com.dbn.common.util.LinkRejectingFiles.createDirectoriesNoFollow;
import static com.dbn.common.util.LinkRejectingFiles.deleteTreeNoFollow;
import static com.dbn.common.util.LinkRejectingFiles.validatePathNoFollow;
import static com.dbn.common.util.LinkRejectingFiles.validateTreeNoFollow;
import static com.dbn.common.util.LinkRejectingFiles.writeStringNoFollow;
import static com.dbn.nls.NlsResources.txt;

public final class McpMavenBuilder {

    private McpMavenBuilder() {}

    public static Path build(
            Project project,
            Path workDir,
            Path artifactDir,
            McpServerGenerator generator,
            Path sourceProjectDir,
            ProgressIndicator indicator,
            Consumer<String> outputHandler)
            throws IOException {

        workDir = workDir.toAbsolutePath().normalize();
        validateTreeNoFollow(artifactDir);
        createDirectoriesNoFollow(workDir);
        Path projDir = Files.createTempDirectory(workDir, "mcp-mvn-" + generator.getServerName() + "-");
        Throwable failure = null;
        try {
            writeSourceFiles(projDir, generator.getSourceFiles());
            writePom(project, projDir, generator);
            runMaven(project, projDir, generator.getMavenGoals(), indicator, outputHandler);
            exportSourceProject(projDir, sourceProjectDir);
            return copyArtifact(generator, projDir, artifactDir);
        } catch (IOException | RuntimeException | Error e) {
            failure = e;
            throw e;
        } finally {
            try {
                deleteTreeNoFollow(projDir);
            } catch (IOException cleanupFailure) {
                if (failure == null) throw cleanupFailure;
                failure.addSuppressed(cleanupFailure);
            }
        }
    }

    private static void exportSourceProject(Path projDir, Path sourceProjectDir) throws IOException {
        if (sourceProjectDir == null) return;

        validateTreeNoFollow(projDir);
        deleteTreeNoFollow(sourceProjectDir);
        createDirectoriesNoFollow(sourceProjectDir);

        copyFileIfExists(projDir.resolve("pom.xml"), sourceProjectDir.resolve("pom.xml"));
        // rendered as "Dockerfile.ci" so micronaut-maven-plugin does not pick it up
        // as a docker-native packaging override during the build
        copyFileIfExists(projDir.resolve("Dockerfile.ci"), sourceProjectDir.resolve("Dockerfile"));
        copyDirIfExists(projDir.resolve("src"), sourceProjectDir.resolve("src"));
    }

    private static void copyFileIfExists(Path source, Path target) throws IOException {
        validatePathNoFollow(source);
        if (!Files.exists(source) || !Files.isRegularFile(source)) return;
        copyFileNoFollow(source, target);
    }

    private static void copyDirIfExists(Path source, Path target) throws IOException {
        validateTreeNoFollow(source);
        if (!Files.exists(source) || !Files.isDirectory(source)) return;
        copyDirectory(source, target);
    }

    private static void copyDirectory(Path sourceDir, Path targetDir) throws IOException {
        try (Stream<Path> stream = Files.walk(sourceDir)) {
            for (Path source : stream.toList()) {
                Path relative = sourceDir.relativize(source);
                Path target = targetDir.resolve(relative);
                validatePathNoFollow(source);
                if (Files.isDirectory(source)) {
                    createDirectoriesNoFollow(target);
                } else {
                    copyFileNoFollow(source, target);
                }
            }
        }
    }

    private static void writeSourceFiles(Path projDir, Map<String, String> sourceFiles) throws IOException {
        for (Map.Entry<String, String> entry : sourceFiles.entrySet()) {
            Path file = projDir.resolve(entry.getKey());
            writeStringNoFollow(file, entry.getValue(), StandardCharsets.UTF_8);
        }
    }

    private static void writePom(Project project, Path dir, McpServerGenerator generator) throws IOException {
        String pom = TemplateUtilities.generateCode(project, generator.getPomTemplateName(), generator.getPomProperties());
        writeStringNoFollow(dir.resolve("pom.xml"), pom, StandardCharsets.UTF_8);
    }

    private static void runMaven(Project project, Path dir, List<String> goals, ProgressIndicator indicator, Consumer<String> outputHandler) throws IOException {
        validateTreeNoFollow(dir);
        McpMavenBuildManager mavenManager = McpMavenBuildManager.getInstance(project);
        if (mavenManager == null) {
            throw new IOException(txt("msg.mcp.exception.MavenServiceUnavailable"));
        }
        mavenManager.runBuild(dir, goals, indicator, outputHandler);
    }

    private static Path copyArtifact(McpServerGenerator generator, Path proj, Path out) throws IOException {
        Path artifact = generator.locateArtifact(proj.resolve("target"));
        if (artifact == null) return null; // no file artifact (e.g. container image build)

        createDirectoriesNoFollow(out);
        Path dest = out.resolve(artifact.getFileName());
        // COPY_ATTRIBUTES preserves the executable bit of native binaries
        copyFileNoFollow(artifact, dest);
        return dest;
    }
}

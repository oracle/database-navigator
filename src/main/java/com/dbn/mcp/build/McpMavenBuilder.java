package com.dbn.mcp.build;

import com.dbn.common.template.TemplateUtilities;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.dbn.mcp.build.McpJavaVersionManager.resolveJavaVersion;

public final class McpMavenBuilder {
    private static final String POM_TEMPLATE = "DBN - MCP Server POM.xml";
    private static final Pattern PKG = Pattern.compile("\\bpackage\\s+([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)*)\\s*;");
    private static final Pattern PUB_CLASS = Pattern.compile("\\bpublic\\s+class\\s+([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern ANY_CLASS = Pattern.compile("\\bclass\\s+([A-Za-z_][A-Za-z0-9_]*)");

    private McpMavenBuilder() {}

    public static Path build(
            Project project,
            Path outDir,
            String serverName,
            String sdkCoord,
            String jdbcCoord,
            String java,
            Path sourceProjectDir,
            ProgressIndicator indicator,
            Consumer<String> outputHandler)
            throws IOException {
        MavenCoordinate sdk = MavenCoordinate.parse(sdkCoord);
        MavenCoordinate jdbc = MavenCoordinate.parse(jdbcCoord);
        ServerMainClass mainClass = resolveServerMainClass(java);

        Path projDir = uniqueDir(outDir, mainClass.className);
        try {
            setupProject(projDir, java, mainClass);
            writePom(project, projDir, serverName, sdk, jdbc, mainClass.fullyQualifiedName);
            runMaven(project, projDir, indicator, outputHandler);
            exportSourceProject(projDir, sourceProjectDir);
            return copyJar(projDir, outDir);
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

    private static ServerMainClass resolveServerMainClass(String src) {
        String packageName = find(PKG, src);
        String className = find(PUB_CLASS, src);
        if (className == null) className = find(ANY_CLASS, src);
        if (className == null) className = "GeneratedMcpServer";
        String fullyQualifiedName = packageName != null ? packageName + "." + className : className;
        return new ServerMainClass(packageName, className, fullyQualifiedName);
    }

    private static String find(Pattern p, String src) {
        Matcher m = p.matcher(src);
        return m.find() ? m.group(1) : null;
    }

    private static Path uniqueDir(Path base, String name) throws IOException {
        Path dir = base.resolve("mcp-mvn-" + name);
        int i = 1;
        while (Files.exists(dir)) dir = base.resolve("mcp-mvn-" + name + "-" + i++);
        return dir;
    }

    private static void setupProject(Path dir, String java, ServerMainClass mainClass) throws IOException {
        Path src = dir.resolve("src/main/java");
        Files.createDirectories(src);

        Path packagePath = mainClass.packageName != null ? src.resolve(mainClass.packageName.replace('.', '/')) : src;
        Files.createDirectories(packagePath);
        Files.writeString(packagePath.resolve(mainClass.className + ".java"), java);
    }

    private static void writePom(
            Project project,
            Path dir,
            String serverName,
            MavenCoordinate sdk,
            MavenCoordinate jdbc,
            String main)
            throws IOException {
        Properties p = new Properties();
        p.setProperty("SERVER_NAME", serverName);
        p.setProperty("MCP_SDK_GROUP_ID", sdk.groupId);
        p.setProperty("MCP_SDK_ARTIFACT_ID", sdk.artifactId);
        p.setProperty("MCP_SDK_VERSION", sdk.version);
        p.setProperty("JDBC_GROUP_ID", jdbc.groupId);
        p.setProperty("JDBC_ARTIFACT_ID", jdbc.artifactId);
        p.setProperty("JDBC_VERSION", jdbc.version);
        p.setProperty("MAIN_CLASS_FQ", main);
        p.setProperty("PROJECT_JAVA_VERSION", resolveJavaVersion(project));
        Files.writeString(dir.resolve("pom.xml"), TemplateUtilities.generateCode(project, POM_TEMPLATE, p));
    }

    private static void runMaven(Project project, Path dir, ProgressIndicator indicator, Consumer<String> outputHandler) throws IOException {
        McpMavenBuildManager mavenManager = McpMavenBuildManager.getInstance(project);
        if (mavenManager == null) {
            throw new IOException("Maven service is not available. Please enable the Maven plugin.");
        }
        mavenManager.runBuild(dir, indicator, outputHandler);
    }

    private static Path copyJar(Path proj, Path out) throws IOException {
        Path jar;
        try (var stream = Files.list(proj.resolve("target"))) {
            jar = stream.filter(p -> p.toString().endsWith(".jar") && !p.toString().contains("original"))
                    .findFirst().orElseThrow(() -> new IOException("JAR not found"));
        }
        Files.createDirectories(out);
        Path dest = out.resolve(jar.getFileName());
        Files.copy(jar, dest, StandardCopyOption.REPLACE_EXISTING);
        return dest;
    }


    private record MavenCoordinate(String groupId, String artifactId, String version) {
        static MavenCoordinate parse(String s) {
            String[] p = s.split(":");
            return new MavenCoordinate(p[0], p[1], p[2]);
        }
    }

    private record ServerMainClass(String packageName, String className, String fullyQualifiedName) {}
}

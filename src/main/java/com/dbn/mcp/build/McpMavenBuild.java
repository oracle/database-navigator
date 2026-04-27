package com.dbn.mcp.build;

import com.dbn.common.template.TemplateUtilities;
import com.dbn.common.util.Messages;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
public final class McpMavenBuild {
    private static final PluginId MAVEN_PLUGIN_ID = PluginId.getId("org.jetbrains.idea.maven");
    private static final String POM_TEMPLATE = "DBN - MCP Server POM.xml";
    private static final Pattern PKG = Pattern.compile("\\bpackage\\s+([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)*)\\s*;");
    private static final Pattern PUB_CLASS = Pattern.compile("\\bpublic\\s+class\\s+([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern ANY_CLASS = Pattern.compile("\\bclass\\s+([A-Za-z_][A-Za-z0-9_]*)");

    private McpMavenBuild() {}

    public static Path buildWithMaven(
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
        Coord sdk = Coord.parse(sdkCoord);
        Coord jdbc = Coord.parse(jdbcCoord);
        Info info = analyze(java);

        Path projDir = uniqueDir(outDir, info.className);
        try {
            setupProject(projDir, java, info);
            writePom(project, projDir, serverName, sdk, jdbc, info.fqn);
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

    private static Info analyze(String src) {
        String pkg = find(PKG, src);
        String cls = find(PUB_CLASS, src);
        if (cls == null) cls = find(ANY_CLASS, src);
        if (cls == null) cls = "GeneratedMcpServer";
        return new Info(pkg, cls, pkg != null ? pkg + "." + cls : cls);
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

    private static void setupProject(Path dir, String java, Info info) throws IOException {
        Path src = dir.resolve("src/main/java");
        Files.createDirectories(src);

        Path pkg = info.pkg != null ? src.resolve(info.pkg.replace('.', '/')) : src;
        Files.createDirectories(pkg);
        Files.writeString(pkg.resolve(info.className + ".java"), java);
    }

    private static void writePom(Project project, Path dir, String serverName, Coord sdk, Coord jdbc, String main) throws IOException {
        Properties p = new Properties();
        p.setProperty("SERVER_NAME", serverName);
        p.setProperty("MCP_SDK_GROUP_ID", sdk.g);
        p.setProperty("MCP_SDK_ARTIFACT_ID", sdk.a);
        p.setProperty("MCP_SDK_VERSION", sdk.v);
        p.setProperty("JDBC_GROUP_ID", jdbc.g);
        p.setProperty("JDBC_ARTIFACT_ID", jdbc.a);
        p.setProperty("JDBC_VERSION", jdbc.v);
        p.setProperty("MAIN_CLASS_FQ", main);
        Files.writeString(dir.resolve("pom.xml"), TemplateUtilities.generateCode(project, POM_TEMPLATE, p));
    }

    private static void runMaven(Project project, Path dir, ProgressIndicator indicator, Consumer<String> outputHandler) throws IOException {
        McpMavenService mavenService = McpMavenService.getInstance(project);
        if (mavenService == null) {
            throw new IOException("Maven service is not available. Please enable the Maven plugin.");
        }
        mavenService.runBuild(dir, indicator, outputHandler);
    }


    public static boolean isMavenAvailable(Project project) {
        if (project == null) return false;
        if (!isMavenPluginAvailable()) return false;

        try {
            McpMavenService mavenService = McpMavenService.getInstance(project);
            return mavenService != null && mavenService.isRuntimeAvailable();
        } catch (Throwable e) {
            log.warn("Could not resolve Maven runtime", e);
            return false;
        }
    }

    public static boolean isMavenPluginAvailable() {
        return PluginManagerCore.isPluginInstalled(MAVEN_PLUGIN_ID) && !PluginManagerCore.isDisabled(MAVEN_PLUGIN_ID);
    }

    public static boolean ensureMavenPrerequisites(Project project) {
        if (!isMavenPluginAvailable()) {
            int option = Messages.showConfirmationDialog(project,
                    "Maven Plugin Required",
                    "This feature requires the Maven plugin (org.jetbrains.idea.maven).\n" +
                    "Please enable or install it from IDE Plugins settings.",
                    new String[]{"Open Plugins", "Cancel"}, 0);
            if (option == 0) {
                openMavenPluginSettings(project);
            }
            return false;
        }

        if (!isMavenAvailable(project)) {
            int option = Messages.showConfirmationDialog(project,
                    "Maven Required",
                    "Maven runtime is not available or invalid in IDE Maven settings.\n" +
                    "Please verify Maven settings and try again.",
                    new String[]{"Open Plugins", "Cancel"}, 0);
            if (option == 0) {
                openMavenPluginSettings(project);
            }
            return false;
        }

        return true;
    }

    public static void openMavenPluginSettings(Project project) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, "preferences.pluginManager");
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


    private static class Coord {
        final String g, a, v;
        Coord(String g, String a, String v) { this.g = g; this.a = a; this.v = v; }
        static Coord parse(String s) { String[] p = s.split(":"); return new Coord(p[0], p[1], p[2]); }
    }

    private static class Info {
        final String pkg, className, fqn;
        Info(String pkg, String className, String fqn) { this.pkg = pkg; this.className = className; this.fqn = fqn; }
    }
}

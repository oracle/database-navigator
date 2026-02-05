package com.dbn.mcp;

import com.dbn.common.template.TemplateUtilities;
import org.apache.maven.shared.invoker.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MicroBuild {
    private static final String POM_TEMPLATE = "DBN - MCP Server POM.xml";
    private static final Pattern PKG = Pattern.compile("\\bpackage\\s+([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)*)\\s*;");
    private static final Pattern PUB_CLASS = Pattern.compile("\\bpublic\\s+class\\s+([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern ANY_CLASS = Pattern.compile("\\bclass\\s+([A-Za-z_][A-Za-z0-9_]*)");

    private MicroBuild() {}

    public static Path buildWithMaven(Path outDir, String sdkCoord, String jdbcCoord, String java, Path props, Consumer<String> log)
            throws IOException, MavenInvocationException {
        Coord sdk = Coord.parse(sdkCoord);
        Coord jdbc = Coord.parse(jdbcCoord);
        Info info = analyze(java);

        Path projDir = uniqueDir(outDir, info.className);
        setupProject(projDir, java, props, info);
        writePom(projDir, sdk, jdbc, info.fqn);
        runMaven(projDir, log);

        return copyJar(projDir, outDir);
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

    private static void setupProject(Path dir, String java, Path props, Info info) throws IOException {
        Path src = dir.resolve("src/main/java");
        Path res = dir.resolve("src/main/resources");
        Files.createDirectories(src);
        Files.createDirectories(res);

        Path pkg = info.pkg != null ? src.resolve(info.pkg.replace('.', '/')) : src;
        Files.createDirectories(pkg);
        Files.writeString(pkg.resolve(info.className + ".java"), java);
        Files.copy(props, res.resolve("mcp-config.properties"), StandardCopyOption.REPLACE_EXISTING);
    }

    private static void writePom(Path dir, Coord sdk, Coord jdbc, String main) throws IOException {
        Properties p = new Properties();
        p.setProperty("MCP_SDK_GROUP_ID", sdk.g);
        p.setProperty("MCP_SDK_ARTIFACT_ID", sdk.a);
        p.setProperty("MCP_SDK_VERSION", sdk.v);
        p.setProperty("JDBC_GROUP_ID", jdbc.g);
        p.setProperty("JDBC_ARTIFACT_ID", jdbc.a);
        p.setProperty("JDBC_VERSION", jdbc.v);
        p.setProperty("MAIN_CLASS_FQ", main);
        Files.writeString(dir.resolve("pom.xml"), TemplateUtilities.generateCode(null, POM_TEMPLATE, p));
    }

    private static void runMaven(Path dir, Consumer<String> log) throws MavenInvocationException, IOException {
        InvocationRequest req = new DefaultInvocationRequest();
        req.setPomFile(dir.resolve("pom.xml").toFile());
        req.addArgs(List.of("clean", "package"));
        req.setBatchMode(true);
        if (log != null) { req.setOutputHandler(log::accept); req.setErrorHandler(log::accept); }

        Invoker inv = new DefaultInvoker();
        configureMaven(inv, dir);

        InvocationResult res = inv.execute(req);
        if (res.getExitCode() != 0) throw new IllegalStateException("Maven failed: " + res.getExitCode());
    }

    private static void configureMaven(Invoker inv, Path dir) throws IOException {
        File home = findMavenHome();
        if (home != null) { inv.setMavenHome(home); return; }
        File exe = findMavenExe();
        if (exe != null) { inv.setMavenExecutable(exe); return; }
        inv.setMavenExecutable(installWrapper(dir).toFile());
    }

    private static Path copyJar(Path proj, Path out) throws IOException {
        Path jar = Files.list(proj.resolve("target"))
                .filter(p -> p.toString().endsWith(".jar") && !p.toString().contains("original"))
                .findFirst().orElseThrow(() -> new IOException("JAR not found"));
        Files.createDirectories(out);
        Path dest = out.resolve(jar.getFileName());
        Files.copy(jar, dest, StandardCopyOption.REPLACE_EXISTING);
        return dest;
    }

    // Maven discovery
    private static File findMavenHome() {
        for (String c : new String[]{System.getProperty("maven.home"), System.getenv("MAVEN_HOME"), System.getenv("M2_HOME")}) {
            if (c != null && !c.isBlank()) {
                File d = new File(c);
                if (validHome(d)) return d;
            }
        }
        try {
            File d = new File(com.intellij.openapi.application.PathManager.getHomePath() + "/plugins/maven/lib/maven3");
            if (validHome(d)) return d;
        } catch (Throwable ignored) {}
        return null;
    }

    private static boolean validHome(File d) {
        return d.isDirectory() && new File(d, "bin/" + (win() ? "mvn.cmd" : "mvn")).exists();
    }

    private static File findMavenExe() {
        String prop = System.getProperty("mvn.executable");
        if (prop != null) { File f = new File(prop); if (f.canExecute()) return f; }
        String path = System.getenv("PATH");
        if (path != null) {
            for (String dir : path.split(File.pathSeparator)) {
                File f = new File(dir, win() ? "mvn.cmd" : "mvn");
                if (f.canExecute()) return f;
            }
        }
        for (File f : new File[]{new File("/opt/homebrew/bin/mvn"), new File("/usr/local/bin/mvn")}) {
            if (f.canExecute()) return f;
        }
        return null;
    }

    // Maven wrapper
    private static Path installWrapper(Path dir) throws IOException {
        Path wrap = dir.resolve(".mvn/wrapper");
        Files.createDirectories(wrap);
        Files.writeString(wrap.resolve("maven-wrapper.properties"),
                "distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip\n" +
                "wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar\n");

        Path unix = dir.resolve("mvnw");
        Files.writeString(unix,
                "#!/bin/sh\nset -e\nBASE=\"$PWD\"\nJAR=\"$BASE/.mvn/wrapper/maven-wrapper.jar\"\n" +
                "PROPS=\"$BASE/.mvn/wrapper/maven-wrapper.properties\"\nURL=`sed -n 's/^wrapperUrl=//p' \"$PROPS\"`\n" +
                "[ -f \"$JAR\" ] || { curl -fsSL -o \"$JAR\" \"$URL\" 2>/dev/null || wget -q -O \"$JAR\" \"$URL\"; }\n" +
                "exec java -Dmaven.multiModuleProjectDirectory=\"$BASE\" -cp \"$JAR\" org.apache.maven.wrapper.MavenWrapperMain \"$@\"\n");
        try { Files.setPosixFilePermissions(unix, EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE)); }
        catch (UnsupportedOperationException ignored) {}

        Files.writeString(dir.resolve("mvnw.cmd"),
                "@ECHO OFF\nSETLOCAL\nSET BASE=%CD%\nSET JAR=%BASE%\\.mvn\\wrapper\\maven-wrapper.jar\n" +
                "SET PROPS=%BASE%\\.mvn\\wrapper\\maven-wrapper.properties\n" +
                "FOR /F \"tokens=1,2 delims==\" %%A IN (%PROPS%) DO IF \"%%A\"==\"wrapperUrl\" SET URL=%%B\n" +
                "IF NOT EXIST \"%JAR%\" powershell -Command \"(New-Object Net.WebClient).DownloadFile('%URL%','%JAR%')\"\n" +
                "java -Dmaven.multiModuleProjectDirectory=\"%BASE%\" -cp \"%JAR%\" org.apache.maven.wrapper.MavenWrapperMain %*\n");

        return win() ? dir.resolve("mvnw.cmd") : unix;
    }

    private static boolean win() { return System.getProperty("os.name", "").toLowerCase().contains("win"); }

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
